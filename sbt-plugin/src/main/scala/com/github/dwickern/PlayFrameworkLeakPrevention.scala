package com.github.dwickern

import play.runsupport.DelegatedResourcesClassLoader
import play.runsupport.Reloader.ClassLoaderCreator
import sbt.Keys._
import sbt._
import se.jiderhamn.classloader.leak.prevention.ClassLoaderLeakPreventor

object PlayFrameworkLeakPrevention extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = play.sbt.Play && ClassLoaderLeakPrevention
  override def projectSettings = Seq(
    play.sbt.PlayInternalKeys.playReloaderClassLoader := {
      val config = ClassLoaderLeakPrevention.InternalKeys.config.value
      new LeakPreventionCreator(config)
    },
    libraryDependencies ++= Seq(
      BuildInfo.playPluginOrganization %% BuildInfo.playPluginName % BuildInfo.playPluginVersion
    )
  )

  class LeakPreventionCreator(config: LeakPreventionConfig) extends ClassLoaderCreator {
    def apply(name: String, urls: Array[URL], parent: ClassLoader): ClassLoader =
      new LeakPreventionClassLoader(name, urls, parent, config)
  }

  class LeakPreventionClassLoader(name: String, urls: Array[URL], parent: ClassLoader, config: LeakPreventionConfig)
    extends DelegatedResourcesClassLoader(name, urls, parent) {

    private[this] val preventor: ClassLoaderLeakPreventor = config.factory.newLeakPreventor(this)

    if (config.enablePrevention) {
      preventor.runPreClassLoaderInitiators()
    }

    def leakPreventionCleanup(): Unit = {
      if (config.enablePrevention) {
        preventor.runCleanUps()
      }

      if (config.enableDetection) {
        LeakDetectionThread(this, config).start()
      }
    }
  }

}