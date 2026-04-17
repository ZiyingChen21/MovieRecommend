package com.cc

import org.apache.spark.SparkConf
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.text.SimpleDateFormat
import java.util.Date

/**
 * 历史热门统计
 * 最近热门统计
 * 平均评分
 * top n
 *      读取数据 + 处理数据 + 写回 MongoDB
 * */
case class Movie(mid: Int, name: String, descri: String, timelong: String, issue: String,
                 shoot: String, Language: String, genres: String, actors: String, directors: String)
case class Rating(uid: Int, mid: Int, score: Double, timestamp: Int)
case class MongoConfig(uri: String, db: String) //封装好的配置样例类

case class RecommendBase(mid: Int, score: Double)
case class GenresRecommend(genres: String, recommendBase: Seq[RecommendBase])

object StatisticsRecommend {
  val MONGODB_MOVIE_COLLECTION = "Movie"
  val MONGODB_RATING_COLLECTION = "Rating"

  //定义四个存入 MongoDB 表
  val HISTORICAL_POPULARITY_STATISTICS = "HistoricalPopularity"
  val RECENT_POPULARITY_STATISTICS = "RecentPopularity"
  val MOVIE_AVERAGE_RATING = "AverageRating"
  val TOP_10_GENRES = "TopGenres"

  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]", // 使用本地所有 CPU 线程跑 Spark
      "mongo.uri" -> "mongodb://localhost:27017/recommend",
      "mongo.db" -> "recommend"
    )

    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("StatisticsRecommend").set("spark.ui.port", "4050")
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    import spark.implicits._
    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))

    //load Data from mongoDB use : mongp spark connector
    val ratingDF = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .withColumn("uid", col("uid").cast("int"))
      .withColumn("mid", col("mid").cast("int"))
      .withColumn("score", col("score").cast("double"))
      .withColumn("timestamp", col("timestamp").cast("int"))
      .as[Rating]
      .toDF()

    val movieDF = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_MOVIE_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .withColumn("mid", col("mid").cast("int"))
      .as[Movie]
      .toDF()

    //创建名为 ratings 的临时表/视图
    ratingDF.createTempView("ratings")

    //TODO: 不同的统计推荐结果
    //1.历史热门统计 历史评分数据最多-> 注册UDF -》 mid count -> 写入mongoDB表中
    val historicalPopularityDF = spark.sql("select mid, count(mid) as count from Ratings group by mid")
    storeDFToMongoDB(historicalPopularityDF, HISTORICAL_POPULARITY_STATISTICS)

    //2.近期热门统计 按照时间戳“yyyyMM”筛选最近的评分数据，统计评分个数    转 timestamp(s) 格式- 日期格式化工具  Date.parse(new Date())-> ms 后面有 000
    val simpleDataFormat = new SimpleDateFormat("yyyyMM")
    spark.udf.register("changeDate", (x : Int) => simpleDataFormat.format(new Date(x * 1000L)).toInt)  // long防止溢出 因为是要年月 转 int 就可以
    val ratingOfYearMonth = spark.sql("select mid, score, changeDate(timestamp) as yearmonth from Ratings")
    ratingOfYearMonth.createOrReplaceTempView("ratingOfMonth")
    val recentPolularityDF = spark.sql("select mid, count(mid) as count, yearmonth from ratingOfMonth group by yearmonth, mid order by yearmonth desc, count desc")
    storeDFToMongoDB(recentPolularityDF, RECENT_POPULARITY_STATISTICS)

    //3.优质电影推荐 统计电影平均评分
    val averageRatingDF = spark.sql("select mid, avg(score) as average from Ratings group by mid")
    storeDFToMongoDB(averageRatingDF, MOVIE_AVERAGE_RATING)

    //4.各类别电影Top统计 Sql比较复杂，用 Spark 算子处理简单 判断类别+电影类别+ 平均评分提取加入Movie join 没有score则不统计 inner join
    val genres = List("Action","Adventure","Animation","Comedy","Crime","Documentary","Drama","Family","Fantasy","Foreign","History","Horror","Music","Mystery"
      ,"Romance","Science","Tv","Thriller","War","Western", "Sci-Fi")
    val movieWithScore = averageRatingDF.join(movieDF, "mid")
    // 类别 + 电影 笛卡尔积过滤 需要RDD操作 genres -> RDD
    val genresRDD = spark.sparkContext.makeRDD(genres)
    val top10GenresDF = genresRDD.cartesian(movieWithScore.rdd)
      .filter{
        // 条件过滤 movie's genres 包含当前类别
        case (genre, movieRow) =>
          val genresStr = movieRow.getAs[String]("genres")
          genresStr != null && genresStr.toLowerCase().contains(genre.toLowerCase())
      }
      .map{
            //过滤出想要的字段 genre mid socre
        case (genre, movieRow) => (genre, (movieRow.getAs[Int]("mid"), movieRow.getAs[Double]("average")))
      }
      .groupByKey()
      .map{
            // 转为想要的数据结构
        case (genre, items) => GenresRecommend(genre, items.toList.sortWith(_._2 > _._2).take(10).map(
          item => RecommendBase(item._1, item._2)
        ))
      }.toDF()
    storeDFToMongoDB(top10GenresDF, TOP_10_GENRES)
    spark.stop()
  }

  def storeDFToMongoDB(df: DataFrame, collection_name: String)(implicit mongoConfig: MongoConfig): Unit = {
    df.write
      .option("uri", mongoConfig.uri)
      .option("collection", collection_name)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()
  }

}
