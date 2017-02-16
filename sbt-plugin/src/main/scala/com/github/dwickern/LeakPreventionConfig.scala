package com.github.dwickern

import java.io.File

import se.jiderhamn.classloader.leak.prevention.{ClassLoaderLeakPreventorFactory, Logger}

import scala.concurrent.duration.FiniteDuration

case class LeakPreventionConfig(
    factory: ClassLoaderLeakPreventorFactory,
    logger: Logger,
    enablePrevention: Boolean,
    enableDetection: Boolean,
    detectionAttempts: Int,
    detectionInterval: FiniteDuration,
    enableHeapDump: Boolean,
    heapDumpOutputDir: File)
