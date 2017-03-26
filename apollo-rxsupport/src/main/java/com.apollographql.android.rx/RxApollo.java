package com.apollographql.android.rx;


import com.apollographql.android.ApolloCall;
import com.apollographql.android.ApolloWatcher;
import com.apollographql.android.api.graphql.Response;

import java.io.IOException;

import javax.annotation.Nonnull;

import rx.Emitter;
import rx.Observable;
import rx.Single;
import rx.SingleEmitter;
import rx.exceptions.Exceptions;
import rx.functions.Action1;
import rx.functions.Cancellable;

import static com.apollographql.android.api.graphql.util.Utils.checkNotNull;

public final class RxApollo {

  private RxApollo() {
  }

  @Nonnull
  public static <T> Observable<T> from(@Nonnull final ApolloWatcher<T> watcher) {
    checkNotNull(watcher);
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
    checkNotNull(call);
    return Single.fromEmitter(new Action1<SingleEmitter<T>>() {
      @Override public void call(SingleEmitter<T> emitter) {
        emitter.setCancellation(new Cancellable() {
          @Override public void cancel() throws Exception {
            call.cancel();
          }
        });

        try {
          Response<T> response = call.execute();
          emitter.onSuccess(response.data());
        } catch (IOException e) {
          Exceptions.throwIfFatal(e);
          emitter.onError(e);
        }
      }
    });
  }
}
