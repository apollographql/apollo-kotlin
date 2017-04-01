package com.apollographql.android.rx;


import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloWatcher;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.internal.util.Cancelable;

import javax.annotation.Nonnull;

import rx.Emitter;
import rx.Observable;
import rx.Single;
import rx.SingleSubscriber;
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Cancellable;
import rx.subscriptions.Subscriptions;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * The RxApollo class provides methods for converting ApolloCall
 * and ApolloWatcher types to RxJava 1 Observables.
 */
public final class RxApollo {

  private RxApollo() {
  }

  /**
   * Converts an {@link ApolloWatcher} into an Observable. Honors the back pressure from downstream with the back
   * pressure strategy {@link rx.Emitter.BackpressureMode#LATEST}.
   *
   * @param watcher the ApolloWatcher to convert
   * @param <T>     the value type
   * @return the converted Observable
   */
  @Nonnull
  public static <T> Observable<T> from(@Nonnull final ApolloWatcher<T> watcher) {
    return from(watcher, Emitter.BackpressureMode.LATEST);
  }

  /**
   * Converts an {@link ApolloWatcher} into an Observable.
   *
   * @param watcher          the ApolloWatcher to convert
   * @param backpressureMode the back pressure strategy to apply to the observable source.
   * @param <T>              the value type
   * @return the converted Observable
   */
  @Nonnull public static <T> Observable<T> from(@Nonnull final ApolloWatcher<T> watcher,
      @Nonnull Emitter.BackpressureMode backpressureMode) {
    checkNotNull(backpressureMode, "backpressureMode == null");
    checkNotNull(watcher, "watcher == null");
    return Observable.create(new Action1<Emitter<T>>() {
      @Override public void call(final Emitter<T> emitter) {
        emitter.setCancellation(new Cancellable() {
          @Override public void cancel() throws Exception {
            watcher.cancel();
          }
        });
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
    }, backpressureMode);
  }

  /**
   * Converts an {@link ApolloCall} to a Single.
   *
   * @param call the ApolloCall to convert
   * @param <T>  the value type
   * @return the converted Single
   */
  @Nonnull public static <T> Single<T> from(@Nonnull final ApolloCall<T> call) {
    checkNotNull(call, "call == null");
    return Single.create(new Single.OnSubscribe<T>() {
      @Override public void call(SingleSubscriber<? super T> subscriber) {
        cancelOnSingleUnsubscribe(subscriber, call);
        try {
          Response<T> response = call.execute();
          if (!subscriber.isUnsubscribed()) {
            subscriber.onSuccess(response.data());
          }
        } catch (ApolloException e) {
          Exceptions.throwIfFatal(e);
          if (!subscriber.isUnsubscribed()) {
            subscriber.onError(e);
          }
        }
      }
    });
  }

  private static <T> void cancelOnSingleUnsubscribe(SingleSubscriber<? super T> subscriber, final Cancelable toCancel) {
    subscriber.add(Subscriptions.create(new Action0() {
      @Override public void call() {
        toCancel.cancel();
      }
    }));
  }
}
