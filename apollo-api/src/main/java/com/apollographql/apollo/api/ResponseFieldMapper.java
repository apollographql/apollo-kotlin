package com.apollographql.apollo.api;

import java.io.IOException;

/** Converts the response back to the data */
public interface ResponseFieldMapper<T> {
  T map(final ResponseReader responseReader) throws IOException;
}
