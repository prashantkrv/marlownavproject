package controllers.request

import play.api.libs.json.{Json, Reads}

object RequestUtils {

  case class AccountData(`type`: String, balance: Int, owners: Seq[Int], withdrawalLimit: Int)

  object AccountData {
    implicit val accountDataReads: Reads[AccountData] = Json.reads[AccountData]
  }

  case class DebitData(accountId: Int, userId: Int, amount: Int)

  object DebitData {
    implicit val debitDataReads: Reads[DebitData] = Json.reads[DebitData]
  }

  case class TransferData(senderAccountId: Int, receiverAccountId: Int, senderId: Int, amount: Int, memo: String)

  object TransferData {
    implicit val transferDataReads: Reads[TransferData] = Json.reads[TransferData]
  }

  case class CreditData(accountId: Int, userId: Int, amount: Int)

  object CreditData {
    implicit val creditDataReads: Reads[CreditData] = Json.reads[CreditData]
  }

  case class UserData(name: String, address: String)

  object UserData {
    implicit val userDataReads: Reads[UserData] = Json.reads[UserData]
  }

}

