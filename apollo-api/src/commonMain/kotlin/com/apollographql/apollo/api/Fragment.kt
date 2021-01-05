package com.apollographql.apollo.api

interface Fragment<D: Fragment.Data> {
  /**
   * Marker interface for generated models of this fragment
   */
  interface Data
}