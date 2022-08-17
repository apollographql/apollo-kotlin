package com.apollographql.apollo3.java;

import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.api.Operation;

public interface ApolloCallback<D extends Operation.Data> {
  void onResponse(ApolloResponse<D> response);

  void onFailure(Throwable throwable);
}
