package com.apollographql.apollo;

import android.os.Handler;
import android.os.Looper;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;

import javax.annotation.Nonnull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * <p>Android wrapper for {@link ApolloCall.Callback} to be operated on specified {@link Handler}</p>
 *
 * <b>NOTE:</b> {@link #onHttpError(ApolloHttpException)} will be called on the background thread if provided handler is
 * attached to the main looper. This behaviour is intentional as {@link ApolloHttpException} internally has a reference
 * to raw {@link okhttp3.Response} that must be closed on the background, otherwise it throws {@link
 * android.os.NetworkOnMainThreadException} exception.
 */
public final class ApolloCallback<T> extends ApolloCall.Callback<T> {
  private final ApolloCall.Callback<T> delegate;
  private final Handler handler;

  /**
   * Wraps {@code callback} to be be operated on specified {@code handler}
   *
   * @param callback original callback to delegates calls
   * @param handler  the callback will be run on the thread to which this handler is attached
   */
  public static <T> ApolloCallback<T> wrap(@Nonnull ApolloCall.Callback<T> callback, @Nonnull Handler handler) {
    return new ApolloCallback<>(callback, handler);
  }

  /**
   * @param callback original callback to delegates calls
   * @param handler  the callback will be run on the thread to which this handler is attached
   */
  public ApolloCallback(@Nonnull ApolloCall.Callback<T> callback, @Nonnull Handler handler) {
    this.delegate = checkNotNull(callback, "callback == null");
    this.handler = checkNotNull(handler, "handler == null");
  }

  @Override public void onResponse(@Nonnull final Response<T> response) {
    handler.post(new Runnable() {
      @Override public void run() {
        delegate.onResponse(response);
      }
    });
  }

  @Override public void onCompleted() {
    handler.post(new Runnable() {
      @Override public void run() {
        delegate.onCompleted();
      }
    });
  }

  @Override public void onFailure(@Nonnull final ApolloException e) {
    handler.post(new Runnable() {
      @Override public void run() {
        delegate.onFailure(e);
      }
    });
  }

  @Override public void onHttpError(@Nonnull final ApolloHttpException e) {
    if (Looper.getMainLooper() == handler.getLooper()) {
      delegate.onHttpError(e);
    } else {
      handler.post(new Runnable() {
        @Override public void run() {
          delegate.onHttpError(e);
        }
      });
    }
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
