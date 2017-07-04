package com.apollographql.apollo.api;

/**
 * ResponseFieldMapper is an abstraction for mapping the response data returned by
 * the server back to generated models.
 */
public interface ResponseFieldMapper<T> {
  T map(final ResponseReader responseReader);
}
