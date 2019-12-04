package kafka

import java.net.URL
import java.util.Properties

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}

object KafkaProducer {

  def main(args: Array[String]): Unit = {

    println("Kafka Producer Application Started....")

    val topicName = "meetuprsvp"
    val api_endpoint = "http://stream.meetup.com/2/rsvps"

    val url = new URL(api_endpoint)
    val jsonFactoryObj = new JsonFactory(new ObjectMapper)
    val parserObj = jsonFactoryObj.createParser(url.openConnection().getInputStream)

    val conf = ConfigFactory.load
    val envProps = conf.getConfig(args(0))

    val props = new Properties
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
      envProps.getString("bootstrap.server"))
    props.put(ProducerConfig.CLIENT_ID_CONFIG,
      "ProducerExample")
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
      "org.apache.kafka.common.serialization.StringSerializer")
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
      "org.apache.kafka.common.serialization.StringSerializer")
    props.put(ProducerConfig.ACKS_CONFIG, "all")
    props.put("enable.auto.commit", "true")
    props.put("auto.commit.interval.ms", "1000")
    props.put("session.timeout.ms", "30000")

    val producer = new KafkaProducer[String, String](props)
    while(parserObj.nextToken() != null) {
      val messageRecord = parserObj.readValueAsTree().toString
      println(messageRecord)
      val producerRecordObj = new ProducerRecord[String, String](topicName, messageRecord)
      producer.send(producerRecordObj)
    }

    producer.close()
  }

}
