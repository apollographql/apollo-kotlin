package com.apollographql.apollo.api;

import java.io.IOException;

/**
 * ResponseFieldMapper is an abstraction for mapping the response data returned by
 * the server back to generated models.
 */
public interface ResponseFieldMapper<T> {
  T map(final ResponseReader responseReader) throws IOException;
}
