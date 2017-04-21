package com.apollographql.apollo.api;

import java.io.IOException;

/**
 * FragmentResponseFieldMapper is responsible for mapping the response back to a fragment of type T.
 */
public interface FragmentResponseFieldMapper<T> {
  T map(final ResponseReader responseReader, String conditionalType) throws IOException;
}
