package com.apollographql.apollo.network

import com.apollographql.apollo.annotations.ApolloExperimental

/**
 * The protocol to use for incremental delivery (`@defer` and `@stream`).
 */
@ApolloExperimental
enum class IncrementalDeliveryProtocol {

  /**
   * Newer format as implemented by graphql.js version `17.0.0-alpha.2` and specified in this historical commit:
   * https://github.com/graphql/graphql-spec/tree/48cf7263a71a683fab03d45d309fd42d8d9a6659/spec
   *
   * Only `@defer` is supported with this format.
   *
   * This is the default.
   */
  GraphQL17Alpha2,

  /**
   * Newer format as implemented by graphql.js version `17.0.0-alpha.9`.
   *
   * Both `@defer` and `@stream` are supported with this format.
   */
  GraphQL17Alpha9
}
