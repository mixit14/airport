package fr.xebia.xke.akka.airport

import akka.actor.Props
import akka.testkit.TestProbe
import concurrent.duration._
import fr.xebia.xke.akka.airport.Command.{Land, Ack, Contact}
import fr.xebia.xke.akka.airport.GameEvent.{Score, HasParked, TaxiingToGate, HasLeft, StartTaxi, HasLanded, Incoming}
import fr.xebia.xke.akka.airport.specs.ActorSpecs
import languageFeature.postfixOps
import org.scalatest.ShouldMatchers

class PlaneSpec extends ActorSpecs with ShouldMatchers {

  val settings = Settings.TEST

  `Given an actor system` {
    implicit system =>

      "Given a plane" - {

        "When it starts" - {

          "Then it should contact the aircontrol" in {
            val game = TestProbe()
            val airControl = TestProbe()

            system.actorOf(Props(classOf[Plane], airControl.ref, game.ref, settings), "plane")

            airControl expectMsg Incoming
          }
        }
      }
  }

  `Given an actor system` {
    implicit system =>

      "Given a plane" - {

        "Given the radio fability is 0.5" - {

          "When airControl ask for a command" - {

            "Ask is received half of the time" in {
              val airControl = TestProbe()

              for (_ <- 1 to 10) {
                val plane = system.actorOf(Props(classOf[Plane], airControl.ref, TestProbe().ref, settings.copy(radioFability = 0.5, ackMaxDuration = 10)))

                TestProbe().send(plane, Land(TestProbe().ref))

              }

              airControl.receiveWhile(200 milliseconds) {
                case Ack => 1
                case _ => 0
              }.sum should (equal(4) or equal(5) or equal(6))
            }
          }

        }
      }
  }


  `Given an actor system` {
    implicit system =>

      "Given a plane" - {

        "When it runs out of kerozen" - {

          "Then it should terminates" in {
            val game = TestProbe()
            val airControl = TestProbe()
            val plane = system.actorOf(Props(classOf[Plane], airControl.ref, game.ref, settings), "plane")

            val probe = TestProbe()
            probe watch plane

            probe expectTerminated(plane, (2 * settings.outOfKerozenTimeout).milliseconds)
          }
        }
      }
  }

  `Given an actor system` {
    implicit system =>

      "Given a flying plane" - {

        "When the airControl request the plane to land on the runway" - {

          "Then the plane should ack and land withing timeout" in {
            //Given
            val game = TestProbe()
            val airControl = TestProbe()
            val runway = TestProbe()
            system.actorOf(Props(classOf[Plane], airControl.ref, game.ref, settings), "plane")
            airControl expectMsg Incoming

            //When
            airControl reply Command.Land(runway.ref)

            //Then
            airControl expectMsg(2 * settings.ackMaxDuration.milliseconds, Ack)
            airControl expectMsg(2 * settings.landingMaxDuration.milliseconds, HasLanded)
            runway expectMsg(2 * settings.landingMaxDuration.milliseconds, HasLanded)
          }
        }
      }
  }

  `Given an actor system` {
    implicit system =>

      "Given a landed plane" - {

        "When the plane is requested to contact ground control" - {

          "Then the plane should make the contact" in {
            //Given
            val game = TestProbe()
            val airControl = TestProbe()
            val groundControl = TestProbe()
            val plane = system.actorOf(Props(classOf[Plane], airControl.ref, game.ref, settings), "plane")

            airControl expectMsg Incoming
            airControl reply Command.Land(TestProbe().ref)
            airControl expectMsg(2 * settings.ackMaxDuration.milliseconds, Ack)
            airControl expectMsg(2 * settings.landingMaxDuration.milliseconds, HasLanded)

            //When
            airControl reply Contact(groundControl.ref)

            //Then
            airControl expectMsg(2 * settings.ackMaxDuration.milliseconds, Ack)
            groundControl expectMsg Incoming
            groundControl.lastSender should be(plane)
          }
        }
      }
  }

