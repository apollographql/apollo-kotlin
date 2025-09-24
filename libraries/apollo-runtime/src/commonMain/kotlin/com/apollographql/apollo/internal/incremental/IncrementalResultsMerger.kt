package com.apollographql.apollo.internal.incremental

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.IncrementalResultIdentifiers
import okio.BufferedSource

/**
 * Utility for merging GraphQL incremental results received in multiple chunks when using the `@defer` and/or `@stream` directives.
 *
 * Each call to [merge] will merge the given results into the [merged] Map, and will also update [incrementalResultIdentifiers] with the
 * value of their `path` and `label` fields.
 *
 * The fields in `data` are merged into the node found in [merged] at the path known by looking at the `id` field. For the first call to
 * [merge], the payload is copied to [merged] as-is.
 *
 * `errors` in incremental and completed results (if present) are merged together in an array and then set to the `errors` field of the
 * [merged] Map.
 * `extensions` in incremental results (if present) are merged together in an array and then set to the `extensions` field of the [merged]
 * Map.
 */
@ApolloInternal
interface IncrementalResultsMerger {
  val merged: JsonMap

  val incrementalResultIdentifiers: IncrementalResultIdentifiers

  val hasNext: Boolean

  /**
   * A response can sometimes have no `incremental` field, e.g. when the server couldn't predict if there were more data after the last
   * emitted payload. This field allows to test for this in order to ignore such payloads.
   * See https://github.com/apollographql/router/issues/1687.
   */
  val isEmptyResponse: Boolean

  fun merge(part: BufferedSource): JsonMap

  fun merge(part: JsonMap): JsonMap

  fun reset()
}
