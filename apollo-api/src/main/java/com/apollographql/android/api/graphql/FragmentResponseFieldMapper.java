package com.apollographql.android.api.graphql;

import java.io.IOException;

public interface FragmentResponseFieldMapper<T> {
  T map(final ResponseReader responseReader, String conditionalType) throws IOException;
}
