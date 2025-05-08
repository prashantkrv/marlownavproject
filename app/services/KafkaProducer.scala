package services

import play.api.{Configuration, Logger}

import java.util.Properties
import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}


/**
 * This KafkaProducerService class is responsible for producing messages for the kafka messaging queues.
 * NOTE: Not currently using it with the Apis. Just a prototype to show, how it'll work.
 *
 */
@Singleton
class KafkaProducerService @Inject()(configuration: Configuration,
                                     implicit val ec: ExecutionContext,
                                    ) {
  private val logger = Logger(this.getClass)
  private val bootstrapServers = configuration.get[String]("kafka.bootstrap.servers")
  private val topic = configuration.get[String]("kafka.topic")

  private val producerProperties = new Properties()
  producerProperties.put("bootstrap.servers", bootstrapServers)
  producerProperties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  producerProperties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

  private val producer = new KafkaProducer[String, String](producerProperties)

  /**
   * messages here will be sent to the kafkaConsumer
   * */
  def send(message: String) = Future{
    logger.info(s"sending message ${message} of topic ${topic} to kafka")
    val record = new ProducerRecord[String, String](topic, message)
    producer.send(record)
    producer.flush()
    logger.info(s"message sent to kafka")
  }

  def close() = producer.close()

}