package com.apollographql.apollo.api

/**
 * Base interface for a fragment implementation.
 * Fragments do not have variables per the GraphQL spec but they are inferred from arguments and used when reading the cache
 * See https://github.com/graphql/graphql-spec/issues/204 for a proposal to add fragment arguments
 */
interface Fragment<D : Fragment.Data> : Executable<D> {
  /**
   * Marker interface for generated models of this fragment
   */
  interface Data : Executable.Data
}


