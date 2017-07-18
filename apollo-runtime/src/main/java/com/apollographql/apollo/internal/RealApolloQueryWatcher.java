package com.apollographql.apollo.internal;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloQueryWatcher;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
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
  private ApolloCall.Callback<T> callback;
  private ResponseFetcher refetchResponseFetcher = ApolloResponseFetchers.CACHE_FIRST;
  private volatile boolean canceled;
  private boolean executed = false;
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
    }
    this.callback = callback;
    tracker.registerQueryWatcher(this);
    activeCall.enqueue(callbackProxy(this.callback));
    return this;
  }

  @Nonnull @Override public RealApolloQueryWatcher<T> refetchResponseFetcher(@Nonnull ResponseFetcher fetcher) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
    }
    checkNotNull(fetcher, "responseFetcher == null");
    this.refetchResponseFetcher = fetcher;
    return this;
  }

  @Override public void cancel() {
    synchronized (this) {
      canceled = true;
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
    if (canceled) return;

    synchronized (this) {
      apolloStore.unsubscribe(recordChangeSubscriber);
      activeCall.cancel();
      if (!canceled) {
        activeCall = activeCall.clone().responseFetcher(refetchResponseFetcher);
        activeCall.enqueue(callbackProxy(this.callback));
      }
    }
  }

  private ApolloCall.Callback<T> callbackProxy(final ApolloCall.Callback<T> sourceCallback) {
    return new ApolloCall.Callback<T>() {
      @Override public void onResponse(@Nonnull Response<T> response) {
        if (canceled) return;
        dependentKeys = response.dependentKeys();
        apolloStore.subscribe(recordChangeSubscriber);
        sourceCallback.onResponse(response);
      }

      @Override public void onHttpError(@Nonnull ApolloHttpException e) {
        if (canceled) return;
        sourceCallback.onHttpError(e);
      }

      @Override public void onNetworkError(@Nonnull ApolloNetworkException e) {
        if (canceled) return;
        sourceCallback.onNetworkError(e);
      }

      @Override public void onParseError(@Nonnull ApolloParseException e) {
        if (canceled) return;
        sourceCallback.onParseError(e);
      }

      @Override public void onCanceledError(@Nonnull ApolloCanceledException e) {
        if (canceled) return;
        sourceCallback.onCanceledError(e);
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        if (canceled) return;
        sourceCallback.onFailure(e);
      }
    };
  }
}
