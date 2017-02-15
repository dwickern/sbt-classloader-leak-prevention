organization in ThisBuild := "com.github.dwickern"

val `play-plugin` = project
val `sbt-plugin` = project
val root = (project in file("."))
  .settings(publishArtifact := false)
  .aggregate(`sbt-plugin`, `play-plugin`)

pomExtra in Global := {
  <url>https://github.com/dwickern/sbt-classloader-leak-prevention</url>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
    </license>
  </licenses>
  <scm>
    <connection>scm:git:github.com/dwickern/sbt-classloader-leak-prevention.git</connection>
    <developerConnection>scm:git:git@github.com:dwickern/sbt-classloader-leak-prevention.git</developerConnection>
    <url>github.com/dwickern/sbt-classloader-leak-prevention.git</url>
  </scm>
  <developers>
    <developer>
      <id>dwickern</id>
      <name>Derek Wickern</name>
      <url>https://github.com/dwickern</url>
    </developer>
  </developers>
}

releaseProcess := {
  import ReleaseTransformations._
  Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    ReleaseStep(action = Command.process("publishSigned", _), enableCrossBuild = true),
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _), enableCrossBuild = true),
    pushChanges
  )
}
