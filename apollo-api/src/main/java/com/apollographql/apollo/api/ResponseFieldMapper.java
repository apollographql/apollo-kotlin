package com.apollographql.apollo.api;

import java.io.IOException;

/**
 * * ResponseFieldMapper is responsible for mapping the responses returned by the server back to data.
 */
public interface ResponseFieldMapper<T> {
  T map(final ResponseReader responseReader) throws IOException;
}
