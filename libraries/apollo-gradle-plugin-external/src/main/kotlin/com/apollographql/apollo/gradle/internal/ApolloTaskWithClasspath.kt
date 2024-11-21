package com.apollographql.apollo.gradle.internal

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

abstract class ApolloTaskWithClasspath: DefaultTask() {
  @get:Classpath
  abstract val classpath: ConfigurableFileCollection

  @get:Input
  abstract val hasPlugin: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val useProcessIsolation: Property<Boolean>

  @get:Input
  abstract val arguments: MapProperty<String, Any?>

  @get:Input
  abstract val logLevel: Property<LogLevel>

  @Inject
  abstract fun getWorkerExecutor(): WorkerExecutor

  @Internal
  fun getWorkQueue(): WorkQueue {
    return if (useProcessIsolation.orElse(false).get()) {
      getWorkerExecutor().processIsolation { workerSpec ->
        workerSpec.classpath.from(classpath)
      }
    } else {
      getWorkerExecutor().classLoaderIsolation { workerSpec ->
        workerSpec.classpath.from(classpath)
      }
    }
  }

  class Options(
      val classpath: FileCollection,
      val hasPlugin: Boolean,
      val arguments: Map<String, Any?>,
      val logLevel: LogLevel,
      val useProcessIsolation: Property<Boolean>
  )
}