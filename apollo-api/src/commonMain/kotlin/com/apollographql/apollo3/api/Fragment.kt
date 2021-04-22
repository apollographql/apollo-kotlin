package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.JsonWriter

/**
 * Base interface for a fragment implementation.
 * Fragments do not have variables per the GraphQL spec but they are inferred from arguments and used when reading the cache
 * See https://github.com/graphql/graphql-spec/issues/204
 */
interface Fragment<D: Fragment.Data> {
  fun serializeVariables(writer: JsonWriter, responseAdapterCache: ResponseAdapterCache)

  fun adapter(): ResponseAdapter<D>

  fun responseFields(): List<ResponseField.FieldSet>

  /**
   * Marker interface for generated models of this fragment
   */
  interface Data
}

