package com.apollographql.android.rx2;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloPrefetch;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import javax.annotation.Nonnull;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Cancellable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * The RxApollo class provides methods for converting ApolloCall, ApolloPrefetch
 * and ApolloWatcher types to RxJava 2 sources.
 */
public class Rx2Apollo {

  private Rx2Apollo() {
    throw new AssertionError("This class cannot be instantiated");
  }


  /**
   * Converts an {@link ApolloCall} to a Single.
   *
   * @param originalCall the ApolloCall to convert
   * @param <T>          the value type.
   * @return the converted Single
   * @throws NullPointerException if originalCall == null
   */
  @Nonnull public static <T> Single<T> from(@Nonnull final ApolloCall<T> originalCall) {
    checkNotNull(originalCall, "call == null");

    return Single.create(new SingleOnSubscribe<T>() {
      @Override public void subscribe(SingleEmitter<T> emitter) {
        cancelSingleOnDisposed(emitter, originalCall);
        try {
          Response<T> response = originalCall.execute();
          if (!emitter.isDisposed()) {
            emitter.onSuccess(response.data());
          }
        } catch (ApolloException e) {
          Exceptions.throwIfFatal(e);
          if (!emitter.isDisposed()) {
            emitter.onError(e);
          }
        }
      }
    });
  }

  /**
   * Converts an {@link ApolloPrefetch} to a Completable
   *
   * @param prefetch the ApolloPrefetch to convert
   * @return the converted Completable
   * @throws NullPointerException if originalCall == null
   */
  @Nonnull public static Completable from(@Nonnull final ApolloPrefetch prefetch) {
    checkNotNull(prefetch, "prefetch == null");

    return Completable.create(new CompletableOnSubscribe() {
      @Override public void subscribe(CompletableEmitter emitter) {
        cancelCompletableOnDisposed(emitter, prefetch);
        try {
          prefetch.execute();
          if (!emitter.isDisposed()) {
            emitter.onComplete();
          }
        } catch (ApolloException e) {
          Exceptions.throwIfFatal(e);
          if (emitter.isDisposed()) {
            emitter.onError(e);
          }
        }
      }
    });
  }

  private static void cancelCompletableOnDisposed(CompletableEmitter emitter, final ApolloPrefetch prefetch) {
    emitter.setCancellable(new Cancellable() {
      @Override public void cancel() throws Exception {
        prefetch.cancel();
      }
    });
  }

  private static <T> void cancelSingleOnDisposed(SingleEmitter<T> emitter, final ApolloCall<T> originalCall) {
    emitter.setCancellable(new Cancellable() {
      @Override public void cancel() throws Exception {
        originalCall.cancel();
      }
    });
  }
}
