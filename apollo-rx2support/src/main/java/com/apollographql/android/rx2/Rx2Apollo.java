package com.apollographql.android.rx2;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloPrefetch;
import com.apollographql.apollo.ApolloWatcher;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.internal.util.Cancelable;

import javax.annotation.Nonnull;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Cancellable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * The Rx2Apollo class provides methods for converting ApolloCall, ApolloPrefetch
 * and ApolloWatcher types to RxJava 2 sources.
 */
public class Rx2Apollo {

  private Rx2Apollo() {
    throw new AssertionError("This class cannot be instantiated");
  }

  /**
   * <p>Converts an {@link ApolloPrefetch} to an Observable.</p>
   *
   * <b>Note</b>: If you need a reactive type with back pressure support,
   *
   * @param watcher the ApolloWatcher to convert.
   * @param <T>     the value type
   * @return the converted Observable
   * @throws NullPointerException if watcher == null
   */
  public static <T> Observable<T> from(@Nonnull final ApolloWatcher<T> watcher) {
    checkNotNull(watcher, "watcher == null");
    return Observable.create(new ObservableOnSubscribe<T>() {
      @Override public void subscribe(final ObservableEmitter<T> emitter) throws Exception {
        cancelOnObservableDisposed(emitter, watcher);

        watcher.enqueueAndWatch(new ApolloCall.Callback<T>() {
          @Override public void onResponse(@Nonnull Response<T> response) {
            emitter.onNext(response.data());
          }

          @Override public void onFailure(@Nonnull ApolloException e) {
            Exceptions.throwIfFatal(e);
            emitter.onError(e);
          }
        });
      }
    });
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
        cancelOnSingleDisposed(emitter, originalCall);
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
   * @throws NullPointerException if prefetch == null
   */
  @Nonnull public static Completable from(@Nonnull final ApolloPrefetch prefetch) {
    checkNotNull(prefetch, "prefetch == null");

    return Completable.create(new CompletableOnSubscribe() {
      @Override public void subscribe(CompletableEmitter emitter) {
        cancelOnCompletableDisposed(emitter, prefetch);
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

  private static void cancelOnCompletableDisposed(CompletableEmitter emitter, final Cancelable cancelable) {
    emitter.setCancellable(getRx2Cancellable(cancelable));
  }

  private static <T> void cancelOnSingleDisposed(SingleEmitter<T> emitter, final Cancelable cancelable) {
    emitter.setCancellable(getRx2Cancellable(cancelable));
  }

  private static <T> void cancelOnObservableDisposed(ObservableEmitter<T> emitter, final Cancelable cancelable) {
    emitter.setCancellable(getRx2Cancellable(cancelable));
  }

  private static Cancellable getRx2Cancellable(final Cancelable apolloCancelable) {
    return new Cancellable() {
      @Override public void cancel() throws Exception {
        apolloCancelable.cancel();
      }
    };
  }
}
