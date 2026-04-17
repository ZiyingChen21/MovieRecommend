package com.cc

import org.apache.spark.SparkConf
import org.apache.spark.mllib.recommendation.{ALS, Rating}
import org.apache.spark.sql.SparkSession
import org.jblas.DoubleMatrix

case class MovieRating(uid: Int, mid: Int, score: Double, timestamp: Int)
case class MongoConfig(uri: String, db: String)
case class MovieRecBase(mid: Int, score: Double)
case class UserRecommendation(uid: Int, recommendations: Seq[MovieRecBase])
case class MovieSimilarity(mid: Int, similarityMovies: Seq[MovieRecBase])
object ALSRecommend {
  val MONGODB_RATING_COLLECTION = "Rating"
  val USER_RECOMMENDATION = "UserRecommendation"
  val MOVIE_SIMILARITY = "MovieSimilarity"
  val USER_MAX_RECOMMENDATION = 20  //电影推荐个数

  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/recommend",
      "mongo.db" -> "recommend"
    )
    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("ALSRecommend")
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()
    import spark.implicits._
    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))

    //加载数据
    val ratingRDD = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[MovieRating]
      .rdd
      .map(rating => (rating.uid, rating.mid, rating.score))  // 转为 RDD 并去掉时间戳  造一条新数据替换旧数据
      .cache()  //缓存性能提升 RDD 持久化在内存中

    val userRDD = ratingRDD.map(_._1).distinct() //提取所有 uid 和 mid 并去重 准备初级随机特征向量
    val movieRDD = ratingRDD.map(_._2).distinct()

    //训练隐语义模型 训练需要标准 Rating 数据(MLLib-> 包同级文件： ALS MatrixFactorizationModel)
    val trainData = ratingRDD.map(x => Rating(x._1, x._2, x._3))
    var rank = 200
    var lambda = 0.1
    val iterations = 15
    try {
      val paramDF = spark.read
        .option("uri", mongoConfig.uri)
        .option("collection", "ModelParams")
        .format("com.mongodb.spark.sql")
        .load()

      val rows = paramDF.take(1)
      if (rows.length > 0) {
        val row = rows(0)
        rank = row.getAs[Int]("rank")
        lambda = row.getAs[Double]("lambda")
        println(s"成功加载最新最优参数: rank=$rank, lambda=$lambda")
      } else {
        println(s"未在数据库找到优化参数，使用默认参数: rank=$rank, lambda=$lambda")
      }
    } catch {
      case exception: Exception =>
        println(s"读取参数表失败，使用默认参数: rank=$rank, lambda=$lambda。原因: ${exception.getMessage}")
    }
    val model = ALS.train(trainData, rank, iterations, lambda)


    // 直接调用底层优化的 API，给每个用户推荐 Top 20
    val userRecommendation = model.recommendProductsForUsers(USER_MAX_RECOMMENDATION)
      .map {
        case (uid, recommendations) =>
          // recommendations 是一个 Rating 数组，把它转成定义的 MovieRecBase 样例类
          UserRecommendation(uid, recommendations.map(r => MovieRecBase(r.product, r.rating)))
      }.toDF()

    userRecommendation.write
      .option("uri", mongoConfig.uri)
      .option("collection", USER_RECOMMENDATION)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    //基于电影隐特征，计算相似度矩阵，得到电影的相似度列表
    val movieFeatures = model.productFeatures.map{
      case (mid, features) => (mid, new DoubleMatrix(features))
    }
    //电影两两计算相似度
    val similarityMovie = movieFeatures.cartesian(movieFeatures)
      .filter{
        //排除自己和自己的相似度
        case (a, b) => (a._1 != b._1)
      }
      .map{
        case (a, b) => {
          val similarityScore = this.consinSim(a._2, b._2)
          (a._1, (b._1, similarityScore)) //最后一行即是返回
        }
      }
      .filter(_._2._2 > 0.6)  //过滤出相似度大于 0.6
      .groupByKey()
      .map{
        case (mid, item) => MovieSimilarity(mid, item.toList.sortWith(_._2 > _._2).map(x => MovieRecBase(x._1, x._2)))
      }.toDF()

    similarityMovie.write
      .option("uri", mongoConfig.uri)
      .option("collection", MOVIE_SIMILARITY)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    spark.stop()
  }

  //求向量余弦相似度
  def consinSim(movie1: DoubleMatrix, movie2: DoubleMatrix): Double = {
    movie1.dot(movie2) / (movie1.norm2() * movie2.norm2())
  }

}
