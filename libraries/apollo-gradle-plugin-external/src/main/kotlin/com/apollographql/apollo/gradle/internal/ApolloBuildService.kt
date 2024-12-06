package com.apollographql.apollo.gradle.internal

import org.gradle.api.file.FileCollection
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.net.URL
import java.net.URLClassLoader

abstract class ApolloBuildService: BuildService<BuildServiceParameters.None>, AutoCloseable {
  private val classloaders = mutableMapOf<Array<URL>, ClassLoader>()

  fun classloader(classpath: FileCollection): ClassLoader {
    val urls = classpath.map { it.toURI().toURL() }.toTypedArray()
    return classloaders.getOrPut(urls) {
      URLClassLoader(
          urls,
          ClassLoader.getPlatformClassLoader()
      )
    }
  }

  override fun close() {
    classloaders.clear()
  }
}