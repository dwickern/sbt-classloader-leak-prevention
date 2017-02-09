package com.github.dwickern

import sbt.Logger
import se.jiderhamn.classloader.leak.prevention.{Logger => LeakLogger}

/** Redirect to the [[sbt.Logger]] */
class SbtLeakLogger(logger: Logger) extends LeakLogger {
  def warn(msg: String): Unit = logger.warn(msg)
  def warn(t: Throwable): Unit = logger.trace(t)
  def error(msg: String): Unit = logger.error(msg)
  def error(t: Throwable): Unit = logger.trace(t)
  def debug(msg: String): Unit = logger.debug(msg)
  def info(msg: String): Unit = logger.info(msg)
}
