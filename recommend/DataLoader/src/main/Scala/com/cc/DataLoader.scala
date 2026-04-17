package com.cc

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}
import java.text.SimpleDateFormat
import java.util.Locale


/**
 * 读数据并处理后 存入 MongoDB、ElasticSearch
 * 再用 Spark SQL写 需要定义表结构
 * 指定Schema
 * 数据中只有值
 *    定义样例类 定义字段 ^分割内容--- Movie 共 10 个字段
 *    260                                                     电影ID    mid   Int
 *    ^Star Wars: Episode IV - A New Hope (1977)              电影名称  name
 *    ^Princess Leia is captured and held hostage by the e    详情描述  descri
 *    ^ 121 min                                               时长      timelong
 *    ^September 21, 2004                                     发行时间  issue
 *    ^1977                                                   拍摄时间 shoot
 *    ^English                                                语言   Language
 *    ^Action|Adventure|Sci-Fi                                类型   genres
 *    ^Mark Hamill|Harrisoing|Alec Guinness                   演员表 actors
 *    ^George Lucas                                           导演   directors
 * */
case class Movie(mid: Int, name: String, descri: String, timelong: String, issue: String,
                 shoot: String, Language: String, genres: String, actors: String, directors: String)
/**
 * rating ,分割
 * 用户id Int
 * 电影id Int
 * 评分   Double
 * 时间戳 timestamp: Int
 * */
case class Rating(uid: Int, mid: Int, score: Double, timestamp: Int)
case class Tag(uid: Int, mid: Int, tag: String, timestamp: Int)


//将 mongoDB 和 ES 的配置封装成样例类 uri-mongoDB的连接 db-数据库
case class MongoConfig(uri: String, db: String)
//httpHosts：连接ES主机列表 逗号分割，接受请求的端口9200， transportHosts:transport 主机列表，集群做彼此内部之间的传输
// index 需要操作的索引 clustername默认elasticsearch
case class ESConfig(httpHosts:String, transportHosts: String, index: String, clustername: String)

object DataLoader {
  val MOVIE_DATA_PATH = "D:\\gitClone\\MovieRecommend\\recommend\\DataLoader\\src\\main\\resources\\movies.csv"
  val RATING_DATA_PATH = "D:\\gitClone\\MovieRecommend\\recommend\\DataLoader\\src\\main\\resources\\ratings.csv"
  val TAG_DATA_PATH = "D:\\gitClone\\MovieRecommend\\recommend\\DataLoader\\src\\main\\resources\\tags.csv"

  // 定义常量：MongoDB 中的表名（集合名）
  val MONGODB_MOVIE_COLLECTION = "Movie"
  val MONGODB_RATING_COLLECTION = "Rating"
  val MONGODB_TAG_COLLECTION = "Tag"
  val ES_MOVIE_INDEX = "movie"

  def main(args: Array[String]): Unit = {
    //定义常量

    val config = Map(
      "spark.cores" -> "local[*]", // 使用本地所有 CPU 线程跑 Spark
      "mongo.uri" -> "mongodb://localhost:27017/recommend",
      "mongo.db" -> "recommend",
      "es.httpHosts" -> "localhost:9200",
      "es.transportHosts" -> "localhost:9300",
      "es.index" -> "recommend",
      "es.cluster.name" -> "elasticsearch"
    )
    //创建一个 sparkconfig
    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("DataLoader")

    //创建一个sparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    import spark.implicits._
    //加载数据
    val movieRDD = spark.sparkContext.textFile(MOVIE_DATA_PATH)
    // 按照分隔符提取字段数据 再用定义好的样例类进行包装 再转为 DataFrame 再存入MongoDB
    val movieDF = movieRDD.map(
      item => {
        val attr = item.split("\\^") // /在正则中也需要转义 与 ^在正则中表示开始 均需要转义

        // 提取原始时间字段
        val rawIssue = attr(4).trim
        val shoot = attr(5).trim
        var formattedIssue = ""
        try {
          if (rawIssue.nonEmpty) {
            // 定义输入输出格式 Locale.ENGLISH 解析 September 这种英文单词
            val inputFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)
            val outputFormat = new SimpleDateFormat("yyyy-MM-dd")

            val date = inputFormat.parse(rawIssue)
            formattedIssue = outputFormat.format(date)
          } else {
            throw new Exception("Empty issue date")
          }
        } catch {
          case _: Exception =>
            // 兜底策略 (容错处理)：
            // 如果解析失败（比如格式不对、数据缺失），用拍摄年份 shoot 拼接 1月1日 兜底
            if (shoot.nonEmpty) {
              formattedIssue = shoot + "-01-01"
            } else {
              formattedIssue = "1970-01-01" // 终极兜底
            }
        }

        Movie(attr(0).toInt, attr(1).trim, attr(2).trim, attr(3).trim, formattedIssue,
          shoot, attr(6).trim, attr(7).trim, attr(8).trim, attr(9).trim)
      }).toDF()

