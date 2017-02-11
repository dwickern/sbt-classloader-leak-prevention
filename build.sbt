sbtPlugin := true

name := "sbt-classloader-leak-prevention"

organization := "com.github.dwickern"

libraryDependencies ++= Seq(
  "se.jiderhamn.classloader-leak-prevention" % "classloader-leak-prevention-core" % "2.2.0",
  "se.jiderhamn" % "classloader-leak-test-framework" % "1.1.1"
)

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.4")

val play = project