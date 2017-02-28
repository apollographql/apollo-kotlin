package com.apollographql.android.impl;

import com.apollographql.android.ApolloPrefetchCall;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.cache.HttpCache;
import com.squareup.moshi.Moshi;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import okhttp3.Call;
import okhttp3.HttpUrl;

final class RealApolloPrefetchCall extends BaseApolloCall implements ApolloPrefetchCall {
  volatile Call httpCall;
  private boolean executed;

  RealApolloPrefetchCall(Operation operation, HttpUrl serverUrl, Call.Factory httpCallFactory, Moshi moshi) {
    super(operation, serverUrl, httpCallFactory, moshi);
  }

  @Override public void execute() throws IOException {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }
    httpCall = prepareHttpCall(HttpCache.CacheControl.DEFAULT, true);
    httpCall.execute().code();
  }

  @Nonnull @Override public ApolloPrefetchCall enqueue(@Nullable final Callback callback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }

    try {
      httpCall = prepareHttpCall(HttpCache.CacheControl.DEFAULT, true);
    } catch (Exception e) {
      if (callback != null) {
        callback.onFailure(e);
      }
      return this;
    }

    httpCall.enqueue(new okhttp3.Callback() {
      @Override public void onFailure(Call call, IOException e) {
        if (callback != null) {
          callback.onFailure(e);
        }
      }

      @Override public void onResponse(Call call, okhttp3.Response response) throws IOException {
        response.close();
        if (callback != null) {
          callback.onSuccess();
        }
      }
    });
    return this;
  }

  @Override public ApolloPrefetchCall clone() {
    return new RealApolloPrefetchCall(operation, serverUrl, httpCallFactory, moshi);
  }

  @Override public void cancel() {
    Call call = httpCall;
    if (call != null) {
      call.cancel();
    }
  }
}
