package models

import com.google.inject.Singleton
import org.postgresql.util.PSQLException
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api._

import java.sql.Timestamp
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


/**
 * This case class specifies that what a User entity is and its members that'll be stored.
 *
 * @param id
 * @param name
 * @param address
 * @param createdAt
 * @param updatedAt
 */

case class User(id: Int = 0, name: String, address: String, createdAt: Option[Timestamp] = None, updatedAt: Option[Timestamp] = None)


/**
 * This class is singleton and is responsible for interacting with the database, specifically Users table
 * Contains all the query functions that'll be sent to the db
 *
 * @param db
 * @param executionContext
 */
@Singleton
class Users @Inject()(db: Database)(implicit executionContext: ExecutionContext) {

  private val users = TableQuery[UserTable]

  def getUser(id: Int) = db.run(users.filter(_.id === id).result.headOption)

  def addUser(user: User): Future[Int] = db.run(users returning users.map(_.id) += user).recover {
    case psqlLException: PSQLException => throw new Exception("psql Exception Occured-" + psqlLException.getMessage)
    case exception: Exception => throw new Exception("Unknown Exception Occured-" + exception.getMessage)
  }

  def checkUsersExist(ids: Seq[Int]): Future[Boolean] = db.run(users.filter(_.id.inSet(ids)).exists.result).recover {
    case exception: Exception => throw new Exception("Unknown Exception Occured-" + exception.getMessage)
  }

  private class UserTable(tag: Tag) extends Table[User](tag, "Users") {

    def id = column[Int]("id", O.AutoInc, O.PrimaryKey)

    def name = column[String]("name")

    def address = column[String]("address")

    def createdAt = column[Timestamp]("createdAt")

    def updatedAt = column[Timestamp]("updatedAt")

    def * = (id, name, address, createdAt.?, updatedAt.?) <> (User.tupled, User.unapply)
  }
}