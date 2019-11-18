package com.apollographql.apollo.api

/**
 * Represents a GraphQL fragment
 */
interface GraphqlFragment {

  /**
   * Returns marshaller to serialize fragment data
   *
   * @return [ResponseFieldMarshaller] to serialize fragment data
   */
  fun marshaller(): ResponseFieldMarshaller
}