    val ratingRDD = spark.sparkContext.textFile(RATING_DATA_PATH)
    val ratingDF = ratingRDD.map(
      item => {
        val atrr = item.split(",")
        Rating(atrr(0).toInt, atrr(1).toInt, atrr(2).toDouble, atrr(3).toInt)
      }).toDF()

    val tagRDD = spark.sparkContext.textFile(TAG_DATA_PATH)
    val tagDF = tagRDD.map(
      item => {
        val atrr = item.split(",")
        Tag(atrr(0).toInt, atrr(1).toInt, atrr(2).trim, atrr(3).toInt)
      }).toDF()

    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))
    //数据存储到MongoDB
    storeDataToMongoDB(movieDF, ratingDF, tagDF)

    //数据预处理  保存到MongoDB已经好了 ES还没有
    import org.apache.spark.sql.functions._

    /**
     * movie + tags
     * through : mid tags
     *
     * tags : tag1|tag2|tag3..
     * */
    val newTag = tagDF.groupBy("mid")
      .agg(concat_ws("|", collect_set("tag")).as("tags"))
      .select("mid", "tags")

    val movieWithTagsDF = movieDF.join(newTag, Seq("mid"), "left")

    implicit val esConfig = ESConfig(config("es.httpHosts"), config("es.transportHosts"), config("es.index"), config("es.cluster.name"))
    //将数据存到ES
    storeDataToES(movieWithTagsDF)

    spark.stop()
  }

    def storeDataToMongoDB(movieDF: DataFrame, ratingDF: DataFrame, tagDF: DataFrame)(implicit mongoConfig: MongoConfig): Unit ={
      // 新建立连接
      val mongoClient = MongoClient(MongoClientURI(mongoConfig.uri))

      // 如果数据库里已经有这些表了，先清空，防止重复插入
      mongoClient(mongoConfig.db)(MONGODB_MOVIE_COLLECTION).dropCollection()
      mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION).dropCollection()
      mongoClient(mongoConfig.db)(MONGODB_TAG_COLLECTION).dropCollection()
      movieDF.write
        .option("uri", mongoConfig.uri)
        .option("collection", MONGODB_MOVIE_COLLECTION)
        .mode("overwrite")
        .format("com.mongodb.spark.sql")
        .save()

      ratingDF.write
        .option("uri", mongoConfig.uri)
        .option("collection", MONGODB_RATING_COLLECTION)
        .mode("overwrite")
        .format("com.mongodb.spark.sql")
        .save()

      tagDF.write
        .option("uri", mongoConfig.uri)
        .option("collection", MONGODB_TAG_COLLECTION)
        .mode("overwrite")
        .format("com.mongodb.spark.sql")
        .save()

      // 为 MongoDB 创建索引（提升后续业务查询速度）
      mongoClient(mongoConfig.db)(MONGODB_MOVIE_COLLECTION).createIndex(MongoDBObject("mid" -> 1))
      mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION).createIndex(MongoDBObject("uid" -> 1))
      mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION).createIndex(MongoDBObject("mid" -> 1))
      mongoClient(mongoConfig.db)(MONGODB_TAG_COLLECTION).createIndex((MongoDBObject("uid" -> 1)))
      mongoClient(mongoConfig.db)(MONGODB_TAG_COLLECTION).createIndex((MongoDBObject("mid" -> 1)))

      mongoClient.close()
    }



    def storeDataToES(movieWithTagsDF: DataFrame)(implicit esConfig: ESConfig): Unit ={
      movieWithTagsDF.write
        .option("es.nodes", esConfig.httpHosts)
        .option("es.http.timeout", "100m")
        .option("es.mapping.id", "mid") // 指定主键，实现天然的覆盖更新（Upsert），无需提前删表
        .option("es.nodes.wan.only", "true")
        .mode("overwrite")
        .format("org.elasticsearch.spark.sql")
        .save(esConfig.index + "/" + ES_MOVIE_INDEX)
    }
}
