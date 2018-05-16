package com.apollographql.apollo;

import android.os.Handler;
import android.os.Looper;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;

import org.jetbrains.annotations.NotNull;

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
  final ApolloCall.Callback<T> delegate;
  private final Handler handler;

  /**
   * Wraps {@code callback} to be be operated on specified {@code handler}
   *
   * @param callback original callback to delegates calls
   * @param handler  the callback will be run on the thread to which this handler is attached
   */
  public static <T> ApolloCallback<T> wrap(@NotNull ApolloCall.Callback<T> callback, @NotNull Handler handler) {
    return new ApolloCallback<>(callback, handler);
  }

  /**
   * @param callback original callback to delegates calls
   * @param handler  the callback will be run on the thread to which this handler is attached
   */
  public ApolloCallback(@NotNull ApolloCall.Callback<T> callback, @NotNull Handler handler) {
    this.delegate = checkNotNull(callback, "callback == null");
    this.handler = checkNotNull(handler, "handler == null");
  }

  @Override public void onResponse(@NotNull final Response<T> response) {
    handler.post(new Runnable() {
      @Override public void run() {
        delegate.onResponse(response);
      }
    });
  }

  @Override public void onStatusEvent(@NotNull final ApolloCall.StatusEvent event) {
    handler.post(new Runnable() {
      @Override public void run() {
        delegate.onStatusEvent(event);
      }
    });
  }

  @Override public void onFailure(@NotNull final ApolloException e) {
    handler.post(new Runnable() {
      @Override public void run() {
        delegate.onFailure(e);
      }
    });
  }

  @Override public void onHttpError(@NotNull final ApolloHttpException e) {
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

  @Override public void onNetworkError(@NotNull final ApolloNetworkException e) {
    handler.post(new Runnable() {
      @Override public void run() {
        delegate.onNetworkError(e);
      }
    });
  }

  @Override public void onParseError(@NotNull final ApolloParseException e) {
    handler.post(new Runnable() {
      @Override public void run() {
        delegate.onParseError(e);
      }
    });
  }
}
