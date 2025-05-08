package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import models.{Account, AccountSerialized, Accounts}
import play.api.libs.json.{JsError, JsSuccess, Json, Reads}
import play.api.Logger
import scala.concurrent.{ExecutionContext, Future}
import _root_.controllers.request.RequestUtils.{AccountData, CreditData, DebitData, TransferData}
import services.{KafkaConsumerService, KafkaProducerService}


@Singleton
class AccountController @Inject()(
                                   modelAccounts: models.Accounts,
                                   modelUsers: models.Users,
                                   val controllerComponents: ControllerComponents
                                 )(
                                   implicit executionContext: ExecutionContext,
                                 ) extends BaseController {

  private val logger = Logger(this.getClass)

  /**
   * This controller contains action methods to handle various methods
   * related to a customer's accounts, whether it's creating an account,
   * debiting, crediting or transferring money. Each one of the action methods
   * are built for handling a particular action.
   *
   * NOTE: A few things about this controller.
   * -The actions in this controller do assume that the user is signed in.
   * Handling user verification will require a whole other module.
   * - For simplicity, Int datatype has been used to denote balance,
   * instead of Double datatype or any other floating variable which would be ideal.
   * - Not all the edge cases are covered, the application may break for some wrong values, just
   * focused on basic functionalities
   */


  /**
   * The below action method creates a basic Account for a user
   * data required
   *
   * @param IDs             :the ids of users who will own the account,
   * @param Type            : type of account
   * @param Balance         : intial balance of the account at creation
   * @param WithdrawalLimit - what's the maximum amount of money a User can withdraw in one go
   * @return Ok 200 when the action is successful, it returns with AccountID of the account created,
   *         otherwise request fails and returns with error
   */
  def createAccount(): Action[AnyContent] = Action.async { request =>
    logger.info(s"Received request for creating new Account")
    request.body.asJson match {
      case Some(json) => json.validate[AccountData] match {
        case JsSuccess(accountData, _) =>

          val checkAllOwnersExist = modelUsers.checkUsersExist(accountData.owners)

          def createAccount(allOwnersExist: Boolean): Future[Int] =
            if (allOwnersExist) {
              modelAccounts.createAccount(Account(`type` = accountData.`type`, balance = accountData.balance, owners = accountData.owners, withdrawalLimit = accountData.withdrawalLimit))
            } else {
              throw new Exception(s"All the users ${accountData.owners} are not present")
            }

          (for {
            allOwnersExist <- checkAllOwnersExist
            id <- createAccount(allOwnersExist)
          } yield {
            logger.info(s"Account has been created for ${accountData.owners.toString()} with id ${id}")
            Ok(s"Account Created with id ${id}")
          }
            ).recover {
            case exception: Exception =>
              logger.error("Exception occurred- " + exception.getMessage)
              InternalServerError("Unexpected Error occurred-" + exception.getMessage)
          }
        case JsError(errors) => Future(BadRequest(errors.toString()))
      }
      case None => Future(BadRequest("the data is not valid"))
    }
  }


  /**
   * This method is used for sending a debit request to an account to debit some amount.
   * It reduces the balance of the account by the given amount after performing
   * various checks and verifying the sender id is authorized to access that account
   * Note: checks like login/password verification is not done
   * because that wall requires another module for authentication and authorization mechanisms
   *
   * @param userID    : the userID of the owner of the account
   * @param accountID : the id of the account from which the balance will be debited
   * @param amount    : the amount that needs to be deducted
   *
   *                  Note: the transfer function is written such that two concurrent requests are
   *                  handled one after the other  and there is no double spending problem
   *                  Check the transfer function for more details on how it works(it uses the forUpdate feature for updating one row at a time, thus avoiding parallelism issues)
   * @return Once all the checks are passed, the balanced is deducted from account
   *         otherwise request fails and returns with error
   */
  def debit(): Action[AnyContent] = Action.async { request =>
    request.body.asJson match {
      case Some(json) => json.validate[DebitData] match {
        case JsSuccess(debitData, _) =>
          logger.info(s"received request for debiting from Account ${debitData.accountId}, debit amount ${debitData.amount}")
          modelAccounts.debit(debitData.accountId, debitData.userId, debitData.amount).map { _ =>
            logger.info(s"Amount debited from Account ${debitData.accountId}, debit amount ${debitData.amount}")
            Ok("Your balance has been updated")
          }.recover {
            case exception: Exception =>
              logger.error("Debit Failed. Exception occurred- " + exception.getMessage)
              InternalServerError("Debit Failed. Exception occured- " + exception.getMessage)
          }
        case JsError(errors) => Future(BadRequest(errors.toString()))
      }
      case None => Future(BadRequest("the data is not valid"))
    }
  }

  /**
   * this method is used for sending a credit request to an account to credit some amount
   * it increases the balance of the account by the given amount after performing
   * various check
   * Note: checks like login/password verification is not done as mentioned earlier
   *
   * data required
   *
   * @param userID    : the userID of sender
   * @param accountID : the id of the account in which the balance will be credited
   * @param amount    : the amount that needs to be credited
   * @return Once all the checks are passed, the balanced is credited from account
   *         otherwise request fails and returns with error
   */

  def credit(): Action[AnyContent] = Action.async { request =>
    request.body.asJson match {
      case Some(json) => json.validate[CreditData] match {
        case JsSuccess(creditData, _) =>
          logger.info(s"received request to credit Account ${creditData.accountId}, with credit amount ${creditData.amount}")
          val account = modelAccounts.getAccount(creditData.accountId)

          def creditAndUpdateAmount(account: AccountSerialized) = modelAccounts.updateBalance(id = account.id, newBalance = account.balance + creditData.amount)

          (for {
            accountResult <- account
            _ <- creditAndUpdateAmount(accountResult)
          } yield {
            logger.info(s"Amount credited to Account ${creditData.accountId}, credit amount ${creditData.amount}")
            Ok("Your balance has been updated")
          }
            ).recover {
            case exception: Exception =>
              logger.error("Credit Failed. Exception occurred- " + exception.getMessage)
              InternalServerError("Credit Failed. Exception occurred- " + exception.getMessage)
          }
        case JsError(errors) => Future(BadRequest(errors.toString()))
      }
      case None => Future(BadRequest("the data is not valid"))
    }
  }

  /**
   * this method is used for sending a transfer request by a user to transfer some amount
   * from their account to another account
   * it reduces the balance of the sender account by the given amount
   * and increases the balance of the receiver account by the same
   * after performing various checks and verifying the sender id is authorized to access that account
   * Note: checks like login/password verification is not done as mentioned earlier
   *
   * Data required
   *
   * @param senderAccountID   : the accountID of the sender from which th balance will be deducted
   * @param receiverAccountId : the accountID of the receiver in which the balance will be credited
   * @param senderID          : the userID of the sender who is owner of the sendingAccount
   * @param amount            : the amount that needs to be transferred
   *
   *                          Note: the transfer function is written such that two concurrent requests are
   *                          handled one after the other  and there is no double spending problem
   *                          Check the transfer function for more details on how it works
   * @return Once all the checks are passed, the balanced is deducted from sender account and credited in receiver account
   *         otherwise request fails and returns with error
   */
  def transfer(): Action[AnyContent] = Action.async { request =>
    request.body.asJson match {
      case Some(json) => json.validate[TransferData] match {
        case JsSuccess(transferData, _) =>
          logger.info(s"received request for transferring from Account ${transferData.senderAccountId}, to Account ${transferData.receiverAccountId}, transfer amount ${transferData.amount}")
          val senderAccount = modelAccounts.getAccount(transferData.senderAccountId)
          val receiverAccount = modelAccounts.getAccount(transferData.receiverAccountId)

          def transferAmount(senderAccount: Account, receiverAccount: Account) = {

            if(transferData.senderAccountId == transferData.receiverAccountId){
              throw new Exception("Sender account and receiver account cannot be the same")
            }else if (!senderAccount.owners.contains(transferData.senderId)) {
              throw new Exception(s"the user ${transferData.senderId} is not authorised to access account ${senderAccount.id} ")
            } else if (senderAccount.balance <= transferData.amount) {
              throw new Exception(s"Balance for account ${senderAccount.id} is not enough")
            } else if (senderAccount.withdrawalLimit < transferData.amount) {
              throw new Exception(s"Cannot withdraw more then ${senderAccount.withdrawalLimit} for account ${senderAccount.id}")
            } else {
              modelAccounts.transfer(senderAccountId = senderAccount.id, receiverAccountId = receiverAccount.id, amount = transferData.amount)
            }
          }

          (for {
            senderAccountSerialized <- senderAccount
            receiverAccountSerialized <- receiverAccount
            _ <- transferAmount(senderAccountSerialized.deserialize, receiverAccountSerialized.deserialize)
          } yield {
            logger.info(s"Amount transferred from Account ${transferData.senderAccountId}, to receiver ${transferData.receiverAccountId}, transfer amount ${transferData.amount}")
            Ok(s"Amount ${transferData.amount} transferred from sender ${transferData.senderAccountId} to receiver ${transferData.receiverAccountId}")
          }
            ).recover {
            case exception: Exception =>
              logger.error("Transfer Failed. Exception occurred- " + exception.getMessage)
              InternalServerError(s"Transfer Failed. Exception occurred ${exception.getMessage} ")
          }
        case JsError(errors) => Future(BadRequest("Failed to parse json " + errors.toString()))
      }
      case None => Future(BadRequest("the data is not valid"))
    }
  }
}