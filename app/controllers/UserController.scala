package controllers

import controllers.request.RequestUtils.UserData
import play.api.libs.json.{JsError, JsSuccess}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, Request}
import models.User
import play.api.Logger

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserController @Inject()(
                                modelUsers: models.Users,
                                val controllerComponents: ControllerComponents
                              )(
                                implicit executionContext: ExecutionContext,
                              ) extends BaseController {


  private val logger = Logger(this.getClass)

  /** this action method is used for adding a new user
   *
   * Data required
   * Name: name of the user
   * Address: address of the user
   *
   * @return Once the user is created, the request returns or fails with an error response
   */

  def addUser(): Action[AnyContent] = Action.async { request =>
    logger.info(s"Received request for creating user")
    (request.body.asJson match {
      case Some(json) => json.validate[UserData] match {
        case JsSuccess(userData, _) =>
          modelUsers.addUser(User(name = userData.name, address = userData.address)).map { id =>
            logger.info(s"User created with id ${id}")
            Ok(s"User created with id ${id}")
          }

        case JsError(errors) => Future(BadRequest("Invalid data"))
      }
      case None => Future(BadRequest("Invalid data"))
    }).recover {
      case exception: Exception =>
        logger.error("Exception occurred- " + exception.getMessage)
        InternalServerError("Exception Occurred- " + exception.getMessage)
    }
  }

}