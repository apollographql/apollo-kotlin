package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloCompiler
import org.gradle.api.Task
import org.gradle.workers.WorkAction

fun Task.logger() = object : ApolloCompiler.Logger {
  override fun warning(message: String) {
    logger.lifecycle(message)
  }
}

/**
 *
 */
fun WorkAction<*>.logger() = object : ApolloCompiler.Logger {
  override fun warning(message: String) {
    println(message)
  }
}