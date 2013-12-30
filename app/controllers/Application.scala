package controllers

import akka.actor.{Inbox, ActorRef, Props, Actor}
import akka.pattern.ask
import akka.util.Timeout
import concurrent.duration._
import fr.xebia.xke.akka.airport.Game.NewPlane
import fr.xebia.xke.akka.airport.{PlaneEvent, UIEvent, Settings, Game}
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee.Enumerator.TreatCont1
import play.api.libs.iteratee.{Input, Enumerator, Iteratee}
import play.api.mvc._
import scala.Some
import scala.concurrent.{ExecutionContext, Future}
import scala.collection.immutable.Queue

object Application extends Controller {

  def index = Action {
    if (game != null) {
      system.stop(game)
      game = null
    }

    game = system.actorOf(Props(classOf[Game], Settings.EASY))

    Ok(views.html.index())
  }

  def events = WebSocket.using[String] {
    request =>

    // Log events to the console
      import scala.concurrent.ExecutionContext.Implicits.global
      val in = Iteratee.foreach[String](println).map {
        _ =>
          println("Disconnected")
      }

      val out = Enumerator2.infineUnfold(listener) {
        listener =>
          ask(listener, DequeueEvents)(Timeout(1 second))
            .mapTo[Option[String]]
            .map(replyOption => replyOption
            .map(reply => (listener, reply))
          )
      }

      (in, out)
  }

  def newPlane = Action {
    if (game != null) {
      val inbox = Inbox.create(system)
      inbox.send(game, NewPlane)
    }
    Ok
  }

  import play.api.Play.current

  val system = Akka.system

  val listener = system.actorOf(Props[Listener])
  var game: ActorRef = null
  system.eventStream.subscribe(listener, classOf[UIEvent])
}

class Listener extends Actor {

  private var buffer = Queue.empty[String]

  def receive = {
    case PlaneEvent(evt, name) =>
      buffer = buffer enqueue s"$evt:$name"

    case DequeueEvents =>
      if (buffer.nonEmpty) {
        val (msg, newBuffer) = buffer.dequeue
        sender ! Some(msg)
        buffer = newBuffer
      } else {
        sender ! Option.empty[String]
      }
  }
}

case object DequeueEvents

case class GameEvent(message: String)

object Enumerator2 {
  /**
   * Like [[play.api.libs.iteratee.Enumerator.unfold]], but allows the unfolding to be done asynchronously.
   *
   * @param s The value to unfold
   * @param f The unfolding function. This will take the value, and return a future for some tuple of the next value
   *          to unfold and the next input, or none if the value is completely unfolded.
   *          $paramEcSingle
   */
  def infineUnfold[S, E](s: S)(f: S => Future[Option[(S, E)]])(implicit ec: ExecutionContext): Enumerator[E] = Enumerator.checkContinue1(s)(new TreatCont1[E, S] {
    val pec = ec.prepare()

    def apply[A](loop: (Iteratee[E, A], S) => Future[Iteratee[E, A]], s: S, k: Input[E] => Iteratee[E, A]): Future[Iteratee[E, A]] = {
      f(s).flatMap {
        case Some((newS, e)) => loop(k(Input.El(e)), newS)
        case None => Thread.sleep(100); loop(k(Input.Empty), s)
      }(ExecutionContext.global)
    }
  })

}