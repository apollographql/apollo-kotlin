package com.apollographql.apollo.mutiny;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloPrefetch;
import com.apollographql.apollo.ApolloQueryWatcher;
import com.apollographql.apollo.ApolloSubscriptionCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.normalized.ApolloStoreOperation;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.internal.subscription.ApolloSubscriptionTerminatedException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import org.jetbrains.annotations.NotNull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * The MutinyApollo class provides methods for converting ApolloCall, ApolloPrefetch and ApolloWatcher types to Mutiny sources.
 */
public class MutinyApollo {

  private MutinyApollo() {
    throw new AssertionError("This class cannot be instantiated");
  }

  /**
   * Converts an {@link ApolloQueryWatcher} to an asynchronous Uni.
   *
   * @param watcher the ApolloQueryWatcher to convert.
   * @param <T> the value type
   * @return the converted Uni
   * @throws NullPointerException if watcher == null
   */
  @NotNull
  public static <T> Uni<Response<T>> from(@NotNull final ApolloQueryWatcher<T> watcher) {
    checkNotNull(watcher, "watcher == null");
    return Uni.createFrom().emitter(emitter -> {
      ApolloQueryWatcher<T> clone = watcher.clone();
      emitter.onTermination(clone::cancel);
      clone.enqueueAndWatch(new ApolloCall.Callback<T>() {
        @Override public void onResponse(@NotNull Response<T> response) {
          emitter.complete(response);
        }

        @Override public void onFailure(@NotNull ApolloException e) {
          emitter.fail(e);
        }
      });
    });
  }

  /**
   * Converts an {@link ApolloCall} to an {@link Uni}. The number of emissions this Uni will have is based on the {@link
   * com.apollographql.apollo.fetcher.ResponseFetcher} used with the call.
   *
   * @param call the ApolloCall to convert
   * @param <T> the value type.
   * @return the converted Uni
   * @throws NullPointerException if originalCall == null
   */
  @NotNull
  public static <T> Uni<Response<T>> from(@NotNull final ApolloCall<T> call) {
    checkNotNull(call, "call == null");
    return Uni.createFrom().emitter(emitter -> {
          ApolloCall<T> clone = call.toBuilder().build();
          emitter.onTermination(clone::cancel);
          clone.enqueue(new ApolloCall.Callback<T>() {
                          @Override public void onResponse(@NotNull Response<T> response) {
                            emitter.complete(response);
                          }

                          @Override public void onFailure(@NotNull ApolloException e) {
                            emitter.fail(e);
                          }

                          @Override public void onStatusEvent(@NotNull ApolloCall.StatusEvent event) {
                            if (event == ApolloCall.StatusEvent.COMPLETED) {
                              emitter.complete(null);
                            }
                          }
                        }
          );
        }
    );
  }

  /**
   * Converts an {@link ApolloPrefetch} to a synchronous Uni<Void>
   *
   * @param prefetch the ApolloPrefetch to convert
   * @return the converted Uni<Void>
   * @throws NullPointerException if prefetch == null
   */
  @NotNull
  public static Uni<Void> from(@NotNull final ApolloPrefetch prefetch) {
    checkNotNull(prefetch, "prefetch == null");
    return Uni.createFrom().emitter(emitter -> {
      ApolloPrefetch clone = prefetch.clone();
      emitter.onTermination(clone::cancel);
      clone.enqueue(new ApolloPrefetch.Callback() {
                      @Override public void onSuccess() {
                        emitter.complete(null);
                      }

                      @Override public void onFailure(@NotNull ApolloException e) {
                        emitter.fail(e);
                      }
                    }
      );
    });
  }

  @NotNull
  public static <T> Multi<Response<T>> from(@NotNull ApolloSubscriptionCall<T> call) {
    return from(call, BackPressureStrategy.LATEST);
  }

  @NotNull
  public static <T> Multi<Response<T>> from(@NotNull final ApolloSubscriptionCall<T> call,
      @NotNull BackPressureStrategy backpressureStrategy) {
    checkNotNull(call, "originalCall == null");
    checkNotNull(backpressureStrategy, "backpressureStrategy == null");
    return Multi.createFrom().emitter(emitter -> {
      ApolloSubscriptionCall<T> clone = call.clone();
      emitter.onTermination(clone::cancel);
      clone.execute(
          new ApolloSubscriptionCall.Callback<T>() {
            @Override public void onResponse(@NotNull Response<T> response) {
              if (!emitter.isCancelled()) {
                emitter.emit(response);
              }
            }

            @Override public void onFailure(@NotNull ApolloException e) {
              if (!emitter.isCancelled()) {
                emitter.fail(e);
              }
            }

            @Override public void onCompleted() {
              if (!emitter.isCancelled()) {
                emitter.complete();
              }
            }

            @Override public void onTerminated() {
              onFailure(new ApolloSubscriptionTerminatedException("Subscription server unexpectedly terminated connection"));
            }

            @Override public void onConnected() {
              //Do nothing when GraphQL subscription server connection is opened
            }
          }
      );
    }, backpressureStrategy);
  }

  /**
   * Converts an {@link ApolloStoreOperation} to a Uni.
   *
   * @param operation the ApolloStoreOperation to convert
   * @param <T> the value type
   * @return the converted Uni
   */
  @NotNull
  public static <T> Uni<T> from(@NotNull final ApolloStoreOperation<T> operation) {
    checkNotNull(operation, "operation == null");
    return Uni.createFrom().emitter(emitter -> operation.enqueue(new ApolloStoreOperation.Callback<T>() {
      @Override
      public void onSuccess(T result) {
        emitter.complete(result);
      }

      @Override
      public void onFailure(@NotNull Throwable t) {
        emitter.fail(t);
      }
    }));
  }
}
