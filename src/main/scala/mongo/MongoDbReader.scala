package mongo

import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.types.{ArrayType, StringType, StructField, StructType}
import org.apache.spark.sql.functions._

object MongoDbReader {

  def main(args: Array[String]): Unit = {

    val conf = ConfigFactory.load.getConfig(args(0))

    val mongodb_uri = "mongodb://" +
      conf.getString("mongodb.username") + ":" +
      conf.getString("mongodb.password") + "@" +
      conf.getString("mongodb.hostname") + ":" +
      conf.getString("mongodb.port_no") + "/?authSource=" +
      conf.getString("mongodb.dbname")

    println("Printing MongoDB URI : " + mongodb_uri)

    val spark = SparkSession.
      builder.
      master(conf.getString("execution.mode")).
      appName("Read from MongoDb").
      getOrCreate()

    val df = spark.read.
      format("mongo").
      option("uri", mongodb_uri).
      option("database", conf.getString("mongodb.dbname")).
      option("collection", conf.getString("mongodb.collection_name")).
      load()

    df.createOrReplaceTempView("meetup_message")

    val result = spark.sql("select group_country, response, count(*) as response_count from meetup_message " +
      "group by group_country, response order by response_count desc limit 20")

    result.show()
  }
}
