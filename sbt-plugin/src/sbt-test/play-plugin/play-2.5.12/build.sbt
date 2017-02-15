scalaVersion := "2.11.8"

enablePlugins(PlayScala)

libraryDependencies += filters
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test

// fix conflict with sbt scripted "test" file
scalaSource in Test := baseDirectory.value / "tests"
