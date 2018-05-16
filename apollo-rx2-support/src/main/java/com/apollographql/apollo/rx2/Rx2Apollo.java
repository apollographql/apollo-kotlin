package com.apollographql.apollo.rx2;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloPrefetch;
import com.apollographql.apollo.ApolloQueryWatcher;
import com.apollographql.apollo.ApolloSubscriptionCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.internal.util.Cancelable;

import org.jetbrains.annotations.NotNull;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
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
  public static <T> Observable<Response<T>> from(@NotNull final ApolloQueryWatcher<T> watcher) {
    checkNotNull(watcher, "watcher == null");
    return Observable.create(new ObservableOnSubscribe<Response<T>>() {
      @Override public void subscribe(final ObservableEmitter<Response<T>> emitter) throws Exception {
        cancelOnObservableDisposed(emitter, watcher);

        watcher.enqueueAndWatch(new ApolloCall.Callback<T>() {
          @Override public void onResponse(@NotNull Response<T> response) {
            if (!emitter.isDisposed()) {
              emitter.onNext(response);
            }
          }

          @Override public void onFailure(@NotNull ApolloException e) {
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
   * @param call the ApolloCall to convert
   * @param <T>  the value type.
   * @return the converted Observable
   * @throws NullPointerException if originalCall == null
   */
  @NotNull public static <T> Observable<Response<T>> from(@NotNull final ApolloCall<T> call) {
    checkNotNull(call, "call == null");

    return Observable.create(new ObservableOnSubscribe<Response<T>>() {
      @Override public void subscribe(final ObservableEmitter<Response<T>> emitter) throws Exception {
        cancelOnObservableDisposed(emitter, call);
        call.enqueue(new ApolloCall.Callback<T>() {
          @Override public void onResponse(@NotNull Response<T> response) {
            if (!emitter.isDisposed()) {
              emitter.onNext(response);
            }
          }

          @Override public void onFailure(@NotNull ApolloException e) {
            Exceptions.throwIfFatal(e);
            if (!emitter.isDisposed()) {
              emitter.onError(e);
            }
          }

          @Override public void onStatusEvent(@NotNull ApolloCall.StatusEvent event) {
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
  @NotNull public static Completable from(@NotNull final ApolloPrefetch prefetch) {
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

          @Override public void onFailure(@NotNull ApolloException e) {
            Exceptions.throwIfFatal(e);
            if (!emitter.isDisposed()) {
              emitter.onError(e);
            }
          }
        });
      }
    });
  }

  @NotNull public static <T> Flowable<Response<T>> from(@NotNull ApolloSubscriptionCall<T> call) {
    return from(call, BackpressureStrategy.LATEST);
  }

  @NotNull public static <T> Flowable<Response<T>> from(@NotNull final ApolloSubscriptionCall<T> call,
      @NotNull BackpressureStrategy backpressureStrategy) {
    checkNotNull(call, "originalCall == null");
    checkNotNull(backpressureStrategy, "backpressureStrategy == null");
    return Flowable.create(new FlowableOnSubscribe<Response<T>>() {
      @Override public void subscribe(final FlowableEmitter<Response<T>> emitter) throws Exception {
        cancelOnFlowableDisposed(emitter, call);
        call.execute(
            new ApolloSubscriptionCall.Callback<T>() {
              @Override public void onResponse(@NotNull Response<T> response) {
                if (!emitter.isCancelled()) {
                  emitter.onNext(response);
                }
              }

              @Override public void onFailure(@NotNull ApolloException e) {
                Exceptions.throwIfFatal(e);
                if (!emitter.isCancelled()) {
                  emitter.onError(e);
                }
              }

              @Override public void onCompleted() {
                if (!emitter.isCancelled()) {
                  emitter.onComplete();
                }
              }
            }
        );
      }
    }, backpressureStrategy);
  }

  private static void cancelOnCompletableDisposed(CompletableEmitter emitter, final Cancelable cancelable) {
    emitter.setDisposable(getRx2Disposable(cancelable));
  }

  private static <T> void cancelOnObservableDisposed(ObservableEmitter<T> emitter, final Cancelable cancelable) {
    emitter.setDisposable(getRx2Disposable(cancelable));
  }

  private static <T> void cancelOnFlowableDisposed(FlowableEmitter<T> emitter, final Cancelable cancelable) {
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
