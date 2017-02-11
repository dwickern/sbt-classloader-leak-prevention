package com.github.dwickern

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin
import se.jiderhamn.classloader.leak.prevention.{ClassLoaderLeakPreventorFactory, Logger => LeakLogger}

object ClassLoaderLeakPrevention extends AutoPlugin {
  trait AutoImportKeys {
    lazy val enableLeakPrevention = settingKey[Boolean]("Whether to use automatic ClassLoader leak prevention")
    lazy val enableLeakDetection = settingKey[Boolean]("Whether to check for a leaking ClassLoader")
    lazy val enableLeakDetectionHeapDump = settingKey[Boolean]("Whether to create a heap dump when a ClassLoader leak is detected")
  }
  object Keys extends AutoImportKeys {
    lazy val factory = taskKey[ClassLoaderLeakPreventorFactory]("Leak prevention configuration")
    lazy val logger = taskKey[LeakLogger]("Leak prevention logger")
    lazy val heapDumpDirectory = settingKey[File]("The directory to write the heap dump")
    lazy val config = taskKey[LeakPreventionConfig]("Internal ClassLoader leak config")
  }
  object autoImport extends AutoImportKeys
  import Keys._

  override def trigger = allRequirements
  override def requires = JvmPlugin
  override lazy val projectSettings = Seq(
    enableLeakPrevention := true,
    enableLeakDetection := true,
    enableLeakDetectionHeapDump := false,
    logger := new SbtLeakPreventionLogger(streams.value.log("leak-prevention")),
    factory := {
      val factory = new ClassLoaderLeakPreventorFactory()
      factory.setLogger(logger.value)
      factory
    },
    config := LeakPreventionConfig(
      factory = factory.value,
      logger = logger.value,
      enablePrevention = enableLeakPrevention.value,
      enableDetection = enableLeakDetection.value,
      enableHeapDump = enableLeakDetectionHeapDump.value,
      heapDumpOutputDir = heapDumpDirectory.value
    ),
    heapDumpDirectory := target.value,
    (loadedTestFrameworks in Test) := {
      val c = config.value
      (loadedTestFrameworks in Test).value.map {
        case (tf, f) => tf -> new LeakPreventionTestFramework(f, c)
      }
    }
  )
}

