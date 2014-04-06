package fr.xebia.xke.akka.plane

import akka.actor.{Props, ActorRef}
import akka.event.EventStream
import fr.xebia.xke.akka.game.Settings
import fr.xebia.xke.akka.plane.state.{TaxiingAsLastStep, OnRunwayWaitingForTaxiway, Incoming}

case class JustTaxiingPlane(airControl: ActorRef, game: ActorRef, settings: Settings, eventStream: EventStream)
  extends Plane
  with Incoming
  with OnRunwayWaitingForTaxiway
  with TaxiingAsLastStep {

  def initialState = idle
}

object JustTaxiingPlane {

  def props(airControl: ActorRef, game: ActorRef, settings: Settings, eventStream: EventStream) =
    Props(classOf[JustTaxiingPlane], airControl, game, settings, eventStream)
}