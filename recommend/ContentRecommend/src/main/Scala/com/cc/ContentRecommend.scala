package com.cc

import org.apache.spark.SparkConf
import org.apache.spark.ml.feature.{HashingTF, IDF, Tokenizer}
import org.apache.spark.ml.linalg.SparseVector
import org.apache.spark.sql.SparkSession
import org.jblas.DoubleMatrix

case class Movie(mid: Int, name: String, descri: String, timelong: String, issue: String,
                 shoot: String, language: String, genres: String, actors: String, directors: String)

case class MongoConfig(uri:String, db:String)
case class MovieRecBase(mid: Int, score: Double)
//基于电影内容信息提取出的特征向量的电影相似度列表
case class MovieSimilarity(mid: Int, similarityMovies: Seq[MovieRecBase])


object ContentRecommend {
  val MONGODB_MOVIE_COLLECTION = "Movie"
  val CONTENT_MOVIE_RECS = "ContentMovieRecommendation"

  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/recommend",
      "mongo.db" -> "recommender"
    )
    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("ContentRecommendation")
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    import spark.implicits._
    val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))

    val movieTagsDF = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_MOVIE_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .withColumn("mid", $"mid".cast("int"))
      .as[Movie]
      .map(x => {
        // 1. 处理空值并将分隔符 | 替换为空格，防止分词器切不开
        val genres = if (x.genres != null) x.genres.replace('|', ' ') else ""
        val actors = if (x.actors != null) x.actors.replace('|', ' ') else ""
        val directors = if (x.directors != null) x.directors.replace('|', ' ') else ""
        val descri = if (x.descri != null) x.descri else ""
        //2. 将所有特征拼接成一段超长文本，权重也可以通过重复追加来调整（比如把导演拼两次增加比重）
        val allFeatures = genres + " " + genres + " " + genres + " " + genres + " " + genres + " " + actors + " " + directors + " " + descri

        (x.mid, x.name, allFeatures)
      }).toDF("mid", "name", "textFeatures")
      .cache()


    // 核心部分： 用TF-IDF从内容信息中提取电影特征向量

    // 创建一个分词器，默认按空格分词
    val tokenizer = new Tokenizer().setInputCol("textFeatures").setOutputCol("words")

    // 用分词器对原始数据做转换，生成新的一列words [mid, name, genres, words]
    val wordsData = tokenizer.transform(movieTagsDF)
    wordsData.show()

    // 引入HashingTF工具，可以把一个词语序列转化成对应的词频
    val hashingTF = new HashingTF().setInputCol("words").setOutputCol("rawFeatures").setNumFeatures(50)
    val featurizedData = hashingTF.transform(wordsData)

    // 引入IDF工具，可以得到idf模型
    val idf = new IDF().setInputCol("rawFeatures").setOutputCol("features")
    // 训练idf模型，得到每个词的逆文档频率
    val idfModel = idf.fit(featurizedData)
    // 用模型对原数据进行处理，得到文档中每个词的tf-idf，作为新的特征向量
    val rescaleData = idfModel.transform(featurizedData)

    rescaleData.show(truncate = false)

    //featurizedData.show(false)
    // rescaledData.show(truncate = false)

    val movieFeatures = rescaleData.map(
      row => (row.getAs[Int]("mid"), row.getAs[SparseVector]("features").toArray)
    )
      .rdd
      .map(
        x => (x._1, new DoubleMatrix(x._2))
      )
    movieFeatures.collect().foreach(println)

    // 对所有电影两两计算它们的相似度，先做笛卡尔积
    val movieRecommend = movieFeatures.cartesian(movieFeatures)
      .filter{
        case (a, b) => a._1 != b._1
      }
      .map{
        case (a, b) => {
          val similarityScore = this.consinSim(a._2, b._2)
          (a._1, (b._1, similarityScore))
        }
      }
      .filter(_._2._2 > 0.6)
      .groupByKey()
      .map{
        case (mid, items) => MovieSimilarity(mid, items.toList.sortWith(_._2 > _._2).take(20).map(x => MovieRecBase(x._1, x._2)))
      }.toDF()

    movieRecommend.write
      .option("uri", mongoConfig.uri)
      .option("collection", CONTENT_MOVIE_RECS)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()


    spark.stop()
  }

  def consinSim(movie1: DoubleMatrix, movie2: DoubleMatrix):Double ={
    movie1.dot(movie2) / ( movie1.norm2() * movie2.norm2() )
  }

}
