package com.apollographql.apollo.internal;

import com.apollographql.apollo.ApolloSubscriptionCall;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.api.internal.ApolloLogger;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.cache.normalized.ApolloStoreOperation;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.cache.normalized.internal.ResponseNormalizer;
import com.apollographql.apollo.cache.normalized.internal.Transaction;
import com.apollographql.apollo.cache.normalized.internal.WriteableStore;
import com.apollographql.apollo.internal.subscription.ApolloSubscriptionException;
import com.apollographql.apollo.internal.subscription.SubscriptionManager;
import com.apollographql.apollo.internal.subscription.SubscriptionResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;
import static com.apollographql.apollo.internal.CallState.ACTIVE;
import static com.apollographql.apollo.internal.CallState.CANCELED;
import static com.apollographql.apollo.internal.CallState.IDLE;
import static com.apollographql.apollo.internal.CallState.TERMINATED;

public class RealApolloSubscriptionCall<D extends Operation.Data> implements ApolloSubscriptionCall<D> {
  private final Subscription<D> subscription;
  private final SubscriptionManager subscriptionManager;
  private final ApolloStore apolloStore;
  private final CachePolicy cachePolicy;
  private final Executor dispatcher;
  private final ApolloLogger logger;
  private final AtomicReference<CallState> state = new AtomicReference<>(IDLE);
  private SubscriptionManagerCallback<D> subscriptionCallback;

  public RealApolloSubscriptionCall(
      @NotNull Subscription<D> subscription,
      @NotNull SubscriptionManager subscriptionManager,
      @NotNull ApolloStore apolloStore,
      @NotNull CachePolicy cachePolicy,
      @NotNull Executor dispatcher,
      @NotNull ApolloLogger logger) {
    this.subscription = subscription;
    this.subscriptionManager = subscriptionManager;
    this.apolloStore = apolloStore;
    this.cachePolicy = cachePolicy;
    this.dispatcher = dispatcher;
    this.logger = logger;
  }

  @Override
  public void execute(@NotNull final Callback<D> callback) throws ApolloCanceledException {
    checkNotNull(callback, "callback == null");
    synchronized (this) {
      switch (state.get()) {
        case IDLE: {
          state.set(ACTIVE);

          if (cachePolicy == CachePolicy.CACHE_AND_NETWORK) {
            dispatcher.execute(new Runnable() {
              @Override public void run() {
                final Response<D> cachedResponse = resolveFromCache();
                if (cachedResponse != null) {
                  callback.onResponse(cachedResponse);
                }
              }
            });
          }

          subscriptionCallback = new SubscriptionManagerCallback<>(callback, this);
          subscriptionManager.subscribe(subscription, subscriptionCallback);
          break;
        }

        case CANCELED:
          throw new ApolloCanceledException();

        case TERMINATED:
        case ACTIVE:
          throw new IllegalStateException("Already Executed");

        default:
          throw new IllegalStateException("Unknown state");
      }
    }
  }

  @Override
  public void cancel() {
    synchronized (this) {
      switch (state.get()) {
        case IDLE: {
          state.set(CANCELED);
          break;
        }

        case ACTIVE: {
          try {
            subscriptionManager.unsubscribe(subscription);
          } finally {
            state.set(CANCELED);
            subscriptionCallback.release();
          }
          break;
        }

        case CANCELED:
        case TERMINATED:
          // These are not illegal states, but cancelling does nothing
          break;

        default:
          throw new IllegalStateException("Unknown state");
      }
    }
  }

  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @Override
  public ApolloSubscriptionCall<D> clone() {
    return new RealApolloSubscriptionCall<>(subscription, subscriptionManager, apolloStore, cachePolicy, dispatcher, logger);
  }

  @Override public boolean isCanceled() {
    return state.get() == CANCELED;
  }

  @NotNull @Override public ApolloSubscriptionCall<D> cachePolicy(@NotNull CachePolicy cachePolicy) {
    checkNotNull(cachePolicy, "cachePolicy is null");
    return new RealApolloSubscriptionCall<>(subscription, subscriptionManager, apolloStore, cachePolicy, dispatcher, logger);
  }

