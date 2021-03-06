version := "1.0.0"

organization := "fr.xebia.xke.akka.airport"

name := "client"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.0" % "test",
  "com.typesafe.akka" %% "akka-actor" % "2.3.1",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.1",
  "ch.qos.logback" % "logback-classic" % "1.0.13",
  "ch.qos.logback" % "logback-core" % "1.0.13",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.1" % "test",
  "com.typesafe.akka" %% "akka-cluster" % "2.3.1",
  "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.1"
)

incOptions := incOptions.value.withNameHashing(true)

fork in run := true

connectInput in run := true

seedNode := "akka.tcp://airportSystem@127.0.0.1:2554"

airport   := ""

check := {
  println("seedNode: " + seedNode.value )
  println("airport : " + airport.value )
}

javaOptions in run := Seq("-Dakka.cluster.seed-nodes.0=" + seedNode.value,
                          "-Dakka.cluster.roles.0=" + airport.value,
                          "-Dakka.persistence.snapshot-store.local.dir=target/"+airport.value+"/snapshots",
                          "-Dakka.persistence.journal.leveldb.dir=target/"+airport.value+"/journal"
)

mainClass in (Compile,run) := Some("Launcher")
