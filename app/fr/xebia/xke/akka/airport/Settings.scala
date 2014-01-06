package fr.xebia.xke.akka.airport

import scala.concurrent.duration._
import scala.util.Random

case class Settings(
                     nrOfRunways: Int,
                     taxiwayCapacity: Int,
                     nrOfGates: Int,
                     landingMaxDuration: Int,
                     taxiingDuration: Int,
                     unloadingPassengersMaxDuration: Int,
                     outOfKerozenTimeout: Int,
                     ackMaxDuration: Int,
                     radioReliability: Double,
                     objective: Int,
                     planeGenerationInterval: Int) {

  private def aRandomDuration(maxDurationInMillis: Int): FiniteDuration = {
    val minDuration = maxDurationInMillis / 2
    val random = Random.nextInt(minDuration)

    Duration(
      random + minDuration,
      MILLISECONDS
    )
  }

  def aLandingDuration =
    aRandomDuration(landingMaxDuration)

  def anUnloadingPassengersDuration =
    aRandomDuration(unloadingPassengersMaxDuration)

  def aRandomAckDuration =
    aRandomDuration(ackMaxDuration)

  def isRadioOk =
    Random.nextDouble() <= radioReliability

}

object Settings {
  lazy val EASY = new Settings(
    nrOfRunways = 1,
    taxiingDuration = 1500,
    taxiwayCapacity = 1,
    nrOfGates = 1,
    landingMaxDuration = 3000,
    unloadingPassengersMaxDuration = 5000,
    outOfKerozenTimeout = 10000,
    ackMaxDuration = 500,
    radioReliability = 1,
    objective = 20,
    planeGenerationInterval = 8500
  )

  lazy val TEST = new Settings(
    nrOfRunways = 1,
    taxiingDuration = 10,
    taxiwayCapacity = 1,
    nrOfGates = 1,
    landingMaxDuration = 300,
    unloadingPassengersMaxDuration = 500,
    outOfKerozenTimeout = 1000,
    ackMaxDuration = 100,
    radioReliability = 1,
    objective = 20,
    planeGenerationInterval = 200
  )
}
