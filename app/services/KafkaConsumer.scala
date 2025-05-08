package services

import play.api.{Configuration, Logger}

import java.util.{Collections, Properties}
import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}
import org.apache.kafka.clients.consumer.{ConsumerRecord, ConsumerRecords, KafkaConsumer}
import play.api.libs.json.JsValue
import org.apache.pekko.actor.ActorSystem
import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.IterableHasAsScala
import com.typesafe.config.ConfigFactory


/**
 * This KafkaConsumerService class is responsible for consuming messages from the kafka messaging queues.
 * NOTE: Not currently using it with the Apis. Just a prototype to show, how it'll work.
 *
 */
@Singleton
class KafkaConsumerService @Inject()(configuration: Configuration,
                                     actorSystem: ActorSystem,
                                     implicit val ec: ExecutionContext,
                                    ) {

  private val logger = Logger(this.getClass)
  private val bootstrapServers = configuration.get[String]("kafka.bootstrap.servers")
  private val topic = configuration.get[String]("kafka.topic")
  private val groupId = configuration.get[String]("kafka.consumer.group.id")


  private val consumerProperties = new Properties()
  consumerProperties.put("bootstrap.servers", bootstrapServers)
  consumerProperties.put("group.id", groupId)
  consumerProperties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  consumerProperties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  consumerProperties.put("auto.offset.reset", "earliest")

  private val consumer = new KafkaConsumer[String, String](consumerProperties)
  consumer.subscribe(Collections.singletonList(topic))


  /**
   * messages will be consumed and processed here after polling kafka at regular intervals
   */
  private val runnable = new Runnable {
    override def run(): Unit = {
      val records: ConsumerRecords[String, String] = consumer.poll(java.time.Duration.ofMillis(1000))
      records.asScala.map { record => record.value() }.toList.map { message =>
        message match {
          /**
           * Logic needs to be written here for various types of messages
           */
          case "debit" => ()
          case "credit" => ()
          case _ => ()
        }
      }
    }
  }

  //run a scheduler here to poll for messages at fixed intervals
  //actorSystem.scheduler.scheduleAtFixedRate(5.second, 1.seconds)(runnable)

  def consume(): List[String] = {
    val records: ConsumerRecords[String, String] = consumer.poll(java.time.Duration.ofMillis(1000))
    records.asScala.map(_.value()).toList
  }
}