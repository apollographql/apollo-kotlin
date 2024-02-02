package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.Plugin
import java.util.ServiceLoader

internal fun apolloCompilerPlugin(): Plugin? {
  val plugins = ServiceLoader.load(Plugin::class.java).toList()

  if (plugins.size > 1) {
    error("Apollo: only a single compiler plugin is allowed")
  }

  return plugins.singleOrNull()
}
