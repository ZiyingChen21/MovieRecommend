package com.cc

import com.mongodb.casbah.Imports.MongoClientURI
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import redis.clients.jedis.Jedis

//连接助手对象，序列化
object ConnectorHelper extends Serializable{
  lazy val jedis = new Jedis("localhost") // redis connector
  lazy val mongoClient = MongoClient(MongoClientURI("mongodb://localhost:27017/recommend"))
}
case class MongoConfig(uri: String, db: String)
case class MovieRecBase(mid: Int, score: Double)
case class UserRecommendation(uid: Int, recommendations: Seq[MovieRecBase])
case class MovieSimilarity(mid: Int, similarityMovies: Seq[MovieRecBase])

object StreamingRecommend {
  val MAX_USER_RATINGS_NUM = 20
  val MAX_SIM_MOVIES_NUM = 20
  val MONGODB_STREAM_RECOMMENDATION_COLLECTION = "StreamRecommendation"
  val MONGODB_RATING_COLLECTION = "Rating"
  val MONGODB_MOVIE_RECOMMENDATION_COLLECTION = "MovieSimilarity"   // 电影相似度表

  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/recommend",
      "mongo.db" -> "recommend",
      "kafka.topic" -> "recommend"
    )
    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("StreamingRecommend")
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()
    // get streaming context
    val sc = spark.sparkContext
    val ssc = new StreamingContext(sc, Seconds(2)) // batch duration 2s > 500ms
    import spark.implicits._
    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))

    //加载数据 电影的相似度矩阵数据， 将其广播出去 考虑性能 好处：一个exector上会保存一个副本，节省内存资源， 放 redis 更好
    val similarityMovieMatrix = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_MOVIE_RECOMMENDATION_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[MovieSimilarity]
      .rdd
      .map { movieRecommendation =>
        (movieRecommendation.mid, movieRecommendation.similarityMovies.map(x => (x.mid, x.score)).toMap)
      }.collectAsMap()

    //定义广播变量
    val similarityMovieMatrixBroadcast = sc.broadcast(similarityMovieMatrix)

    //连接 卡夫卡  定义连接参数
    val kafkaParam = Map (
      "bootstrap.servers" -> "localhost:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "recommender",
      "auto.offset.reset" -> "latest"
    )

    //通过卡夫卡创建一个 DStream
    val kafkaStream = KafkaUtils.createDirectStream[String, String](ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](Array(config("kafka.topic")), kafkaParam)
    )

    //把原始数据 UID|MID|SCORE|TIMESTAMP 转换成评分流
    val ratingStream = kafkaStream.map {
      msg =>
        val attr = msg.value().split("\\|")
        (attr(0).toInt, attr(1).toInt, attr(2).toDouble, attr(3).toInt)
    }

    // 继续做流式处理，核心实时算法部分 一个时间窗口内的一组 RDD
    ratingStream.foreachRDD {
    rdds =>
      rdds.foreach {
        case (uid, mid, score, timestamp) => {
          println("rating data coming >>>>>>>>>>>>")
          //分布计算
          // 1. 从redis里获取当前用户最近的K次评分，保存成Array[(mid, score)]
          val userRecentRtings = getUserRecentRatings(MAX_USER_RATINGS_NUM, uid, ConnectorHelper.jedis)
          println(s"Debug -> Redis 近期评分数量: ${userRecentRtings.length}")
          // 2. 从相似度矩阵中取出当前电影最相似的N个电影，作为备选列表，Array[mid]
          val candidateMovies = getTopSimilarityMovies(MAX_SIM_MOVIES_NUM, mid, uid, similarityMovieMatrixBroadcast.value)
          println(s"Debug -> 过滤已看后的候选电影数量: ${candidateMovies.length}")
          // 3. 对每个备选电影，计算推荐优先级，得到当前用户的实时推荐列表，Array[(mid, score)]
          val streamRecommendation = computeMovieScores(candidateMovies, userRecentRtings, similarityMovieMatrixBroadcast.value)
          println(s"Debug -> 最终满足 >0.7 阈值的计算结果数量: ${streamRecommendation.length}")
          // 4. 把推荐数据保存到mongodb
          saveDataToMongoDB(uid, streamRecommendation)
        }
      }
    }
    // 开始接收和处理数据
    ssc.start()
    println(">>>>>>>>>>>>>>> streaming started!")
    ssc.awaitTermination()
  }
  // redis操作返回的是java类，为了用map操作需要引入转换类
  import scala.collection.JavaConversions._
  def getUserRecentRatings(num: Int, uid: Int, jedis: Jedis): Array[(Int, Double)] = {
    // 从redis读取数据，用户评分数据保存在 uid:UID 为key的队列里，value是 MID:SCORE
    jedis.lrange("uid:" + uid, 0, num - 1)
      .map{
        item =>  // 具体每个评分又是以冒号分隔的两个值
          val atrr = item.split("\\:")
          (atrr(0).trim.toInt, atrr(1).trim.toDouble)
      }
      .toArray
  }
  /**
   * 获取跟当前电影做相似的num个电影，作为备选电影
   * @param num       相似电影的数量
   * @param mid       当前电影ID
   * @param uid       当前评分用户ID
   * @param similarityMoviesMatrixBoradcast 相似度矩阵
   * @return          过滤之后的备选电影列表
   */
  def getTopSimilarityMovies(num: Int, mid: Int, uid: Int, similarityMovieMatrixBroadcast: collection.Map[Int, scala.collection.immutable.Map[Int, Double]])
                            (implicit mongoConfig: MongoConfig): Array[Int] = {
    // 1. 从相似度矩阵中拿到所有相似的电影
    //在实时流处理中，若用户评分了一部极冷门电影，且该电影在离线阶段未匹配到任何相似电影，广播字典中将不存在该 mid 键，直接读取将导致整个 Spark Streaming 作业崩溃挂掉。
    val allSimilarityMovies = similarityMovieMatrixBroadcast.getOrElse(mid, Map()).toArray //电影 mid 的所有相似电影
    // 2. 从mongodb中查询用户已看过的电影
    val ratingExist = ConnectorHelper.mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION)
      .find(MongoDBObject("uid" -> uid))
      .toArray
      .map{
        item => item.get("mid").toString.toInt
      }
    // 3. 把看过的过滤，得到输出列表
    allSimilarityMovies.filter(x => !ratingExist.contains(x._1))
      .sortWith(_._2 > _._2)
      .take(num)
      .map(x => x._1)
  }

  //计算推荐优先级
  def computeMovieScores(candidateMovies: Array[Int],
                         userRecentRatings: Array[(Int, Double)],
                         similarityMoviesBroadcastMatrix: scala.collection.Map[Int, scala.collection.immutable.Map[Int, Double]]): Array[(Int, Double)] = {
    // 定义一个ArrayBuffer，用于保存每一个备选电影的基础得分
    val scores = scala.collection.mutable.ArrayBuffer[(Int, Double)]()
    // 定义一个HashMap，保存每一个备选电影的增强减弱因子
    val increMap = scala.collection.mutable.HashMap[Int, Int]()
    val decreMap = scala.collection.mutable.HashMap[Int, Int]()
    for(candidateMovie <- candidateMovies; userRecentRatins <- userRecentRatings){
      // 拿到备选电影和最近评分电影的相似度
      val similarityScore = getMovieSimilarityScore(candidateMovie, userRecentRatins._1, similarityMoviesBroadcastMatrix)
      if (similarityScore > 0.7) {
        // 计算备选电影的基础推荐得分 相似度 * 用户评分
        scores += ((candidateMovie, similarityScore * userRecentRatins._2))
        if (userRecentRatins._2 > 3) {
          increMap(candidateMovie) = increMap.getOrElse(candidateMovie, 0) + 1
        } else {
          decreMap(candidateMovie) = decreMap.getOrElse(candidateMovie, 0) + 1
        }
      }
    }
    // 根据备选电影的mid做groupby，根据公式去求最后的推荐评分
    scores.groupBy(_._1).map{
      // groupBy之后得到的数据 Map( mid -> ArrayBuffer[(mid, score)] )
      case (mid, scoreList) =>
        (mid, scoreList.map(_._2).sum / scoreList.length + 0.1 * log(increMap.getOrElse(mid, 1)) - 0.1 * log(decreMap.getOrElse(mid, 1)))
    }.toArray.sortWith(_._2 > _._2)
  }
  // 获取两个电影之间的相似度
  def getMovieSimilarityScore(mid1: Int, mid2: Int, similarityMoviesBroadcastMatrix:scala.collection.Map[Int,
    scala.collection.immutable.Map[Int, Double]]): Double = {
    similarityMoviesBroadcastMatrix.get(mid1) match {
      case Some(sims) => sims.get(mid2) match {
        case Some(score) => score
        case None => 0.0
      }
      case None => 0.0
    }
  }

  def log(m: Int): Double = {
    val N = 10
    math.log(m) / math.log(N)
  }
  def saveDataToMongoDB(uid: Int, streamRecommendation: Array[(Int, Double)])(implicit mongoConfig: MongoConfig): Unit = {
    // 定义到StreamRecs表的连接
    val streamingRecommendCollection = ConnectorHelper.mongoClient(mongoConfig.db)(MONGODB_STREAM_RECOMMENDATION_COLLECTION)
    // 如果表中已有uid对应的数据，则删除
    streamingRecommendCollection.findAndRemove(MongoDBObject("uid" -> uid))
    // 将streamRecs数据存入表中
    streamingRecommendCollection.insert(MongoDBObject("uid" -> uid,
      "recs"-> streamRecommendation.map(x=>MongoDBObject( "mid"->x._1, "score"->x._2 ))))
  }
}
