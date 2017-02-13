package com.github.dwickern

import java.net.URLClassLoader

import sbt.testing.{Framework, Runner, TaskDef}

import scala.annotation.tailrec

class LeakPreventionTestFramework(framework: Framework, config: LeakPreventionConfig) extends Framework {
  def name = framework.name
  def fingerprints = framework.fingerprints
  def runner(args: Array[String], remoteArgs: Array[String], testClassLoader: ClassLoader): Runner = {
    val runner = framework.runner(args, remoteArgs, testClassLoader)
    new LeakPreventionTestRunner(runner, findURLClassLoader(testClassLoader), config)
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

private class LeakPreventionTestRunner(runner: Runner, testClassLoader: ClassLoader, config: LeakPreventionConfig) extends Runner {
  private[this] val preventor = config.factory.newLeakPreventor(testClassLoader)

  if (config.enablePrevention) {
    preventor.runPreClassLoaderInitiators()
  }

  def tasks(taskDefs: Array[TaskDef]) = runner.tasks(taskDefs)
  def args = runner.args
  def remoteArgs = runner.remoteArgs
  def done(): String = {
    val summary = runner.done()

    if (config.enablePrevention) {
      preventor.runCleanUps()
    }

    if (config.enableDetection) {
      LeakDetectionThread(testClassLoader, config).start()
    }

    summary
  }
}