  private void terminate() {
    synchronized (this) {
      switch (state.get()) {
        case ACTIVE: {
          state.set(TERMINATED);
          subscriptionCallback.release();
          break;
        }

        case CANCELED:
          break;

        case IDLE:
        case TERMINATED:
          throw new IllegalStateException(
              CallState.IllegalStateMessage.forCurrentState(state.get()).expected(ACTIVE, CANCELED));

        default:
          throw new IllegalStateException("Unknown state");
      }
    }
  }

  @SuppressWarnings("unchecked")
  private Response<D> resolveFromCache() {
    final ResponseNormalizer<Record> responseNormalizer = apolloStore.cacheResponseNormalizer();

    final ApolloStoreOperation<Response<D>> apolloStoreOperation = apolloStore.read(
        subscription,
        responseNormalizer,
        CacheHeaders.NONE);

    Response<D> cachedResponse = null;
    try {
      cachedResponse = apolloStoreOperation.execute();
    } catch (Exception e) {
      logger.e(e, "Failed to fetch subscription `%s` from the store", subscription);
    }

    if (cachedResponse != null && cachedResponse.getData() != null) {
      logger.d("Cache HIT for subscription `%s`", subscription);
      return cachedResponse;
    } else {
      logger.d("Cache MISS for subscription `%s`", subscription);
      return null;
    }
  }

  private void cacheResponse(final SubscriptionResponse<D> networkResponse) {
    if (networkResponse.cacheRecords.isEmpty() || cachePolicy == CachePolicy.NO_CACHE) {
      return;
    }

    dispatcher.execute(new Runnable() {
      @Override public void run() {
        final Set<String> cacheKeys;
        try {
          cacheKeys = apolloStore.writeTransaction(new Transaction<WriteableStore, Set<String>>() {
            @Nullable @Override public Set<String> execute(WriteableStore cache) {
              return cache.merge(networkResponse.cacheRecords, CacheHeaders.NONE);
            }
          });
        } catch (Exception e) {
          logger.e(e, "Failed to cache response for subscription `%s`", subscription);
          return;
        }

        try {
          apolloStore.publish(cacheKeys);
        } catch (Exception e) {
          logger.e(e, "Failed to publish cache changes for subscription `%s`", subscription);
        }
      }
    });
  }

  private static final class SubscriptionManagerCallback<D extends Operation.Data> implements SubscriptionManager.Callback<D> {
    private Callback<D> originalCallback;
    private RealApolloSubscriptionCall<D> delegate;

    SubscriptionManagerCallback(Callback<D> originalCallback, RealApolloSubscriptionCall<D> delegate) {
      this.originalCallback = originalCallback;
      this.delegate = delegate;
    }

    @Override
    public void onResponse(@NotNull SubscriptionResponse<D> response) {
      Callback<D> callback = this.originalCallback;
      if (callback != null) {
        delegate.cacheResponse(response);
        callback.onResponse(response.response);
      }
    }

    @Override
    public void onError(@NotNull ApolloSubscriptionException error) {
      Callback<D> callback = this.originalCallback;
      if (callback != null) {
        callback.onFailure(error);
      }
      terminate();
    }

    @Override
    public void onNetworkError(@NotNull Throwable t) {
      Callback<D> callback = this.originalCallback;
      if (callback != null) {
        callback.onFailure(new ApolloNetworkException("Subscription failed", t));
      }
      terminate();
    }

    @Override
    public void onCompleted() {
      Callback<D> callback = this.originalCallback;
      if (callback != null) {
        callback.onCompleted();
      }
      terminate();
    }

    @Override
    public void onTerminated() {
      Callback<D> callback = this.originalCallback;
      if (callback != null) {
        callback.onTerminated();
      }
      terminate();
    }

    @Override
    public void onConnected() {
      Callback<D> callback = this.originalCallback;
      if (callback != null) {
        callback.onConnected();
      }
    }

    void terminate() {
      RealApolloSubscriptionCall<D> delegate = this.delegate;
      if (delegate != null) {
        delegate.terminate();
      }
    }

    void release() {
      originalCallback = null;
      delegate = null;
    }
  }
}
