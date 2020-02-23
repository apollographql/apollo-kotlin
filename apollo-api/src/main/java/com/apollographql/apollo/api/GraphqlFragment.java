package com.apollographql.apollo.api;

import com.apollographql.apollo.api.internal.ResponseFieldMarshaller;

/**
 * Represents a GraphQL fragment
 */
public interface GraphqlFragment {

  /**
   * Returns marshaller to serialize fragment data
   *
   * @return {@link com.apollographql.apollo.api.internal.ResponseFieldMarshaller} to serialize fragment data
   */
  ResponseFieldMarshaller marshaller();
}
