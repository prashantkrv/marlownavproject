package modules

import com.google.inject.AbstractModule
import play.api.{Configuration, Environment}
import services.{KafkaConsumerService, KafkaProducerService}

class Module(environment: Environment,configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[KafkaProducerService]).asEagerSingleton()
    bind(classOf[KafkaConsumerService]).asEagerSingleton()
  }
}