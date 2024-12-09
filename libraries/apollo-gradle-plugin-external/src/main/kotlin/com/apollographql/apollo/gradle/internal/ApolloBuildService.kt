package com.apollographql.apollo.gradle.internal

import org.gradle.api.file.FileCollection
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.net.URI
import java.net.URL
import java.net.URLClassLoader

abstract class ApolloBuildService: BuildService<BuildServiceParameters.None>, AutoCloseable {
  private val classloaders = mutableMapOf<List<URI>, ClassLoader>()

  @Synchronized
  fun classloader(classpath: FileCollection): ClassLoader {
    val urls = classpath.map { it.toURI() }
    return classloaders.getOrPut(urls) {
      URLClassLoader(
          urls.map { it.toURL() }.toTypedArray(),
          ClassLoader.getPlatformClassLoader()
      )
    }
  }

  override fun close() {
    classloaders.clear()
  }
}