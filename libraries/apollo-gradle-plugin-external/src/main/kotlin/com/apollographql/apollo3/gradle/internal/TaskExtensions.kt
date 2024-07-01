package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.compiler.ApolloCompiler
import org.gradle.api.logging.Logging

fun logger() = object : ApolloCompiler.Logger {
  val logger = Logging.getLogger("apollo")
  override fun warning(message: String) {
    logger.lifecycle(message)
  }
}
