lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  //.enablePlugins(PlayNettyServer).disablePlugins(PlayPekkoHttpServer) // uncomment to use the Netty backend
  .settings(
    name := """marlownavproject""",
    version := "1.0-SNAPSHOT",
    crossScalaVersions := Seq("2.13.15", "3.3.3"),
    scalaVersion := crossScalaVersions.value.head,
    libraryDependencies ++= Seq(
      guice,
      "com.h2database" % "h2" % "2.3.232",
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
    ),
    scalacOptions ++= Seq(
      "-feature",
      "-Werror"
    ),
    // Needed for ssl-config to create self signed certificated under Java 17
    Test / javaOptions ++= List("--add-exports=java.base/sun.security.x509=ALL-UNNAMED"),
  )

libraryDependencies ++= Seq(evolutions)


libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "42.2.20" // PostgreSQL driver
)

libraryDependencies ++= Seq(
  "org.playframework" %% "play-slick"            % "6.1.0",
  "org.playframework" %% "play-slick-evolutions" % "6.1.0"
)

libraryDependencies ++= Seq(ehcache, ws, specs2 % Test, guice)


val pekkoVersion = "1.0.0"



libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.5.0"


libraryDependencies += "com.typesafe.play" %% "play-ws" % "2.9.4" // Replace with the compatible version

//enablePlugins(ScalaPB)

//scalaVersion := "2.13.6" // Or your desired Scala version

libraryDependencies += "com.google.protobuf" % "protobuf-java" % "3.19.1"  // For Protobuf Java runtime

// Kafka dependencies
libraryDependencies += "org.apache.kafka" % "kafka-clients" % "3.6.0"
libraryDependencies += "com.typesafe.akka" %% "akka-stream-kafka" % "4.0.0"
