package com.apollographql.apollo.rx2;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloPrefetch;
import com.apollographql.apollo.ApolloQueryWatcher;
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
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * The Rx2Apollo class provides methods for converting ApolloCall, ApolloPrefetch and ApolloWatcher types to RxJava 2
 * sources.
 */
public class Rx2Apollo {

  private Rx2Apollo() {
    throw new AssertionError("This class cannot be instantiated");
  }

  /**
   * Converts an {@link ApolloQueryWatcher} to an asynchronous Observable.
   *
   * @param watcher the ApolloQueryWatcher to convert.
   * @param <T>     the value type
   * @return the converted Observable
   * @throws NullPointerException if watcher == null
   */
  public static <T> Observable<Response<T>> from(@Nonnull final ApolloQueryWatcher<T> watcher) {
    checkNotNull(watcher, "watcher == null");
    return Observable.create(new ObservableOnSubscribe<Response<T>>() {
      @Override public void subscribe(final ObservableEmitter<Response<T>> emitter) throws Exception {
        cancelOnObservableDisposed(emitter, watcher);

        watcher.enqueueAndWatch(new ApolloCall.Callback<T>() {
          @Override public void onResponse(@Nonnull Response<T> response) {
            if (!emitter.isDisposed()) {
              emitter.onNext(response);
            }
          }

          @Override public void onFailure(@Nonnull ApolloException e) {
            Exceptions.throwIfFatal(e);
            if (!emitter.isDisposed()) {
              emitter.onError(e);
            }
          }
        });
      }
    });
  }

  /**
   * Converts an {@link ApolloCall} to an {@link Observable}. The number of emissions this Observable will have is based
   * on the {@link com.apollographql.apollo.fetcher.ResponseFetcher} used with the call.
   *
   * @param originalCall the ApolloCall to convert
   * @param <T>          the value type.
   * @return the converted Observable
   * @throws NullPointerException if originalCall == null
   */
  @Nonnull public static <T> Observable<Response<T>> from(@Nonnull final ApolloCall<T> originalCall) {
    checkNotNull(originalCall, "call == null");

    return Observable.create(new ObservableOnSubscribe<Response<T>>() {
      @Override public void subscribe(final ObservableEmitter<Response<T>> emitter) throws Exception {
        cancelOnObservableDisposed(emitter, originalCall);
        originalCall.enqueue(new ApolloCall.Callback<T>() {
          @Override public void onResponse(@Nonnull Response<T> response) {
            if (!emitter.isDisposed()) {
              emitter.onNext(response);
            }
          }

          @Override public void onFailure(@Nonnull ApolloException e) {
            Exceptions.throwIfFatal(e);
            if (!emitter.isDisposed()) {
              emitter.onError(e);
            }
          }

          @Override public void onStatusEvent(@Nonnull ApolloCall.StatusEvent event) {
            if (event == ApolloCall.StatusEvent.COMPLETED && !emitter.isDisposed()) {
              emitter.onComplete();
            }
          }
        });
      }
    });
  }

  /**
   * Converts an {@link ApolloPrefetch} to a synchronous Completable
   *
   * @param prefetch the ApolloPrefetch to convert
   * @return the converted Completable
   * @throws NullPointerException if prefetch == null
   */
  @Nonnull public static Completable from(@Nonnull final ApolloPrefetch prefetch) {
    checkNotNull(prefetch, "prefetch == null");

    return Completable.create(new CompletableOnSubscribe() {
      @Override public void subscribe(final CompletableEmitter emitter) {
        cancelOnCompletableDisposed(emitter, prefetch);
        prefetch.enqueue(new ApolloPrefetch.Callback() {
          @Override public void onSuccess() {
            if (!emitter.isDisposed()) {
              emitter.onComplete();
            }
          }

          @Override public void onFailure(@Nonnull ApolloException e) {
            Exceptions.throwIfFatal(e);
            if (!emitter.isDisposed()) {
              emitter.onError(e);
            }
          }
        });
      }
    });
  }

  private static void cancelOnCompletableDisposed(CompletableEmitter emitter, final Cancelable cancelable) {
    emitter.setDisposable(getRx2Disposable(cancelable));
  }

  private static <T> void cancelOnObservableDisposed(ObservableEmitter<T> emitter, final Cancelable cancelable) {
    emitter.setDisposable(getRx2Disposable(cancelable));
  }

  private static Disposable getRx2Disposable(final Cancelable cancelable) {
    return new Disposable() {
      @Override public void dispose() {
        cancelable.cancel();
      }

      @Override public boolean isDisposed() {
        return cancelable.isCanceled();
      }
    };
  }
}
