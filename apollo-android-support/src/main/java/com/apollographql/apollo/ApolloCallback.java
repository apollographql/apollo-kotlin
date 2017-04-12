package com.apollographql.apollo;

import android.os.Handler;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;

import javax.annotation.Nonnull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * Android wrapper for {@link ApolloCall.Callback} to be operated on specified {@link Handler}
 */
public final class ApolloCallback<T> extends ApolloCall.Callback<T> {
  private final ApolloCall.Callback<T> delegate;
  private final Handler handler;

  /**
   * @param callback original callback to delegates calls
   * @param handler  the callback will be run on the thread to which this handler is attached
   */
  public ApolloCallback(@Nonnull ApolloCall.Callback<T> callback, @Nonnull Handler handler) {
    this.delegate = checkNotNull(callback, "callback == null");
    this.handler = checkNotNull(handler, "handler == null");
  }

  @Override public final void onResponse(@Nonnull final Response<T> response) {
    handler.post(new Runnable() {
      @Override public void run() {
        delegate.onResponse(response);
      }
    });
  }

  @Override public final void onFailure(@Nonnull final ApolloException e) {
    handler.post(new Runnable() {
      @Override public void run() {
        delegate.onFailure(e);
      }
    });
  }

  @Override public void onHttpError(@Nonnull final ApolloHttpException e) {
    handler.post(new Runnable() {
      @Override public void run() {
        delegate.onHttpError(e);
      }
    });
  }

  @Override public void onNetworkError(@Nonnull final ApolloNetworkException e) {
    handler.post(new Runnable() {
      @Override public void run() {
        delegate.onNetworkError(e);
      }
    });
  }

  @Override public void onParseError(@Nonnull final ApolloParseException e) {
    handler.post(new Runnable() {
      @Override public void run() {
        delegate.onParseError(e);
      }
    });
  }
}
