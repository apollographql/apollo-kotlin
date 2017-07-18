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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

final class RealApolloQueryWatcher<T> implements ApolloQueryWatcher<T> {
  private RealApolloCall<T> activeCall;
  private Optional<ApolloCall.Callback<T>> callback = Optional.absent();
  private ResponseFetcher refetchResponseFetcher = ApolloResponseFetchers.CACHE_FIRST;
  private boolean canceled;
  private boolean executed;
  private final ApolloStore apolloStore;
  private Set<String> dependentKeys = Collections.emptySet();
  private final ApolloCallTracker tracker;
  private final ApolloStore.RecordChangeSubscriber recordChangeSubscriber = new ApolloStore.RecordChangeSubscriber() {
    @Override public void onCacheRecordsChanged(Set<String> changedRecordKeys) {
      if (!Utils.areDisjoint(dependentKeys, changedRecordKeys)) {
        refetch();
      }
    }
  };

  RealApolloQueryWatcher(RealApolloCall<T> originalCall, ApolloStore apolloStore, ApolloCallTracker tracker) {
    this.activeCall = originalCall;
    this.apolloStore = apolloStore;
    this.tracker = tracker;
  }

  @Override public ApolloQueryWatcher<T> enqueueAndWatch(@Nullable final ApolloCall.Callback<T> callback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed.");
      executed = true;
      this.callback = Optional.fromNullable(callback);
      tracker.registerQueryWatcher(this);
    }
    activeCall.enqueue(callbackProxy());
    return this;
  }

  @Nonnull @Override public RealApolloQueryWatcher<T> refetchResponseFetcher(@Nonnull ResponseFetcher fetcher) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      checkNotNull(fetcher, "responseFetcher == null");
      this.refetchResponseFetcher = fetcher;
    }
    return this;
  }

  @Override public void cancel() {
    synchronized (this) {
      canceled = true;
      callback = Optional.absent();
      try {
        activeCall.cancel();
        apolloStore.unsubscribe(recordChangeSubscriber);
      } finally {
        tracker.unregisterQueryWatcher(this);
      }
    }
  }

  @Override public boolean isCanceled() {
    return canceled;
  }

  @Nonnull @Override public Operation operation() {
    return activeCall.operation();
  }

  @Override public void refetch() {
    synchronized (this) {
      if (canceled) return;
      apolloStore.unsubscribe(recordChangeSubscriber);
      activeCall.cancel();
      if (!canceled) {
        activeCall = activeCall.clone().responseFetcher(refetchResponseFetcher);
        activeCall.enqueue(callbackProxy());
      }
    }
  }

  private ApolloCall.Callback<T> callbackProxy() {
    return new ApolloCall.Callback<T>() {
      @Override public void onResponse(@Nonnull Response<T> response) {
        synchronized (RealApolloQueryWatcher.this) {
          if (canceled) return;
          if (!callback.isPresent()) return;
          dependentKeys = response.dependentKeys();
          apolloStore.subscribe(recordChangeSubscriber);
          callback.get().onResponse(response);
        }
      }

      @Override public void onHttpError(@Nonnull ApolloHttpException e) {
        synchronized (RealApolloQueryWatcher.this) {
          if (canceled) return;
          if (!callback.isPresent()) return;
          callback.get().onHttpError(e);
        }
      }

      @Override public void onNetworkError(@Nonnull ApolloNetworkException e) {
        synchronized (RealApolloQueryWatcher.this) {
          if (canceled) return;
          if (!callback.isPresent()) return;
          callback.get().onNetworkError(e);
        }
      }

      @Override public void onParseError(@Nonnull ApolloParseException e) {
        synchronized (RealApolloQueryWatcher.this) {
          if (canceled) return;
          if (!callback.isPresent()) return;
          callback.get().onParseError(e);
        }
      }

      @Override public void onCanceledError(@Nonnull ApolloCanceledException e) {
        synchronized (RealApolloQueryWatcher.this) {
          if (canceled) return;
          if (!callback.isPresent()) return;
          callback.get().onCanceledError(e);
        }
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        synchronized (RealApolloQueryWatcher.this) {
          if (canceled) return;
          if (!callback.isPresent()) return;
          callback.get().onFailure(e);
        }
      }
    };
  }
}
