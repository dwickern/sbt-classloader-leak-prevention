package com.github.dwickern

import sbt._
import Keys._
import sbt.plugins.JvmPlugin
import se.jiderhamn.classloader.leak.prevention.ClassLoaderLeakPreventorFactory
import se.jiderhamn.classloader.leak.prevention.{Logger => LeakLogger}

object ClassLoaderLeakPrevention extends AutoPlugin {
  trait AutoImportKeys {
    lazy val enableLeakDetection = settingKey[Boolean]("Whether to check for a leaking ClassLoader")
    lazy val enableLeakDetectionHeapDump = settingKey[Boolean]("Whether to create a heap dump when a ClassLoader leak is detected")
  }
  object Keys extends AutoImportKeys {
    lazy val factory = taskKey[ClassLoaderLeakPreventorFactory]("Leak prevention configuration")
    lazy val logger = taskKey[LeakLogger]("Leak prevention logger")
    lazy val heapDumpDirectory = settingKey[File]("The directory to write the heap dump")
  }
  object autoImport extends AutoImportKeys
  import Keys._

  override def trigger = allRequirements
  override def requires = JvmPlugin
  override lazy val projectSettings = Seq(
    enableLeakDetection := true,
    enableLeakDetectionHeapDump := false,
    logger := new SbtLeakLogger(streams.value.log("leak-prevention")),
    factory := {
      val factory = new ClassLoaderLeakPreventorFactory()
      factory.setLogger(logger.value)
      factory
    },
    heapDumpDirectory := target.value,
    (loadedTestFrameworks in Test) := {
      val config = LeakConfig(
        factory = factory.value,
        logger = logger.value,
        enableDetection = enableLeakDetection.value,
        enableHeapDump = enableLeakDetectionHeapDump.value,
        heapDumpOutputDir = heapDumpDirectory.value
      )
      (loadedTestFrameworks in Test).value.map {
        case (tf, f) => tf -> new LeakFramework(f, config)
      }
    }
  )
}

