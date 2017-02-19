package com.apollographql.android;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Response;

import java.io.IOException;

import javax.annotation.Nullable;

public interface OperationRequest<T extends Operation.Data> {

  void cancel();

  Response<T> execute() throws IOException;

  OperationRequest<T> enqueue(@Nullable Callback<T> callback);

  public interface Callback<T extends Operation.Data> {
    void onResponse(Response<T> response);

    void onFailure(Exception e);
  }
}
