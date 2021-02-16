package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.internal.ResponseAdapter

interface Fragment<D: Fragment.Data> {
  /**
   * Marker interface for generated models of this fragment
   */
  interface Data

  fun variables(): Operation.Variables

  fun adapter(responseAdapterCache: ResponseAdapterCache): ResponseAdapter<D>

  fun responseFields(): List<ResponseField.FieldSet>
}