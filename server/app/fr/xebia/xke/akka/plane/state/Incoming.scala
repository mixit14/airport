package fr.xebia.xke.akka.plane.state

import akka.actor.{Cancellable, ActorRef}
import fr.xebia.xke.akka.airport.PlaneEvent
import fr.xebia.xke.akka.airport.PlaneEvent.{Incoming, HasLanded, OutOfKerozen}
import fr.xebia.xke.akka.airport.command.{Contact, Land}
import languageFeature.postfixOps
import concurrent.duration._
import fr.xebia.xke.akka.plane.Plane
import fr.xebia.xke.akka.Transition

private[plane] trait Incoming extends Plane with RadioCommunication {

  private var outOfKerozenCrash: Cancellable = null

  def landing(airTrafficControl: ActorRef) = State("incoming", behavior = LoggingReceive {
    case msg@this.Landed(runway) =>
      transitionTo(transition = () => {
        airTrafficControl ! HasLanded
        runway ! HasLanded
      })(nextState = waitingToTaxi(airTrafficControl, runway))

  })

  val flying = State("incoming", behavior = LoggingReceive {
    case Land(runway) =>
      val airTrafficControl = sender

      replyWithRadio(to = airTrafficControl)(() => {
        transitionTo(transition = () => {
          outOfKerozenCrash.cancel()

          import context.dispatcher
          context.system.scheduler.scheduleOnce(settings.aLandingDuration, self, Landed(runway))
        })(nextState = landing(airTrafficControl))
      })

    case OutOfKerozen =>
      terminateInError(s"Plane ${self.path.name} is out of kerozen, it crashes")
  })

  val idle = State("incoming", behavior = LoggingReceive {
    case Contact(airTrafficControl) => {

      transitionTo(transition = () => {
        airTrafficControl ! Incoming

        import context.dispatcher
        outOfKerozenCrash = context.system.scheduler.scheduleOnce(settings.outOfKerozenTimeout milliseconds, self, OutOfKerozen)
      })(nextState = flying)
    }
  })

  def waitingToTaxi(airControl: ActorRef, runway: ActorRef): State

  private case class Landed(runway: ActorRef) extends PlaneEvent {
    override def toString = s"Landed on ${runway.path.name}"
  }

}
