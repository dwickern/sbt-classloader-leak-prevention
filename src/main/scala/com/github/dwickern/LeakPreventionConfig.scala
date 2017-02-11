package com.github.dwickern

import java.io.File

import se.jiderhamn.classloader.leak.prevention.{ClassLoaderLeakPreventorFactory, Logger}

case class LeakPreventionConfig(
    factory: ClassLoaderLeakPreventorFactory,
    logger: Logger,
    enablePrevention: Boolean,
    enableDetection: Boolean,
    enableHeapDump: Boolean,
    heapDumpOutputDir: File)
