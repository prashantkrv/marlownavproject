package models

import slick.jdbc.PostgresProfile.api._
import slick.jdbc.JdbcBackend.Database
import scala.concurrent.{ExecutionContext, Future}
import javax.inject._
import play.api._
import play.api.libs.json.{Json, Reads}
import java.sql.Timestamp

/**
 * This case class specifies that what an Account entity is and its members that'll be stored.
 * Note: it will first be converted to AccountSerialized before storing because Postgresql has trouble storing Array of Elements
 * @param id
 * @param `type`
 * @param balance
 * @param owners
 * @param withdrawalLimit
 * @param createdAt
 * @param updatedAt
 */

case class Account(id: Int = 0, `type`: String, balance: Int, owners: Seq[Int], withdrawalLimit: Int, createdAt: Option[Timestamp] = None, updatedAt: Option[Timestamp] = None) {
  def serialize: AccountSerialized = AccountSerialized(id = id, `type` = `type`, balance = balance, owners = Json.stringify(Json.toJson(owners)), withdrawalLimit = withdrawalLimit, createdAt = createdAt, updatedAt = updatedAt)
}

case class AccountSerialized(id: Int, `type`: String, balance: Int, owners: String, withdrawalLimit: Int, createdAt: Option[Timestamp] = None, updatedAt: Option[Timestamp] = None) {
  def deserialize: Account = Account(id = id, `type` = `type`, balance = balance, owners = Json.parse(owners).as[Seq[Int]], withdrawalLimit = withdrawalLimit, createdAt = createdAt, updatedAt = updatedAt)
}

 /**
 * This class is singleton and is responsible for interacting with the database, specifically Accounts table
 * Contains all the query functions that'll be sent to the db
 * @param db
 * @param executionContext
 */

@Singleton
class Accounts @Inject()(
                          db: Database
                        )(implicit executionContext: ExecutionContext) {

  private val accounts = TableQuery[AccountTable]

  /**
   *
   * */
  def createAccount(account: Account) = db.run(accounts returning accounts.map(_.id) += account.serialize).recover{
    case exception: Exception => throw new Exception(s"Exception occurred while creating Account ${exception.getMessage}")
  }

  def getAccount(id: Int): Future[AccountSerialized] = db.run(accounts.filter(_.id === id).result.head).recover {
    case noSuchElementException: NoSuchElementException => throw new Exception(s"Account with ID- ${id} not found")
    case exception: Exception => throw new Exception(s"Exception occurred  for accountID ${id} ${exception.getMessage}")
  }

  def updateBalance(id:Int, newBalance:Int)= db.run(accounts.filter(_.id === id).map(_.balance).update(newBalance)).recover {
    case exception: Exception => throw new Exception(s"Exception occurred  while updating balance for ${id} ${exception.getMessage}")
  }


   /**
    * This debit function is written such that the operation is atomic and two concurrent debit processes run synchronously
    * avoiding the double spending issue by concurrent debit requests. It uses the forUpdate feature which blocks the
    * row for which the operation needs to be performed. And all sql operations are performed as a single transaction,
    * so either the whole operation is committed or it rollbacks and no changes are done.
    *
    * @param accountId
    * @param userId
    * @param amount
    * @return it returns after a successful debit tx is done, or fails with error
    */
  def debit(accountId: Int, userId: Int, amount: Int) = {
    val action = accounts.filter(_.id === accountId).forUpdate.result.headOption.flatMap {
      case Some(account) =>
        if (account.deserialize.owners.contains(userId)) {
          if (account.balance < amount) {
            DBIO.failed(new Exception("Insufficient Balance"))
          } else if(amount > account.withdrawalLimit ){
            DBIO.failed(new Exception("Can debit above withdrawal limit"))
          }else{
            accounts.filter(_.id === account.id).map(_.balance).update(account.balance - amount)
          }
        } else DBIO.failed(new Exception("Unauthorized"))
      case None => DBIO.failed(new Exception("Account Not Found"))
    }
    db.run(action.transactionally).recover{
      case exception: Exception => throw new Exception(s"Error while debiting balance from ${accountId} : ${exception.getMessage}")
    }
  }

   /**
    * This transfer function is written such that the operation is atomic and two concurrent transfer processes run synchronously
    * avoiding the double spending issue by concurrent transfer requests.
    * Its a collection of sql statements put together and run as a transaction.
    * It uses the forUpdate feature which blocks the rows for which the operation needs to be performed.
    * And all sql operations are performed as a single transaction, so either
    * the whole operation is committed or it fails and rollbacks, so no changes are done.
    *
    * @param senderAccountId
    * @param receiverAccountId
    * @param amount
    * @return it returns after a successful transfer tx is done, or fails with error
    */

  def transfer(senderAccountId: Int, receiverAccountId: Int, amount: Int) = {
    val action = {
      for {
        senderAccount <- accounts.filter(_.id === senderAccountId).forUpdate.result.head
        receiverAccount <- accounts.filter(_.id === receiverAccountId).forUpdate.result.head
      } yield {
        if (senderAccount.balance >= amount && senderAccount.withdrawalLimit >= amount) {
          DBIO.seq(accounts.filter(_.id === senderAccountId).map(_.balance).update(senderAccount.balance-amount),
          accounts.filter(_.id === receiverAccountId).map(_.balance).update(receiverAccount.balance+amount)
          )
        } else {
          DBIO.failed(new Exception("Transfer Failed"))
        }
      }
    }
      db.run(action.transactionally.flatten).recover{
        case exception: Exception => throw new Exception(s"Exception occurred  while transferring balance from account ${senderAccountId} to account ${receiverAccountId} ${exception.getMessage}")
      }
  }


  private class AccountTable(tag: Tag) extends Table[AccountSerialized](tag, "Accounts") {
    def id = column[Int]("id", O.AutoInc, O.PrimaryKey)

    def `type` = column[String]("type")

    def balance = column[Int]("balance")

    def owners = column[String]("owners")

    def withdrawalLimit = column[Int]("withdrawalLimit")

    def createdAt = column[Timestamp]("createdAt")

    def updatedAt = column[Timestamp]("updatedAt")

    def * = (id, `type`, balance, owners, withdrawalLimit, createdAt.?, updatedAt.?) <> (AccountSerialized.tupled, AccountSerialized.unapply)
  }
}


