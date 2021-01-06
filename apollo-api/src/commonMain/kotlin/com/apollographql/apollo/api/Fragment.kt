package com.apollographql.apollo.api

import com.apollographql.apollo.api.internal.ResponseAdapter

interface Fragment<D: Fragment.Data> {
  /**
   * Marker interface for generated models of this fragment
   */
  interface Data

  fun variables(): Operation.Variables

  fun adapter(): ResponseAdapter<D>
}