package com.apollographql.apollo.rx;


import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloPrefetch;
import com.apollographql.apollo.ApolloQueryWatcher;
import com.apollographql.apollo.ApolloSubscriptionCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.internal.util.Cancelable;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jetbrains.annotations.NotNull;

import rx.Completable;
import rx.CompletableSubscriber;
import rx.Emitter;
import rx.Observable;
import rx.Subscription;
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Cancellable;
import rx.subscriptions.Subscriptions;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * The RxApollo class provides methods for converting ApolloCall and ApolloWatcher types to RxJava 1 Observables.
 */
public final class RxApollo {

  private RxApollo() {
  }

  /**
   * Converts an {@link ApolloQueryWatcher} into an Observable. Honors the back pressure from downstream with the back
   * pressure strategy {@link rx.Emitter.BackpressureMode#LATEST}.
   *
   * @param watcher the ApolloQueryWatcher to convert
   * @param <T>     the value type
   * @return the converted Observable
   */
  @NotNull
  public static <T> Observable<Response<T>> from(@NotNull final ApolloQueryWatcher<T> watcher) {
    return from(watcher, Emitter.BackpressureMode.LATEST);
  }

  /**
   * Converts an {@link ApolloQueryWatcher} into an Observable.
   *
   * @param watcher          the ApolloQueryWatcher to convert
   * @param backpressureMode the back pressure strategy to apply to the observable source.
   * @param <T>              the value type
   * @return the converted Observable
   */
  @NotNull public static <T> Observable<Response<T>> from(@NotNull final ApolloQueryWatcher<T> watcher,
      @NotNull Emitter.BackpressureMode backpressureMode) {
    checkNotNull(backpressureMode, "backpressureMode == null");
    checkNotNull(watcher, "watcher == null");
    return Observable.create(new Action1<Emitter<Response<T>>>() {
      @Override public void call(final Emitter<Response<T>> emitter) {
        final AtomicBoolean canceled = new AtomicBoolean();
        emitter.setCancellation(new Cancellable() {
          @Override public void cancel() throws Exception {
            canceled.set(true);
            watcher.cancel();
          }
        });
        watcher.enqueueAndWatch(new ApolloCall.Callback<T>() {
          @Override public void onResponse(@NotNull Response<T> response) {
            if (!canceled.get()) {
              emitter.onNext(response);
            }
          }

          @Override public void onFailure(@NotNull ApolloException e) {
            Exceptions.throwIfFatal(e);
            if (!canceled.get()) {
              emitter.onError(e);
            }
          }
        });
      }
    }, backpressureMode);
  }

  /**
   * Converts an {@link ApolloCall} to a Observable. The number of emissions this Observable will have is based on the
   * {@link ResponseFetcher} used with the call.
   *
   * @param call             the ApolloCall to convert
   * @param <T>              the value type
   * @param backpressureMode The {@link rx.Emitter.BackpressureMode} to use.
   * @return the converted Observable
   */
  @NotNull public static <T> Observable<Response<T>> from(@NotNull final ApolloCall<T> call,
      Emitter.BackpressureMode backpressureMode) {
    checkNotNull(call, "call == null");
    return Observable.create(new Action1<Emitter<Response<T>>>() {
      @Override public void call(final Emitter<Response<T>> emitter) {
        final AtomicBoolean canceled = new AtomicBoolean();
        emitter.setCancellation(new Cancellable() {
          @Override public void cancel() throws Exception {
            canceled.set(true);
            call.cancel();
          }
        });
        call.enqueue(new ApolloCall.Callback<T>() {
          @Override public void onResponse(@NotNull Response<T> response) {
            if (!canceled.get()) {
              emitter.onNext(response);
            }
          }

          @Override public void onFailure(@NotNull ApolloException e) {
            Exceptions.throwIfFatal(e);
            if (!canceled.get()) {
              emitter.onError(e);
            }
          }

          @Override public void onStatusEvent(@NotNull ApolloCall.StatusEvent event) {
            if (!canceled.get()) {
              if (event == ApolloCall.StatusEvent.COMPLETED) {
                emitter.onCompleted();
              }
            }
          }
        });
      }
    }, backpressureMode);
  }

  @NotNull public static <T> Observable<Response<T>> from(@NotNull final ApolloSubscriptionCall<T> call,
      Emitter.BackpressureMode backpressureMode) {
    checkNotNull(call, "call == null");
    return Observable.create(new Action1<Emitter<Response<T>>>() {
      @Override public void call(final Emitter<Response<T>> emitter) {
        final AtomicBoolean canceled = new AtomicBoolean();
        emitter.setCancellation(new Cancellable() {
          @Override public void cancel() throws Exception {
            canceled.set(true);
            call.cancel();
          }
        });
        call.execute(new ApolloSubscriptionCall.Callback<T>() {
          @Override public void onResponse(@NotNull Response<T> response) {
            if (!canceled.get()) {
              emitter.onNext(response);
            }
          }

          @Override public void onFailure(@NotNull ApolloException e) {
            Exceptions.throwIfFatal(e);
            if (!canceled.get()) {
              emitter.onError(e);
            }
          }

          @Override public void onCompleted() {
            if (!canceled.get()) {
              emitter.onCompleted();
            }
          }
        });
      }
    }, backpressureMode);
  }

  /**
   * Converts an {@link ApolloCall} to a Observable with backpressure mode {@link rx.Emitter.BackpressureMode#BUFFER}.
   * The number of emissions this Observable will have is based on the {@link ResponseFetcher} used with the call.
   *
   * @param call the ApolloCall to convert
   * @param <T>  the value type
   * @return the converted Observable
   */
  @NotNull public static <T> Observable<Response<T>> from(@NotNull final ApolloCall<T> call) {
    return from(call, Emitter.BackpressureMode.BUFFER);
  }

  @NotNull public static <T> Observable<Response<T>> from(@NotNull final ApolloSubscriptionCall<T> call) {
    return from(call, Emitter.BackpressureMode.LATEST);
  }

  /**
   * Converts an {@link ApolloPrefetch} to a Completable.
   *
   * @param prefetch the ApolloPrefetch to convert
   * @return the converted Completable
   */
  @NotNull public static Completable from(@NotNull final ApolloPrefetch prefetch) {
    checkNotNull(prefetch, "prefetch == null");
    return Completable.create(new Completable.OnSubscribe() {
      @Override public void call(final CompletableSubscriber subscriber) {
        final Subscription subscription = getSubscription(subscriber, prefetch);
        prefetch.enqueue(new ApolloPrefetch.Callback() {
          @Override public void onSuccess() {
            if (!subscription.isUnsubscribed()) {
              subscriber.onCompleted();
            }
          }

          @Override public void onFailure(@NotNull ApolloException e) {
            Exceptions.throwIfFatal(e);
            if (!subscription.isUnsubscribed()) {
              subscriber.onError(e);
            }
          }
        });
      }
    });
  }

  static Subscription getSubscription(CompletableSubscriber subscriber, final Cancelable cancelable) {
    Subscription subscription = Subscriptions.create(new Action0() {
      @Override public void call() {
        cancelable.cancel();
      }
    });
    subscriber.onSubscribe(subscription);
    return subscription;
  }

}
