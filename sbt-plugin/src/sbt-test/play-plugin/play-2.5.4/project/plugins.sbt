def pluginVersion = Option(sys.props("plugin.version"))
  .getOrElse(sys.error("Specify sbt-classloader-leak-prevention version with -Dplugin.version=???"))

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.4")
addSbtPlugin("com.github.dwickern" % "sbt-classloader-leak-prevention" % pluginVersion)
