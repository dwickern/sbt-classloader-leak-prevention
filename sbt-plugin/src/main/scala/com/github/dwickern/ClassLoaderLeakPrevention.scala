package com.github.dwickern

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin
import se.jiderhamn.classloader.leak.prevention.cleanup.{ShutdownHookCleanUp, StopThreadsCleanUp}
import se.jiderhamn.classloader.leak.prevention.preinit.OracleJdbcThreadInitiator
import se.jiderhamn.classloader.leak.prevention.{ClassLoaderLeakPreventorFactory, Logger => LeakLogger}

import scala.concurrent.duration._

object ClassLoaderLeakPrevention extends AutoPlugin {
  object Keys {
    lazy val enableLeakPrevention = settingKey[Boolean]("Whether to use automatic ClassLoader leak prevention")
    lazy val enableLeakDetection = settingKey[Boolean]("Whether to check for a leaking ClassLoader")
    lazy val enableLeakDetectionHeapDump = settingKey[Boolean]("Whether to create a heap dump when a ClassLoader leak is detected")
    lazy val stopThreads = settingKey[Boolean]("Whether to force threads to stop")
    lazy val stopTimerThreads = settingKey[Boolean]("Whether to force timer threads to stop")
    lazy val executeShutdownHooks = settingKey[Boolean]("Whether to execute shutdown hooks")
    lazy val startOracleTimeoutThread = settingKey[Boolean]("Whether to force the Oracle JDBC timer thread to start")
    lazy val threadWait = settingKey[FiniteDuration]("How long to wait for threads to finish before stopping them")
    lazy val shutdownHookWait = settingKey[FiniteDuration]("How long to wait for shutdown hooks to finish before stopping them")
  }
  object InternalKeys {
    lazy val factory = taskKey[ClassLoaderLeakPreventorFactory]("Leak prevention configuration")
    lazy val logger = taskKey[LeakLogger]("Leak prevention logger")
    lazy val heapDumpDirectory = settingKey[File]("The directory to write the heap dump")
    lazy val config = taskKey[LeakPreventionConfig]("Internal ClassLoader leak config")
  }
  object autoImport {
    val ClassLoaderLeakPreventor = Keys
  }
  import Keys._
  import InternalKeys._

  override def trigger = allRequirements
  override def requires = JvmPlugin
  override lazy val projectSettings = Seq(
    enableLeakPrevention := true,
    enableLeakDetection := true,
    enableLeakDetectionHeapDump := false,
    stopThreads := true,
    stopTimerThreads := true,
    executeShutdownHooks := true,
    startOracleTimeoutThread := true,
    threadWait := 5.seconds,
    shutdownHookWait := 10.seconds,
    logger := new SbtLeakPreventionLogger(streams.value.log("leak-prevention")),
    factory := {
      val factory = new ClassLoaderLeakPreventorFactory()
      factory.setLogger(logger.value)

      if (!startOracleTimeoutThread.value) {
        factory.removePreInitiator(classOf[OracleJdbcThreadInitiator])
      }
      val shutdownHookCleanUp = factory.getCleanUp(classOf[ShutdownHookCleanUp])
      shutdownHookCleanUp.setExecuteShutdownHooks(executeShutdownHooks.value)
      shutdownHookCleanUp.setShutdownHookWaitMs(shutdownHookWait.value.toMillis.toInt)

      val stopThreadsCleanUp = factory.getCleanUp(classOf[StopThreadsCleanUp])
      stopThreadsCleanUp.setStopThreads(stopThreads.value)
      stopThreadsCleanUp.setStopTimerThreads(stopTimerThreads.value)
      stopThreadsCleanUp.setThreadWaitMs(threadWait.value.toMillis.toInt)

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

