package com.cc

import breeze.numerics.sqrt
import org.apache.spark.SparkConf
import org.apache.spark.mllib.evaluation.RegressionMetrics
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD
import org.apache.spark.rdd.RDD.numericRDDToDoubleRDDFunctions
import org.apache.spark.sql.SparkSession

object ALSParameterModel {
  //只需要参数计算 提取数据 不需要存储 加载数据计算就可以 别的地方不会调用 用Object
  val MONGODB_RATING_COLLECTION = "Rating"
  def main(args: Array[String]): Unit ={
    val config = Map(
      "spark.cores" -> "local[4]",
      "mongo.uri" -> "mongodb://localhost:27017/recommend",
      "mongo.db" -> "recommend"
    )
    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("ALSParameterModel")
      // 给Driver拉满内存
      .set("spark.driver.memory", "4g")
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")

    val spark = SparkSession.builder().config(sparkConf).getOrCreate()
    // 屏蔽大量无用的 INFO 日志，防止看漏报错
    spark.sparkContext.setLogLevel("WARN")

    import spark.implicits._
    val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))
    val ratingRDD = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[MovieRating]
      .rdd
      .map(rating => Rating(rating.uid, rating.mid, rating.score))
      .cache() //缓存全量数据
    //随机切分数据集 生成训练集和测试集 randomSplit
    val splits = ratingRDD.randomSplit(Array(0.8, 0.2))
    val traingRDD = splits(0).cache()   //缓存训练集
    val testRDD = splits(1).cache()     //缓存测试集

    //模型参数选择 输出最有参数
    adjustALSParameter(traingRDD, testRDD, spark, mongoConfig)
    spark.close()
  }
  def adjustALSParameter(trainData: RDD[Rating], testData: RDD[Rating], spark: SparkSession, mongoConfig: MongoConfig ): Unit = {
    //创建一个可变的 List 保存结果，避免使用 yield 防止血统堆积
    //防止因为RDD延迟计算，导致一次性计算多个循环中的RDD转换操作，全部堆叠起来会形成一个巨大的执行计划链
    var bestRmse = Double.MaxValue
    var bestRank = 0
    var bestLambda = 0.0
    val ranks = Array(20, 50, 100, 150, 200)
    val lambdas = Array(0.001,0.01,0.1)
    for (rank <- ranks; lambda <- lambdas) {
      println(s"========== 开始评估参数: rank = $rank, lambda = $lambda ==========")
      val model = new org.apache.spark.mllib.recommendation.ALS()
        .setRank(rank)
        .setIterations(15)
        .setLambda(lambda)
        .run(trainData)
      val rmse = getRMSE(model, testData)
      println(s"当前参数 RMSE: $rmse")
      if (rmse < bestRmse) {
        bestRmse = rmse
        bestRank = rank
        bestLambda = lambda
      }
      // 强制解除当前模型的内存占用，防止栈溢出或 OOM
      model.userFeatures.unpersist()
      model.productFeatures.unpersist()
    }
    println(s"最优参数组合 -> rank: $bestRank, lambda: $bestLambda, RMSE: $bestRmse")

    import spark.implicits._

    // 构建 DataFrame
    val paramDF = Seq(("ALS_Movie_Model", bestRank, bestLambda, bestRmse))
      .toDF("model_name", "rank", "lambda", "rmse")

    // 写入 MongoDB
    paramDF.write
      .option("uri", "mongodb://localhost:27017/recommend")
      .option("collection", "ModelParams")
      .mode("overwrite") // 每次算完覆盖旧参数
      .format("com.mongodb.spark.sql")
      .save()

    println("最新最优参数已成功同步至 MongoDB 的 ModelParams 集合!")
  }

  //使用 Spark 内置的 RegressionMetrics 计算 RMSE，更稳定且不会爆栈
  def getRMSE(model: MatrixFactorizationModel, data: RDD[Rating]): Double = {
    //提取测试数据 (uid, mid)
    val userProducts = data.map(item => (item.user, item.product))
    val predictRating = model.predict(userProducts).map {
      case Rating(user, product, rating) => ((user, product), rating)
    } // RDD
    //以 uid mid 为外键，inner join实际观测值和预测值
    val observed = data.map(item => ((item.user, item.product), item.rating))
    //内连接得到 （uid,mid),actual, predict

    val predictAndActual = predictRating.join(observed).values //RegressionMetrics 要求 RDD [(预测值，真实值)]

    val metrics = new RegressionMetrics(predictAndActual)
    metrics.rootMeanSquaredError
  }
}
