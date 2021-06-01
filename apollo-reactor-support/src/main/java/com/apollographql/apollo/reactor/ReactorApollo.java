package com.apollographql.apollo.reactor;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloPrefetch;
import com.apollographql.apollo.ApolloQueryWatcher;
import com.apollographql.apollo.ApolloSubscriptionCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.normalized.ApolloStoreOperation;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.internal.subscription.ApolloSubscriptionTerminatedException;
import com.apollographql.apollo.internal.util.Cancelable;
import org.jetbrains.annotations.NotNull;
import reactor.core.Disposable;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * The ReactorApollo class provides methods for converting ApolloCall, ApolloPrefetch and ApolloWatcher types to Reactor sources.
 */
public class ReactorApollo {

  private ReactorApollo() {
    throw new AssertionError("This class cannot be instantiated");
  }

  /**
   * Converts an {@link ApolloQueryWatcher} to an asynchronous Mono.
   *
   * @param watcher the ApolloQueryWatcher to convert.
   * @param <T> the value type
   * @return the converted Mono
   * @throws NullPointerException if watcher == null
   */
  @NotNull
  public static <T> Mono<Response<T>> from(@NotNull final ApolloQueryWatcher<T> watcher) {
    checkNotNull(watcher, "watcher == null");
    return Mono.create(sink -> {
      ApolloQueryWatcher<T> clone = watcher.clone();
      cancelOnMonoDisposed(sink, clone);
      clone.enqueueAndWatch(new ApolloCall.Callback<T>() {
        @Override public void onResponse(@NotNull Response<T> response) {
          sink.success(response);
        }

        @Override public void onFailure(@NotNull ApolloException e) {
          Exceptions.throwIfFatal(e);
          sink.error(e);
        }
      });
    });
  }

  /**
   * Converts an {@link ApolloCall} to an {@link Mono}. The number ofemissions this Mono will have is based on the {@link
   * com.apollographql.apollo.fetcher.ResponseFetcher} used with the call.
   *
   * @param call the ApolloCall to convert
   * @param <T> the value type.
   * @return the converted Mono
   * @throws NullPointerException if originalCall == null
   */
  @NotNull
  public static <T> Mono<Response<T>> from(@NotNull final ApolloCall<T> call) {
    checkNotNull(call, "call == null");
    return Mono.create(sink -> {
      ApolloCall<T> clone = call.toBuilder().build();
      cancelOnMonoDisposed(sink, clone);
      clone.enqueue(new ApolloCall.Callback<T>() {
        @Override public void onResponse(@NotNull Response<T> response) {
          sink.success(response);
        }

        @Override public void onFailure(@NotNull ApolloException e) {
          Exceptions.throwIfFatal(e);
          sink.error(e);
        }

        @Override public void onStatusEvent(@NotNull ApolloCall.StatusEvent event) {
          if (event == ApolloCall.StatusEvent.COMPLETED) {
            sink.success();
          }
        }
      });
    });
  }

  /**
   * Converts an {@link ApolloPrefetch} to a synchronous Mono<Void>
   *
   * @param prefetch the ApolloPrefetch to convert
   * @return the converted Mono<Void>
   * @throws NullPointerException if prefetch == null
   */
  @NotNull
  public static Mono<Void> from(@NotNull final ApolloPrefetch prefetch) {
    checkNotNull(prefetch, "prefetch == null");
    return Mono.create(sink -> {
      ApolloPrefetch clone = prefetch.clone();
      cancelOnMonoDisposed(sink, clone);
      clone.enqueue(new ApolloPrefetch.Callback() {
        @Override public void onSuccess() {
          sink.success();
        }

        @Override public void onFailure(@NotNull ApolloException e) {
          Exceptions.throwIfFatal(e);
          sink.error(e);
        }
      });
    });
  }

  @NotNull
  public static <T> Flux<Response<T>> from(@NotNull ApolloSubscriptionCall<T> call) {
    return from(call, FluxSink.OverflowStrategy.LATEST);
  }

  @NotNull
  public static <T> Flux<Response<T>> from(@NotNull final ApolloSubscriptionCall<T> call,
      @NotNull FluxSink.OverflowStrategy backpressureStrategy) {
    checkNotNull(call, "originalCall == null");
    checkNotNull(backpressureStrategy, "backpressureStrategy == null");
    return Flux.create(sink -> {
      ApolloSubscriptionCall<T> clone = call.clone();
      cancelOnFluxDisposed(sink, clone);
      clone.execute(
          new ApolloSubscriptionCall.Callback<T>() {
            @Override public void onResponse(@NotNull Response<T> response) {
              if (!sink.isCancelled()) {
                sink.next(response);
              }
            }

            @Override public void onFailure(@NotNull ApolloException e) {
              Exceptions.throwIfFatal(e);
              if (!sink.isCancelled()) {
                sink.error(e);
              }
            }

            @Override public void onCompleted() {
              if (!sink.isCancelled()) {
                sink.complete();
              }
            }

            @Override public void onTerminated() {
              onFailure(new ApolloSubscriptionTerminatedException("Subscription server unexpectedly terminated "
                  + "connection"));
            }

            @Override public void onConnected() {
               //Do nothing when GraphQL subscription server connection is opened
            }
          }
      );
    }, backpressureStrategy);
  }

  /**
   * Converts an {@link ApolloStoreOperation} to a Mono.
   *
   * @param operation the ApolloStoreOperation to convert
   * @param <T> the value type
   * @return the converted Mono
   */
  @NotNull
  public static <T> Mono<T> from(@NotNull final ApolloStoreOperation<T> operation) {
    checkNotNull(operation, "operation == null");
    return Mono.create(sink -> operation.enqueue(new ApolloStoreOperation.Callback<T>() {
      @Override
      public void onSuccess(T result) {
        sink.success(result);
      }

      @Override
      public void onFailure(@NotNull Throwable t) {
        sink.error(t);
      }
    }));

  }

  private static <T> void cancelOnMonoDisposed(MonoSink<T> sink, final Cancelable cancelable) {
    sink.onCancel(getReactorDisposable(cancelable));
    sink.onDispose(getReactorDisposable(cancelable));
  }

  private static <T> void cancelOnFluxDisposed(FluxSink<T> sink, final Cancelable cancelable) {
    sink.onCancel(getReactorDisposable(cancelable));
    sink.onDispose(getReactorDisposable(cancelable));
  }

  private static Disposable getReactorDisposable(final Cancelable cancelable) {
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
