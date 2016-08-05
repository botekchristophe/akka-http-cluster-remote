name := "akka-http-cluster-remote"

version := "1.0.0"

scalaVersion := "2.11.8"

libraryDependencies ++= {
  val akkaV = "2.4.8"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaV,
    "com.typesafe.akka" %% "akka-http-core" % akkaV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaV
  )
}
