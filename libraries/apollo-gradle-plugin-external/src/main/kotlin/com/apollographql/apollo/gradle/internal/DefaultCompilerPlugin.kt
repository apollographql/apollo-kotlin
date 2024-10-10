package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.CompilerPlugin

class DefaultCompilerPlugin: CompilerPlugin {
  internal val arguments = mutableMapOf<String, Any?>()
  override fun argument(name: String, value: Any?) {
    arguments.put(name, value)
  }
}