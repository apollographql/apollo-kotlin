package com.apollographql.android.rx;


import com.apollographql.android.ApolloCall;
import com.apollographql.android.ApolloWatcher;
import com.apollographql.android.Cancelable;
import com.apollographql.android.api.graphql.Response;

import java.io.IOException;

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

import static com.apollographql.android.api.graphql.util.Utils.checkNotNull;

public final class RxApollo {

  private RxApollo() {
  }

  @Nonnull
  public static <T> Observable<T> from(@Nonnull final ApolloWatcher<T> watcher) {
    checkNotNull(watcher, "watcher == null");
    return Observable.fromEmitter(new Action1<Emitter<T>>() {
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

          @Override public void onFailure(@Nonnull Throwable e) {
            Exceptions.throwIfFatal(e);
            emitter.onError(e);
          }
        });
      }
    }, Emitter.BackpressureMode.LATEST);
  }

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
        } catch (IOException e) {
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
