package controllers

import play.api.mvc.{AnyContent, Action, Controller}
import play.api.data.Form
import play.api.data.Forms._
import scala.Some


trait SecuredController extends Controller {

  var users = Users.empty
  var systems = Systems.empty

  def currentUser(session: play.api.mvc.Session) =
    for {teamMail <- session.get("email")
         userInfo <- users.get(teamMail)
    } yield userInfo


  def LoggedInAction(checkedAction: (UserInfo => play.api.mvc.Request[_] => play.api.mvc.SimpleResult)): play.api.mvc.Action[play.api.mvc.AnyContent] = Action {
    implicit request =>
      currentUser(session) match {
        case Some(user) =>
          checkedAction(user)(request)
        case None =>
          Ok(views.html.register(currentHost(request)))
      }
  }

  def register = Action {
    implicit request => {

      currentUser(session) match {

        case Some(_) =>

          Redirect(routes.Application.index())

        case None => {

          val form: Form[String] = Form(single("email" -> email))
          val teamMail = form.bindFromRequest().get

          if (users.contains(teamMail)) {

            Conflict(views.html.register(currentHost(request)))

          } else {

            val userHost = currentHost(request)
            val userInfo = UserInfo(teamMail, userHost, systems.get(userHost))

            users += (teamMail -> userInfo)

            Redirect(routes.Application.level0).withSession("email" -> teamMail)
          }
        }
      }
    }
  }


  def currentHost(request: play.api.mvc.Request[_]): String = {
    request.host.split(":").head
  }

  def index: Action[AnyContent]
}