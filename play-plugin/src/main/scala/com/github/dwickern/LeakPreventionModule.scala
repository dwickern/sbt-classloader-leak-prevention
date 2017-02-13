package com.github.dwickern

import javax.inject.{Inject, Singleton}

import play.api.inject._
import play.api.{Configuration, Environment, Mode}

import scala.concurrent.Future

class LeakPreventionModule extends Module {
  def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    if (environment.mode == Mode.Prod) Seq.empty
    else Seq(bind[LeakPreventionHook].toSelf.eagerly)
  }
}

/**
  * Tells the sbt plugin when the application was stopped.
  *
  * HACK: because there is no easy way to hook into the application lifecycle
  */
@Singleton class LeakPreventionHook @Inject()(lifecycle: DefaultApplicationLifecycle, environment: Environment) {
  addFinalStopHook { () =>
    val cleanupField = {
      val field = environment.classLoader.getClass.getDeclaredMethod("leakPreventionCleanup")
      field.setAccessible(true)
      field
    }

    // run the classloader cleanup
    cleanupField.invoke(environment.classLoader)
    Future.successful(())
  }

  /**
    * HACK: Appends the hook instead of prepending it, so it runs after all other hooks
    */
  def addFinalStopHook(hook: () => Future[_]): Unit = {
    val mutexField = {
      val field = classOf[DefaultApplicationLifecycle].getDeclaredField("mutex")
      field.setAccessible(true)
      field
    }
    val hooksField = {
      val field = classOf[DefaultApplicationLifecycle].getDeclaredField("hooks")
      field.setAccessible(true)
      field
    }

    val mutex = mutexField.get(lifecycle)
    mutex.synchronized {
      val old = hooksField.get(lifecycle).asInstanceOf[List[() => Future[_]]]
      hooksField.set(lifecycle, old :+ hook)
    }
  }
}