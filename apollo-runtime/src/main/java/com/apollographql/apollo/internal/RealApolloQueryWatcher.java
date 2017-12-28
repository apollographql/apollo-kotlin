package com.apollographql.apollo.internal;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloQueryWatcher;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.internal.Utils;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.fetcher.ApolloResponseFetchers;
import com.apollographql.apollo.fetcher.ResponseFetcher;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;
import static com.apollographql.apollo.internal.CallState.ACTIVE;
import static com.apollographql.apollo.internal.CallState.CANCELED;
import static com.apollographql.apollo.internal.CallState.IDLE;
import static com.apollographql.apollo.internal.CallState.TERMINATED;

final class RealApolloQueryWatcher<T> implements ApolloQueryWatcher<T> {
  private RealApolloCall<T> activeCall;
  private ResponseFetcher refetchResponseFetcher = ApolloResponseFetchers.CACHE_FIRST;
  final ApolloStore apolloStore;
  Set<String> dependentKeys = Collections.emptySet();
  final ApolloLogger logger;
  private final ApolloCallTracker tracker;
  final ApolloStore.RecordChangeSubscriber recordChangeSubscriber = new ApolloStore.RecordChangeSubscriber() {
    @Override public void onCacheRecordsChanged(Set<String> changedRecordKeys) {
      if (!Utils.areDisjoint(dependentKeys, changedRecordKeys)) {
        refetch();
      }
    }
  };
  private final AtomicReference<CallState> state = new AtomicReference<>(IDLE);
  private final AtomicReference<ApolloCall.Callback<T>> originalCallback = new AtomicReference<>();

  RealApolloQueryWatcher(RealApolloCall<T> originalCall, ApolloStore apolloStore, ApolloLogger logger,
      ApolloCallTracker tracker) {
    this.activeCall = originalCall;
    this.apolloStore = apolloStore;
    this.logger = logger;
    this.tracker = tracker;
  }

  @Override public ApolloQueryWatcher<T> enqueueAndWatch(@Nullable final ApolloCall.Callback<T> callback) {
    try {
      activate(Optional.fromNullable(callback));
    } catch (ApolloCanceledException e) {
      if (callback != null) {
        callback.onCanceledError(e);
      } else {
        logger.e(e, "Operation: %s was canceled", operation().name().name());
      }
      return this;
    }
    activeCall.enqueue(callbackProxy());
    return this;
  }

  @Nonnull
  @Override public synchronized RealApolloQueryWatcher<T> refetchResponseFetcher(@Nonnull ResponseFetcher fetcher) {
    if (state.get() != IDLE) throw new IllegalStateException("Already Executed");
    checkNotNull(fetcher, "responseFetcher == null");
    this.refetchResponseFetcher = fetcher;
    return this;
  }

  @Override public synchronized void cancel() {
    switch (state.get()) {
      case ACTIVE:
        try {
          activeCall.cancel();
          apolloStore.unsubscribe(recordChangeSubscriber);
        } finally {
          tracker.unregisterQueryWatcher(this);
          originalCallback.set(null);
          state.set(CANCELED);
        }
        break;
      case IDLE:
        state.set(CANCELED);
        break;
      case CANCELED:
      case TERMINATED:
        // These are not illegal states, but cancelling does nothing
        break;
      default:
        throw new IllegalStateException("Unknown state");
    }
  }

  @Override public boolean isCanceled() {
    return state.get() == CANCELED;
  }

  @Nonnull @Override public Operation operation() {
    return activeCall.operation();
  }

  @Override public synchronized void refetch() {
    switch (state.get()) {
      case ACTIVE:
        apolloStore.unsubscribe(recordChangeSubscriber);
        activeCall.cancel();
        activeCall = activeCall.clone().responseFetcher(refetchResponseFetcher);
        activeCall.enqueue(callbackProxy());
        break;
      case IDLE:
        throw new IllegalStateException("Cannot refetch a watcher which has not first called enqueueAndWatch.");
      case CANCELED:
        throw new IllegalStateException("Cannot refetch a canceled watcher,");
      case TERMINATED:
        throw new IllegalStateException("Cannot refetch a watcher which has experienced an error.");
      default:
        throw new IllegalStateException("Unknown state");

    }

  }

  private ApolloCall.Callback<T> callbackProxy() {
    return new ApolloCall.Callback<T>() {
      @Override public void onResponse(@Nonnull Response<T> response) {
        Optional<ApolloCall.Callback<T>> callback = responseCallback();
        if (!callback.isPresent()) {
          logger.d("onResponse for watched operation: %s. No callback present.", operation().name().name());
          return;
        }
        dependentKeys = response.dependentKeys();
        apolloStore.subscribe(recordChangeSubscriber);
        callback.get().onResponse(response);
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        Optional<ApolloCall.Callback<T>> callback = terminate();
        if (!callback.isPresent()) {
          logger.d(e, "onFailure for operation: %s. No callback present.", operation().name().name());
          return;
        }
        if (e instanceof ApolloHttpException) {
          callback.get().onHttpError((ApolloHttpException) e);
        } else if (e instanceof ApolloParseException) {
          callback.get().onParseError((ApolloParseException) e);
        } else if (e instanceof ApolloNetworkException) {
          callback.get().onNetworkError((ApolloNetworkException) e);
        } else {
          callback.get().onFailure(e);
        }
      }
    };
  }

  private synchronized void activate(Optional<ApolloCall.Callback<T>> callback) throws ApolloCanceledException {
    switch (state.get()) {
      case IDLE:
        originalCallback.set(callback.orNull());
        tracker.registerQueryWatcher(this);
        break;
      case CANCELED:
        throw new ApolloCanceledException("Call is cancelled.");
      case TERMINATED:
      case ACTIVE:
        throw new IllegalStateException("Already Executed");
      default:
        throw new IllegalStateException("Unknown state");
    }
    state.set(ACTIVE);
  }

  synchronized Optional<ApolloCall.Callback<T>> responseCallback() {
    switch (state.get()) {
      case ACTIVE:
      case CANCELED:
        return Optional.fromNullable(originalCallback.get());
      case IDLE:
      case TERMINATED:
        throw new IllegalStateException(
            CallState.IllegalStateMessage.forCurrentState(state.get()).expected(ACTIVE, CANCELED));
      default:
        throw new IllegalStateException("Unknown state");
    }
  }

  synchronized Optional<ApolloCall.Callback<T>> terminate() {
    switch (state.get()) {
      case ACTIVE:
        tracker.unregisterQueryWatcher(this);
        state.set(TERMINATED);
        return Optional.fromNullable(originalCallback.getAndSet(null));
      case CANCELED:
        return Optional.fromNullable(originalCallback.getAndSet(null));
      case IDLE:
      case TERMINATED:
        throw new IllegalStateException(
            CallState.IllegalStateMessage.forCurrentState(state.get()).expected(ACTIVE, CANCELED));
      default:
        throw new IllegalStateException("Unknown state");
    }
  }

}
