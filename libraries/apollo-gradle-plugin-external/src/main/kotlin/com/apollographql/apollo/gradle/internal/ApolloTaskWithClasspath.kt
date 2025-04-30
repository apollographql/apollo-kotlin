package com.apollographql.apollo.gradle.internal

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logging
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import java.util.function.Consumer
import javax.inject.Inject

@CacheableTask
abstract class ApolloTaskWithClasspath: DefaultTask() {
  @get:Classpath
  abstract val classpath: ConfigurableFileCollection

  @get:Input
  abstract val hasPlugin: Property<Boolean>

  @get:Input
  abstract val arguments: MapProperty<String, Any?>

  @get:Internal
  abstract val logLevel: Property<LogLevel>

  @Inject
  abstract fun getWorkerExecutor(): WorkerExecutor

  // This property provides access to the service instance
  @get:Internal
  abstract val apolloBuildService: Property<ApolloBuildService>

  @Internal
  fun getWorkQueue(): WorkQueue {
    return getWorkerExecutor().noIsolation()
  }

  class Options(
      val classpath: FileCollection,
      val hasPlugin: Boolean,
      val arguments: Map<String, Any?>,
      val logLevel: LogLevel,
  )
}

internal fun runInIsolation(buildService: ApolloBuildService, classpath: FileCollection,  block: (Any) -> Unit) {
  val clazz = buildService.classloader(classpath).loadClass("com.apollographql.apollo.compiler.EntryPoints")

  block(clazz.declaredConstructors.single().newInstance())
}

internal val warningMessageConsumer: Consumer<String> = object : Consumer<String> {
  private val logger = Logging.getLogger("apollo")

  override fun accept(p0: String) {
    logger.lifecycle(p0)
  }
}
