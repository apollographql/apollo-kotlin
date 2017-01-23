package com.apollographql.api.graphql;

import java.io.IOException;

public interface ResponseFieldMapper<T> {
  void map(final ResponseReader responseReader, final T instance) throws IOException;
}
