lazy val root = (project in file(".")).
  settings(
    name := "quil-example",
    version := "1.0",
    scalaVersion := "2.11.5"
  )

mainClass in (Compile, run) := Some("QuillExample")

libraryDependencies += "io.getquill" %% "quill-cassandra" % "0.4.1" withSources()

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.4.6" withSources()

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test"
