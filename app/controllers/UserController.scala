package controllers

import controllers.request.RequestUtils.UserData
import play.api.libs.json.{JsError, JsSuccess}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, Request}
import models.User

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


  /** this action method is used for adding a new user
   *
   * Data required
   * Name: name of the user
   * Address: address of the user
   *
   * @return Once the user is created, the request returns or fails with an error response
   */

  def addUser(): Action[AnyContent] = Action.async { request =>
    (request.body.asJson match {
      case Some(json) => json.validate[UserData] match {
        case JsSuccess(userData, _) =>
          modelUsers.addUser(User(name = userData.name, address = userData.address)).map { id =>
            Ok(s"User Created with id ${id}")
          }

        case JsError(errors) => Future(BadRequest("Invalid data"))
      }
      case None => Future(BadRequest("Invalid data"))
    }).recover {
      case exception: Exception => InternalServerError("Error Occurred- " + exception.getMessage)
    }
  }

}