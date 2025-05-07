package modules

import com.google.inject.{AbstractModule, Provides}
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api._

class DatabaseModule extends AbstractModule{
  @Provides
  def provideDatabase : Database={
    Database.forConfig("slick.db")
  }
}