package structuredStreaming

import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.types.{ArrayType, StringType, StructField, StructType}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger

import java.util.Properties

object SparkConsumer {

  def main(args: Array[String]): Unit = {

    println("Structured Streaming Application Started....")

    val topicName = "meetuprsvp"
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
      appName("Stream Processing App").
      getOrCreate()

    import spark.implicits._
    spark.sparkContext.setLogLevel("ERROR")
    spark.conf.set("spark.sql.shuffle.partitions", "2")

    val dataDf = spark.
      readStream.
      format("kafka").
      option("kafka.bootstrap.servers", conf.getString("bootstrap.server")).
      option("subscribe", topicName).
      option("startingOffsets", "latest").
      load.
      withColumn("value", $"value".cast("string")).
      withColumn("timestamp", $"timestamp".cast("timestamp"))

    println("Printing the schema of raw DF: ")
    dataDf.printSchema()

    val meetup_rsvp_message_schema = StructType(Array(
      StructField("venue", StructType(Array(
        StructField("venue_name", StringType),
        StructField("lon", StringType),
        StructField("lat", StringType),
        StructField("venue_id", StringType)
      ))),
      StructField("visibility", StringType),
      StructField("response", StringType),
      StructField("guests", StringType),
      StructField("member", StructType(Array(
        StructField("member_id", StringType),
        StructField("photo", StringType),
        StructField("member_name", StringType)
      ))),
      StructField("rsvp_id", StringType),
      StructField("mtime", StringType),
      StructField("event", StructType(Array(
        StructField("event_name", StringType),
        StructField("event_id", StringType),
        StructField("time", StringType),
        StructField("event_url", StringType)
      ))),
      StructField("group", StructType(Array(
        StructField("group_topics", ArrayType(StructType(Array(
          StructField("urlkey", StringType),
          StructField("topic_name", StringType)
        )), containsNull = true)),
        StructField("group_city", StringType),
        StructField("group_country", StringType),
        StructField("group_id", StringType),
        StructField("group_name", StringType),
        StructField("group_lon", StringType),
        StructField("group_urlname", StringType),
        StructField("group_state", StringType),
        StructField("group_lat", StringType)
      )))
    ))

    val meetupDf1 = dataDf.select(from_json(
                                            col("value"), meetup_rsvp_message_schema).
                                  as("message_detail"), col("timestamp"))

    println("Printing the schema of raw DF 1: ")
    meetupDf1.printSchema()

    val meetupDf2 = meetupDf1.select("message_detail.*", "timestamp")
    println("Printing the schema of raw DF 2: ")
    meetupDf2.printSchema()

    val meetupDf3 = meetupDf2.select(col("group.group_name"),
      col("group.group_country"),
      col("group.group_state"),
      col("group.group_city"),
      col("group.group_lat"),
      col("group.group_lon"),
      col("group.group_id"),
      col("group.group_topics"),
      col("member.member_name"),
      col("response"),
      col("guests"),
      col("venue.venue_name"),
      col("venue.lon"),
      col("venue.lat"),
      col("venue.venue_id"),
      col("visibility"),
      col("member.member_id"),
      col("member.photo"),
      col("event.event_name"),
      col("event.event_id"),
      col("event.time"),
      col("event.event_url")
    )
    println("Printing the schema of raw DF 3: ")
    meetupDf3.printSchema()

    val result =  meetupDf3.writeStream.
      trigger(Trigger.ProcessingTime("30 seconds")).
      outputMode("update").
      foreachBatch{ (df: DataFrame, batchId: Long) =>
        val batchDF = df.withColumn("batch_id", lit(batchId))

        batchDF.write.
          format("mongo").
          mode("append").
          option("uri", mongodb_uri).
          option("database", conf.getString("mongodb.dbname")).
          option("collection", conf.getString("mongodb.collection_name")).
          save()
      }.start()

    result.awaitTermination()

//    val res = meetupDf3.writeStream.outputMode("update").format("console").trigger(Trigger.ProcessingTime("20 seconds")).start()
//    res.awaitTermination()

    println("Stream Processing Completed...")
  }
}
