package com.apollographql.android.api.graphql;

import java.io.IOException;

/** TODO */
public interface ResponseFieldMapper<T> {
  void map(final ResponseReader responseReader, final T instance) throws IOException;
}
