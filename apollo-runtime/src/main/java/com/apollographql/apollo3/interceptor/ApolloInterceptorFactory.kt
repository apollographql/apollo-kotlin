package com.apollographql.apollo3.interceptor

import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloLogger

interface ApolloInterceptorFactory {
  /**
   * creates a new interceptor for the given operation
   *
   * @param logger: a logger to output debug information
   * @param operation: the operation
   *
   * @return the interceptor or null if no interceptor is needed for this operation
   */
  fun newInterceptor(logger: ApolloLogger, operation: Operation<*>): ApolloInterceptor?
}
