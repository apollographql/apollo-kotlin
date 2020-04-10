package com.apollographql.apollo.api

import okio.ByteString

/**
 * Represents a GraphQL query that will be sent to the server.
 */
interface Query<D : Operation.Data, T, V : Operation.Variables> : Operation<D, T, V> {
  /**
   * Composes POST JSON-encoded request body to be sent to the GraphQL server.
   *
   * In case when [autoPersistQueries] is set to `true` special `extension` attributes, required by query auto persistence,
   * will be encoded along with regular GraphQL request body. If query was previously persisted on the GraphQL server
   * set [withQueryDocument] to `false` to skip query document be sent in the request.
   *
   * Optional [scalarTypeAdapters] must be provided in case when this operation defines variables with custom GraphQL scalar type.
   *
   * *Example*:
   * ```
   * {
   *    "query": "query TestQuery($episode: Episode) { hero(episode: $episode) { name } }",
   *    "operationName": "TestQuery",
   *    "variables": { "episode": "JEDI" }
   *    "extensions": {
   *      "persistedQuery": {
   *        "version": 1,
   *        "sha256Hash": "32637895609e6c51a2593f5cfb49244fd79358d327ff670b3e930e024c3db8f6"
   *      }
   *    }
   * }
   * ```
   */
  fun composeRequestBody(
      autoPersistQueries: Boolean,
      withQueryDocument: Boolean,
      scalarTypeAdapters: ScalarTypeAdapters
  ): ByteString
}
