package com.github.dwickern

import java.io.File
import java.net.URLClassLoader

import sbt.testing.{Framework, Runner, TaskDef}
import se.jiderhamn.HeapDumper
import se.jiderhamn.classloader.leak.prevention.{ClassLoaderLeakPreventor, ClassLoaderLeakPreventorFactory, Logger}

import scala.annotation.tailrec
import scala.ref.WeakReference

case class LeakConfig(
    factory: ClassLoaderLeakPreventorFactory,
    logger: Logger,
    enableDetection: Boolean,
    enableHeapDump: Boolean,
    heapDumpOutputDir: File)

class LeakFramework(framework: Framework, config: LeakConfig) extends Framework {
  def name = framework.name
  def fingerprints = framework.fingerprints
  def runner(args: Array[String], remoteArgs: Array[String], testClassLoader: ClassLoader): Runner = {
    val runner = framework.runner(args, remoteArgs, testClassLoader)
    new LeakRunner(runner, findURLClassLoader(testClassLoader), config)
  }

  /**
    * We need the classloader which is actually doing the class loading.
    *
    * Skip over the [[sbt.classpath.ClasspathFilter]] to find the classloader
    * which was created by [[sbt.classpath.ClasspathUtilities#toLoader]].
    */
  @tailrec private final def findURLClassLoader(cl: ClassLoader): URLClassLoader = cl match {
    case url: URLClassLoader => url
    case _ => findURLClassLoader(cl.getParent)
  }
}

private class LeakRunner(runner: Runner, testClassLoader: ClassLoader, config: LeakConfig) extends Runner {
  private[this] val preventor = config.factory.newLeakPreventor(testClassLoader)

  preventor.runPreClassLoaderInitiators()

  def tasks(taskDefs: Array[TaskDef]) = runner.tasks(taskDefs)
  def args = runner.args
  def remoteArgs = runner.remoteArgs
  def done(): String = {
    val summary = runner.done()

    preventor.runCleanUps()

    if (config.enableDetection) {
      // start leak detection on another thread
      // because the current thread's stack still references the ClassLoader
      LeakDetectionThread(testClassLoader, config).start()
    }

    summary
  }
}

private class LeakDetectionThread private (classLoader: WeakReference[ClassLoader], config: LeakConfig)
    extends Thread("ClassLoader Leak Detection") {

  setContextClassLoader(null)

  override def run(): Unit = {
    for (_ <- 1 to 3 if classLoader.get.isDefined) {
      Thread.sleep(1000)
      ClassLoaderLeakPreventor.gc()
    }

    if (classLoader.get.isDefined && config.enableHeapDump) {
      classLoader.clear() // don't include this reference in the heap dump

      val out = new File(config.heapDumpOutputDir, s"heapdump-${System.currentTimeMillis()}.hprof")
      config.logger.error("ClassLoader leak detected! Writing heap dump to: " + out.getAbsolutePath)
      HeapDumper.dumpHeap(out, false)
    } else if (classLoader.get.isDefined) {
      config.logger.error("ClassLoader leak detected! To generate a heap dump, `set enableLeakDetectionHeapDump := true`")
    } else {
      config.logger.debug("No ClassLoader leak was detected.")
    }
  }
}

private object LeakDetectionThread {
  def apply(classLoader: ClassLoader, config: LeakConfig): LeakDetectionThread =
    new LeakDetectionThread(WeakReference(classLoader), config)
}