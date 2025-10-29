package com.apollographql.apollo.network

import com.apollographql.apollo.annotations.ApolloExperimental

/**
 * The protocol to use for incremental delivery (`@defer` and `@stream`).
 */
@ApolloExperimental
enum class IncrementalDeliveryProtocol {
  /**
   * Draft v0.1 format as specified by https://specs.apollo.dev/incremental/v0.1/, and in this historical commit:
   * https://github.com/graphql/graphql-spec/tree/48cf7263a71a683fab03d45d309fd42d8d9a6659/spec, and implemented by `graphql.js`
   * version `17.0.0-alpha.2`, also referred to as `20220824`.
   *
   * Only `@defer` is supported with this format.
   */
  V0_1,

  /**
   * Draft v0.2 format as specified by https://specs.apollo.dev/incremental/v0.2/, and implemented by `graphql.js` version `17.0.0-alpha.9`.
   *
   * Both `@defer` and `@stream` are supported with this format.
   */
  V0_2,
}
