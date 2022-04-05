package com.apollographql.apollo3.exception

import com.apollographql.apollo3.annotations.ApolloExperimental

private val DEFAULT_EXCEPTION_HANDLER: (Throwable) -> Unit = {
  println("Apollo: unhandled exception")
  it.printStackTrace()
}

/**
 * A handler that can be set to intercept certain exceptions occurring in the library that are not normally surfaced. This can be used
 * to investigate issues. The default is to log the exception.
 */
@ApolloExperimental
var apolloExceptionHandler: (Throwable) -> Unit = DEFAULT_EXCEPTION_HANDLER
