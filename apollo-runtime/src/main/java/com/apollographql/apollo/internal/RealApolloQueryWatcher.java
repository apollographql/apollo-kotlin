package com.apollographql.apollo.internal;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloQueryWatcher;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.internal.Utils;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.fetcher.ApolloResponseFetchers;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.internal.util.ApolloLogger;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

final class RealApolloQueryWatcher<T> implements ApolloQueryWatcher<T> {
  private RealApolloCall<T> activeCall;
  private ResponseFetcher refetchResponseFetcher = ApolloResponseFetchers.CACHE_FIRST;
  private final AtomicBoolean canceled = new AtomicBoolean();
  private final AtomicBoolean executed = new AtomicBoolean();
  private final AtomicBoolean terminated = new AtomicBoolean();
  private final AtomicReference<ApolloCall.Callback<T>> originalCallback = new AtomicReference<>();
  private final ApolloStore apolloStore;
  private final ApolloLogger logger;
  private Set<String> dependentKeys = Collections.emptySet();
  private final ApolloCallTracker tracker;
  private final ApolloStore.RecordChangeSubscriber recordChangeSubscriber = new ApolloStore.RecordChangeSubscriber() {
    @Override public void onCacheRecordsChanged(Set<String> changedRecordKeys) {
      if (!Utils.areDisjoint(dependentKeys, changedRecordKeys)) {
        refetch();
      }
    }
  };

  RealApolloQueryWatcher(RealApolloCall<T> originalCall, ApolloStore apolloStore, ApolloLogger logger,
      ApolloCallTracker tracker) {
    this.activeCall = originalCall;
    this.apolloStore = apolloStore;
    this.logger = logger;
    this.tracker = tracker;
  }

  @Override public ApolloQueryWatcher<T> enqueueAndWatch(@Nullable final ApolloCall.Callback<T> callback) {
    if (!executed.compareAndSet(false, true)) {
      throw new IllegalStateException("Already Executed");
    }
    synchronized (this) {
      this.originalCallback.set(callback);
      tracker.registerQueryWatcher(this);
    }

    activeCall.enqueue(callbackProxy());
    return this;
  }

  @Nonnull @Override public RealApolloQueryWatcher<T> refetchResponseFetcher(@Nonnull ResponseFetcher fetcher) {
    if (!executed.compareAndSet(false, true)) {
      throw new IllegalStateException("Already Executed");
    }
    checkNotNull(fetcher, "responseFetcher == null");
    this.refetchResponseFetcher = fetcher;
    return this;
  }

  @Override public synchronized void cancel() {
    canceled.set(true);
    originalCallback.set(null);
    try {
      activeCall.cancel();
      apolloStore.unsubscribe(recordChangeSubscriber);
    } finally {
      tracker.unregisterQueryWatcher(this);
    }
  }

  @Override public boolean isCanceled() {
    return canceled.get();
  }

  @Nonnull @Override public Operation operation() {
    return activeCall.operation();
  }

  @Override public void refetch() {
    synchronized (this) {
      if (canceled.get()) return;
      if (terminated.get()) throw new IllegalStateException("Cannot refetch a query watcher after a failure.");
      apolloStore.unsubscribe(recordChangeSubscriber);
      activeCall.cancel();
      activeCall = activeCall.clone().responseFetcher(refetchResponseFetcher);
      activeCall.enqueue(callbackProxy());
    }
  }

  private synchronized Optional<ApolloCall.Callback<T>> responseCallback() {
    if (!terminated.compareAndSet(false, true)) {
      throw new IllegalStateException("Operation already terminated or failed: " + operation().name().name());
    }
    return Optional.fromNullable(originalCallback.get());
  }

  private synchronized Optional<ApolloCall.Callback<T>> terminalCallback() {
    if (!terminated.compareAndSet(false, true)) {
      throw new IllegalStateException("Operation already terminated or failed: " + operation().name().name());
    }
    tracker.unregisterQueryWatcher(this);
    return Optional.fromNullable(originalCallback.getAndSet(null));
  }

  private ApolloCall.Callback<T> callbackProxy() {
    return new ApolloCall.Callback<T>() {
      @Override public void onResponse(@Nonnull Response<T> response) {
        final Optional<ApolloCall.Callback<T>> callback = responseCallback();
        if (!callback.isPresent()) {
          logger.d("onResponse for watched operation: %s. No callback present.", operation().name().name());
        }
        dependentKeys = response.dependentKeys();
        apolloStore.subscribe(recordChangeSubscriber);
        originalCallback.get().onResponse(response);
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        Optional<ApolloCall.Callback<T>> callback = terminalCallback();
        if (!callback.isPresent()) {
          logger.d(e, "QueryWatcher for %s was canceled, but experienced exception.", operation().name().name());
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
}
