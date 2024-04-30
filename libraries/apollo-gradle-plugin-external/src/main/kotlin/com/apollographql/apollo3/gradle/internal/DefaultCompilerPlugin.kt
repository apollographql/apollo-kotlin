package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.gradle.api.CompilerPlugin

class DefaultCompilerPlugin: CompilerPlugin {
  internal val arguments = mutableMapOf<String, Any>()
  override fun argument(name: String, value: Any) {
    arguments.put(name, value)
  }
}