package com.apollographql.apollo.internal;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloQueryWatcher;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.internal.Utils;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.cache.normalized.CacheControl;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class RealApolloQueryWatcher<T> implements ApolloQueryWatcher<T> {
  private RealApolloCall<T> activeCall;
  @Nullable private ApolloCall.Callback<T> callback = null;
  private CacheControl refetchCacheControl = CacheControl.CACHE_FIRST;
  private volatile boolean canceled;
  private boolean executed = false;
  private final ApolloStore apolloStore;
  private Set<String> dependentKeys = Collections.emptySet();
  private final ApolloStore.RecordChangeSubscriber recordChangeSubscriber = new ApolloStore.RecordChangeSubscriber() {
    @Override public void onCacheRecordsChanged(Set<String> changedRecordKeys) {
      if (!Utils.areDisjoint(dependentKeys, changedRecordKeys)) {
        refetch();
      }
    }
  };

  RealApolloQueryWatcher(RealApolloCall<T> originalCall, ApolloStore apolloStore) {
    activeCall = originalCall;
    this.apolloStore = apolloStore;
  }

  @Override public void enqueueAndWatch(@Nullable final ApolloCall.Callback<T> callback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed.");
      executed = true;
    }
    this.callback = callback;
    activeCall.enqueue(callbackProxy(this.callback));
  }

  @Nonnull @Override public RealApolloQueryWatcher<T> refetchCacheControl(@Nonnull CacheControl cacheControl) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
    }
    Utils.checkNotNull(cacheControl, "httpCacheControl == null");
    this.refetchCacheControl = cacheControl;
    return this;
  }

  @Override public void cancel() {
    canceled = true;
    activeCall.cancel();
    apolloStore.unsubscribe(recordChangeSubscriber);
  }

  @Override public boolean isCanceled() {
    return canceled;
  }

  private void refetch() {
    apolloStore.unsubscribe(recordChangeSubscriber);
    activeCall.cancel();
    activeCall = activeCall.clone().cacheControl(refetchCacheControl);
    activeCall.enqueue(callbackProxy(this.callback));
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
