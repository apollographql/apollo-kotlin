/*
 * This file is auto generated from apollo-rx2-support-java by rxjava3.main.kts, do not edit manually.
 */
package com.apollographql.apollo.rx3.java;

import com.apollographql.apollo.api.ApolloResponse;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.runtime.java.ApolloCall;
import com.apollographql.apollo.runtime.java.ApolloCallback;
import com.apollographql.apollo.runtime.java.ApolloDisposable;
import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import org.jetbrains.annotations.NotNull;

import static com.apollographql.apollo.api.java.Assertions.checkNotNull;

public class Rx3Apollo {
  @NotNull
  @CheckReturnValue
  public static <T extends Operation.Data> Flowable<ApolloResponse<T>> flowable(@NotNull final ApolloCall<T> call, @NotNull BackpressureStrategy backpressureStrategy) {
    checkNotNull(call, "originalCall == null");
    checkNotNull(backpressureStrategy, "backpressureStrategy == null");
    return Flowable.create(emitter -> {
      ApolloDisposable disposable = call.enqueue(new ApolloCallback<T>() {

        @Override public void onResponse(@NotNull ApolloResponse<T> response) {
          if (!emitter.isCancelled()) {
            emitter.onNext(response);
          }
        }
      });

      disposable.addListener(() -> {
        if (!emitter.isCancelled()) {
          emitter.onComplete();
        }
      });

      emitter.setDisposable(new Disposable() {
        @Override public void dispose() {
          disposable.dispose();
        }

        @Override public boolean isDisposed() {
          return disposable.isDisposed();
        }
      });
    }, backpressureStrategy);
  }

  @NotNull
  @CheckReturnValue
  public static <T extends Operation.Data> Flowable<ApolloResponse<T>> flowable(@NotNull final ApolloCall<T> call) {
    return flowable(call, BackpressureStrategy.BUFFER);
  }

  @NotNull
  @CheckReturnValue
  public static <T extends Operation.Data> Single<ApolloResponse<T>> single(@NotNull final ApolloCall<T> call, @NotNull BackpressureStrategy backpressureStrategy) {
    return flowable(call, backpressureStrategy).firstOrError();
  }

  @NotNull
  @CheckReturnValue
  public static <T extends Operation.Data> Single<ApolloResponse<T>> single(@NotNull final ApolloCall<T> call) {
    return single(call, BackpressureStrategy.BUFFER);
  }
}
