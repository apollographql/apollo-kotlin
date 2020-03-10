package com.apollographql.apollo.api

import com.apollographql.apollo.api.internal.ResponseFieldMarshaller

/**
 * Represents a GraphQL fragment
 */
interface GraphqlFragment {

  /**
   * Returns marshaller [ResponseFieldMarshaller] to serialize fragment data
   */
  fun marshaller(): ResponseFieldMarshaller
}
