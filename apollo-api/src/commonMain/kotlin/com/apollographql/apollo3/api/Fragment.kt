package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.internal.ResponseAdapter
import com.apollographql.apollo3.api.internal.json.JsonWriter

/**
 * Base interface for a fragment implementation.
 */
interface Fragment<D: Fragment.Data> {
  /**
   * Fragments do not have variable per the GraphQL spec but they are infered from arguments and used when reading the cache
   */
  fun serializeVariables(jsonWriter: JsonWriter, responseAdapterCache: ResponseAdapterCache)

  fun adapter(responseAdapterCache: ResponseAdapterCache): ResponseAdapter<D>

  fun responseFields(): List<ResponseField.FieldSet>

  /**
   * Marker interface for generated models of this fragment
   */
  interface Data
}