  `Given an actor system` {
    implicit system =>

      "Given a landed plane" - {

        "When the plane is requested to taxi" - {

          "Then it should informs airControl, runway, groundControl and taxiway of its movement" in {
            //Given
            val game = TestProbe()
            val airControl = TestProbe()
            val groundControl = TestProbe()
            val taxiway = TestProbe()
            val runway = TestProbe()
            val gate = TestProbe()
            system.actorOf(Props(classOf[Plane], airControl.ref, game.ref, settings), "plane")
            airControl expectMsg Incoming
            airControl reply Command.Land(runway.ref)
            airControl expectMsg(2 * settings.ackMaxDuration.milliseconds, Ack)
            airControl expectMsg(2 * settings.landingMaxDuration.milliseconds, HasLanded)
            runway expectMsg(2 * settings.landingMaxDuration.milliseconds, HasLanded)
            airControl reply Contact(groundControl.ref)
            airControl expectMsg(2 * settings.ackMaxDuration.milliseconds, Ack)
            groundControl expectMsg Incoming

            //When
            groundControl reply Command.TaxiAndPark(taxiway.ref, gate.ref)

            //Then
            groundControl expectMsg(2 * settings.ackMaxDuration.milliseconds, Ack)
            runway expectMsg HasLeft
            airControl expectMsg HasLeft
            taxiway expectMsg TaxiingToGate(gate.ref)
            groundControl expectMsg StartTaxi
          }
        }
      }
  }

  `Given an actor system` {
    implicit system =>

      "Given a taxiing plane" - {

        "When the plane exits from the taxiway" - {

          "Then it should informs the groundcontrol of its movement" in {
            //Given
            val game = TestProbe()
            val airControl = TestProbe()
            val groundControl = TestProbe()
            val taxiway = TestProbe()
            val gate = TestProbe()
            val plane = system.actorOf(Props(classOf[Plane], airControl.ref, game.ref, settings), "plane")
            airControl expectMsg Incoming
            airControl reply Command.Land(TestProbe().ref)
            airControl expectMsg(2 * settings.ackMaxDuration.milliseconds, Ack)
            airControl expectMsg(2 * settings.landingMaxDuration.milliseconds, HasLanded)
            airControl reply Contact(groundControl.ref)
            groundControl expectMsg Incoming
            airControl expectMsg(2 * settings.ackMaxDuration.milliseconds, Ack)
            groundControl reply Command.TaxiAndPark(taxiway.ref, gate.ref)
            groundControl expectMsg(2 * settings.ackMaxDuration.milliseconds, Ack)
            groundControl expectMsg StartTaxi

            //When
            taxiway.send(plane, HasParked)

            //Then
            groundControl expectMsg HasParked
          }
        }
      }
  }

  `Given an actor system` {
    implicit system =>

      "Given a parked plane" - {

        "When the plane has finished unloading passengers" - {

          "Then it should terminates and notify groundControl and gate, and scores the game" in {
            //Given
            val game = TestProbe()
            val airControl = TestProbe()
            val groundControl = TestProbe()
            val taxiway = TestProbe()
            val gate = TestProbe()
            val plane = system.actorOf(Props(classOf[Plane], airControl.ref, game.ref, settings), "plane")
            val probe = TestProbe()
            probe watch plane
            airControl expectMsg Incoming
            airControl reply Command.Land(TestProbe().ref)
            airControl expectMsg(2 * settings.ackMaxDuration.milliseconds, Ack)
            airControl expectMsg(2 * settings.landingMaxDuration.milliseconds, HasLanded)
            airControl reply Contact(groundControl.ref)
            airControl expectMsg(2 * settings.ackMaxDuration.milliseconds, Ack)
            groundControl expectMsg Incoming
            groundControl reply Command.TaxiAndPark(taxiway.ref, gate.ref)
            groundControl expectMsg(2 * settings.ackMaxDuration.milliseconds, Ack)
            groundControl expectMsg StartTaxi
            taxiway.send(plane, HasParked)
            groundControl expectMsg HasParked

            //Then
            probe expectTerminated(plane, 2 * settings.landingMaxDuration.milliseconds)
            gate expectMsg HasLeft
            groundControl expectMsg HasLeft
            game expectMsg Score(10)
          }
        }
      }
  }
}