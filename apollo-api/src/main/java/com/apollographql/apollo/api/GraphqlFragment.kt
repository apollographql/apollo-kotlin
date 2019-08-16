package com.apollographql.apollo.api;

/**
 * Represents a GraphQL fragment
 */
public interface GraphqlFragment {

  /**
   * Returns marshaller to serialize fragment data
   *
   * @return {@link ResponseFieldMarshaller} to serialize fragment data
   */
  ResponseFieldMarshaller marshaller();
}
