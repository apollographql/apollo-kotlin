package com.apollographql.android.impl;


import com.apollographql.android.ApolloCall;
import com.apollographql.android.ApolloWatcher;
import com.apollographql.android.Cancelable;
import com.apollographql.android.api.graphql.Response;

import java.io.IOException;

import javax.annotation.Nonnull;

import rx.Observable;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

public class RxApollo {
  public static <T> Observable<T> from(final ApolloWatcher<T> watcher) {
    return Observable.create(new Observable.OnSubscribe<T>() {
      @Override public void call(final Subscriber<? super T> subscriber) {
        cancelOnUnsubscribe(subscriber, watcher);
        watcher.enqueueAndWatch(new ApolloCall.Callback<T>() {
          @Override public void onResponse(@Nonnull Response<T> response) {
            if (!subscriber.isUnsubscribed()) {
              subscriber.onNext(response.data());
            }
          }

          @Override public void onFailure(@Nonnull Exception e) {
            Exceptions.throwIfFatal(e);
            if (!subscriber.isUnsubscribed()) {
              subscriber.onError(e);
            }
          }
        });

      }
    });
  }


  public static <T> Observable<T> from(final ApolloCall<T> call) {
    return Observable.create(new Observable.OnSubscribe<T>() {
      @Override public void call(Subscriber<? super T> subscriber) {
        cancelOnUnsubscribe(subscriber, call);
        try {
          Response<T> response = call.execute();
          if (!subscriber.isUnsubscribed()) {
            subscriber.onNext(response.data());
          }
        } catch (IOException e) {
          Exceptions.throwIfFatal(e);
          if (!subscriber.isUnsubscribed()) {
            subscriber.onError(e);
          }
          return;
        }
        if (!subscriber.isUnsubscribed()) {
          subscriber.onCompleted();
        }
      }
    });
  }

  private static <T> void cancelOnUnsubscribe(Subscriber<? super T> subscriber, final Cancelable toCancel) {
    subscriber.add(Subscriptions.create(new Action0() {
      @Override public void call() {
        toCancel.cancel();
      }
    }));
  }
}
