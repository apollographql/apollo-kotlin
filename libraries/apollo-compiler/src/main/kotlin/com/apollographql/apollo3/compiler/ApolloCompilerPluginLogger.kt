package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.annotations.ApolloExperimental

@ApolloExperimental
interface ApolloCompilerPluginLogger {
  fun logging(message: String)
  fun info(message: String)
  fun warn(message: String)
  fun error(message: String)

  fun exception(e: Throwable)
}
