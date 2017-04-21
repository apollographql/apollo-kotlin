package com.apollographql.apollo.api;

import java.io.IOException;

/**
 * Maps the response back to the data for a fragment
 * @param <T>
 */
public interface FragmentResponseFieldMapper<T> {
  T map(final ResponseReader responseReader, String conditionalType) throws IOException;
}
