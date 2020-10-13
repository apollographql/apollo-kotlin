package com.apollographql.apollo.internal;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloQueryWatcher;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.internal.ApolloLogger;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.fetcher.ApolloResponseFetchers;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;
import static com.apollographql.apollo.internal.CallState.ACTIVE;
import static com.apollographql.apollo.internal.CallState.CANCELED;
import static com.apollographql.apollo.internal.CallState.IDLE;
import static com.apollographql.apollo.internal.CallState.TERMINATED;

final class RealApolloQueryWatcher<D extends Query.Data> implements ApolloQueryWatcher<D> {
  private RealApolloCall<D> activeCall;
  private ResponseFetcher refetchResponseFetcher = ApolloResponseFetchers.CACHE_FIRST;
  final ApolloStore apolloStore;
  Set<String> dependentKeys = Collections.emptySet();
  final ApolloLogger logger;
  private final ApolloCallTracker tracker;
  final ApolloStore.RecordChangeSubscriber recordChangeSubscriber = new ApolloStore.RecordChangeSubscriber() {
    @Override public void onCacheRecordsChanged(Set<String> changedRecordKeys) {
      if (dependentKeys.isEmpty() || !areDisjoint(dependentKeys, changedRecordKeys)) {
        refetch();
      }
    }
  };
  private final AtomicReference<CallState> state = new AtomicReference<>(IDLE);
  private final AtomicReference<ApolloCall.Callback<D>> originalCallback = new AtomicReference<>();

  RealApolloQueryWatcher(RealApolloCall<D> originalCall, ApolloStore apolloStore, ApolloLogger logger, ApolloCallTracker tracker) {
    this.activeCall = originalCall;
    this.apolloStore = apolloStore;
    this.logger = logger;
    this.tracker = tracker;
  }

  @Override public ApolloQueryWatcher<D> enqueueAndWatch(@Nullable final ApolloCall.Callback<D> callback) {
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

  @NotNull
  @Override public synchronized RealApolloQueryWatcher<D> refetchResponseFetcher(@NotNull ResponseFetcher fetcher) {
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

  @NotNull @Override public Operation operation() {
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

  @NotNull @Override public ApolloQueryWatcher<D> clone() {
    return new RealApolloQueryWatcher<>(activeCall.clone(), apolloStore, logger, tracker);
  }

  private ApolloCall.Callback<D> callbackProxy() {
    return new ApolloCall.Callback<D>() {
      @Override public void onResponse(@NotNull Response<D> response) {
        Optional<ApolloCall.Callback<D>> callback = responseCallback();
        if (!callback.isPresent()) {
          logger.d("onResponse for watched operation: %s. No callback present.", operation().name().name());
          return;
        }
        dependentKeys = response.getDependentKeys();
        apolloStore.subscribe(recordChangeSubscriber);
        callback.get().onResponse(response);
      }

      @Override public void onFailure(@NotNull ApolloException e) {
        Optional<ApolloCall.Callback<D>> callback = terminate();
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

      @Override public void onStatusEvent(@NotNull ApolloCall.StatusEvent event) {
        ApolloCall.Callback<D> callback = originalCallback.get();
        if (callback == null) {
          logger.d("onStatusEvent for operation: %s. No callback present.", operation().name().name());
          return;
        }
        callback.onStatusEvent(event);
      }
    };
  }

  private synchronized void activate(Optional<ApolloCall.Callback<D>> callback) throws ApolloCanceledException {
    switch (state.get()) {
      case IDLE:
        originalCallback.set(callback.orNull());
        tracker.registerQueryWatcher(this);
        break;
      case CANCELED:
        throw new ApolloCanceledException();
      case TERMINATED:
      case ACTIVE:
        throw new IllegalStateException("Already Executed");
      default:
        throw new IllegalStateException("Unknown state");
    }
    state.set(ACTIVE);
  }

  synchronized Optional<ApolloCall.Callback<D>> responseCallback() {
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

  synchronized Optional<ApolloCall.Callback<D>> terminate() {
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

  /**
   * Checks if two {@link Set} are disjoint. Returns true if the sets don't have a single common element. Also returns
   * true if either of the sets is null.
   *
   * @param setOne the first set
   * @param setTwo the second set
   * @param <E> the value type contained within the sets
   * @return True if the sets don't have a single common element or if either of the sets is null.
   */
  private static <E> boolean areDisjoint(Set<E> setOne, Set<E> setTwo) {
    if (setOne == null || setTwo == null) {
      return true;
    }
    Set<E> smallerSet = setOne;
    Set<E> largerSet = setTwo;
    if (setOne.size() > setTwo.size()) {
      smallerSet = setTwo;
      largerSet = setOne;
    }
    for (E el : smallerSet) {
      if (largerSet.contains(el)) {
        return false;
      }
    }
    return true;
  }
}
