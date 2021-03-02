package com.apollographql.apollo3.rx3;

import com.apollographql.apollo3.ApolloCall;
import com.apollographql.apollo3.ApolloPrefetch;
import com.apollographql.apollo3.ApolloQueryWatcher;
import com.apollographql.apollo3.ApolloSubscriptionCall;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.api.Query;
import com.apollographql.apollo3.exception.ApolloException;
import com.apollographql.apollo3.internal.subscription.ApolloSubscriptionTerminatedException;
import com.apollographql.apollo3.internal.util.Cancelable;
import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableEmitter;
import io.reactivex.rxjava3.core.CompletableOnSubscribe;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableEmitter;
import io.reactivex.rxjava3.core.FlowableOnSubscribe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import org.jetbrains.annotations.NotNull;

import static com.apollographql.apollo3.api.internal.Utils.checkNotNull;

/**
 * The Rx3Apollo class provides methods for converting ApolloCall, ApolloPrefetch and ApolloWatcher types to RxJava 3
 * sources.
 */
public class Rx3Apollo {

  private Rx3Apollo() {
    throw new AssertionError("This class cannot be instantiated");
  }

  /**
   * Converts an {@link ApolloQueryWatcher} to an asynchronous Observable.
   *
   * @param watcher the ApolloQueryWatcher to convert.
   * @param <D>     the value type
   * @return the converted Observable
   * @throws NullPointerException if watcher == null
   */
  @NotNull
  @CheckReturnValue
  public static <D extends Query.Data> Observable<ApolloResponse<D>> from(@NotNull final ApolloQueryWatcher<D> watcher) {
    checkNotNull(watcher, "watcher == null");
    return Observable.create(new ObservableOnSubscribe<ApolloResponse<D>>() {
      @Override public void subscribe(final ObservableEmitter<ApolloResponse<D>> emitter) throws Exception {
        ApolloQueryWatcher<D> clone = watcher.clone();
        cancelOnObservableDisposed(emitter, clone);

        clone.enqueueAndWatch(new ApolloCall.Callback<D>() {

          @Override public void onResponse(@NotNull ApolloResponse<? extends D> response) {
            if (!emitter.isDisposed()) {
              emitter.onNext((ApolloResponse<D>)response);
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
   * on the {@link com.apollographql.apollo3.fetcher.ResponseFetcher} used with the call.
   *
   * @param call the ApolloCall to convert
   * @param <D>  the value type.
   * @return the converted Observable
   * @throws NullPointerException if originalCall == null
   */
  @NotNull
  @CheckReturnValue
  public static <D extends Operation.Data> Observable<ApolloResponse<D>> from(@NotNull final ApolloCall<D> call) {
    checkNotNull(call, "call == null");

    return Observable.create(new ObservableOnSubscribe<ApolloResponse<D>>() {
      @Override public void subscribe(final ObservableEmitter<ApolloResponse<D>> emitter) throws Exception {
        ApolloCall<D> clone = call.clone();
        cancelOnObservableDisposed(emitter, clone);
        clone.enqueue(new ApolloCall.Callback<D>() {
          @Override public void onResponse(@NotNull ApolloResponse<? extends D> response) {
            if (!emitter.isDisposed()) {
              emitter.onNext((ApolloResponse<D>) response);
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
  @NotNull
  @CheckReturnValue
  public static Completable from(@NotNull final ApolloPrefetch prefetch) {
    checkNotNull(prefetch, "prefetch == null");

    return Completable.create(new CompletableOnSubscribe() {
      @Override public void subscribe(final CompletableEmitter emitter) {
        ApolloPrefetch clone = prefetch.clone();
        cancelOnCompletableDisposed(emitter, clone);
        clone.enqueue(new ApolloPrefetch.Callback() {
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

  @NotNull
  @CheckReturnValue
  public static <D extends Operation.Data> Flowable<ApolloResponse<D>> from(@NotNull ApolloSubscriptionCall<D> call) {
    return from(call, BackpressureStrategy.LATEST);
  }

  @NotNull
  @CheckReturnValue
  public static <D extends Operation.Data> Flowable<ApolloResponse<D>> from(@NotNull final ApolloSubscriptionCall<D> call,
      @NotNull BackpressureStrategy backpressureStrategy) {
    checkNotNull(call, "originalCall == null");
    checkNotNull(backpressureStrategy, "backpressureStrategy == null");
    return Flowable.create(new FlowableOnSubscribe<ApolloResponse<D>>() {
      @Override public void subscribe(final FlowableEmitter<ApolloResponse<D>> emitter) throws Exception {
        ApolloSubscriptionCall<D> clone = call.clone();
        cancelOnFlowableDisposed(emitter, clone);
        clone.execute(
            new ApolloSubscriptionCall.Callback<D>() {
              @Override public void onResponse(@NotNull ApolloResponse<? extends D> response) {
                if (!emitter.isCancelled()) {
                  emitter.onNext((ApolloResponse<D>) response);
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

              @Override public void onTerminated() {
                onFailure(new ApolloSubscriptionTerminatedException("Subscription server unexpectedly terminated "
                    + "connection"));
              }

              @Override public void onConnected() {
              }
            }
        );
      }
    }, backpressureStrategy);
  }

  private static void cancelOnCompletableDisposed(CompletableEmitter emitter, final Cancelable cancelable) {
    emitter.setDisposable(getRx3Disposable(cancelable));
  }

  private static <T> void cancelOnObservableDisposed(ObservableEmitter<T> emitter, final Cancelable cancelable) {
    emitter.setDisposable(getRx3Disposable(cancelable));
  }

  private static <T> void cancelOnFlowableDisposed(FlowableEmitter<T> emitter, final Cancelable cancelable) {
    emitter.setDisposable(getRx3Disposable(cancelable));
  }

  private static Disposable getRx3Disposable(final Cancelable cancelable) {
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
