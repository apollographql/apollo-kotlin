package com.apollographql.apollo.execution

/**
 * A [RootResolver] is called at the very beginning of the execution to seed execution.
 */
fun interface RootResolver {
  /**
   * @return the value for the root query/mutation/subscription object. May be null.
   */
  fun resolveRoot(): ResolverValue
}