organization in ThisBuild := "com.github.dwickern"

val `play-plugin` = project
val `sbt-plugin` = project
val root = (project in file("."))
  .settings(publishArtifact := false)
  .aggregate(`sbt-plugin`, `play-plugin`)
