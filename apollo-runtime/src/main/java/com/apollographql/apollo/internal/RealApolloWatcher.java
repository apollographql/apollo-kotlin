package com.apollographql.apollo.internal;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloWatcher;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.api.graphql.util.Utils;
import com.apollographql.apollo.internal.cache.normalized.Cache;
import com.apollographql.apollo.cache.normalized.CacheControl;

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class RealApolloWatcher<T> implements ApolloWatcher<T> {
  private RealApolloCall<T> activeCall;
  @Nullable private ApolloCall.Callback<T> callback = null;
  private CacheControl refetchCacheControl = CacheControl.CACHE_FIRST;
  private volatile boolean isActive = true;
  private boolean executed = false;
  private final Cache cache;
  private Set<String> dependentKeys = Collections.emptySet();
  private final Cache.RecordChangeSubscriber recordChangeSubscriber = new Cache.RecordChangeSubscriber() {
    @Override public void onCacheKeysChanged(Set<String> changedCacheKeys) {
      if (!Utils.areDisjoint(dependentKeys, changedCacheKeys)) {
        refetch();
      }
    }
  };

  RealApolloWatcher(RealApolloCall<T> originalCall, Cache cache) {
    activeCall = originalCall;
    this.cache = cache;
  }

  @Override public void enqueueAndWatch(@Nullable final ApolloCall.Callback<T> callback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed.");
      executed = true;
    }
    this.callback = callback;
    activeCall.enqueue(callbackProxy(this.callback, activeCall));
  }

  @Nonnull @Override public RealApolloWatcher<T> refetchCacheControl(@Nonnull CacheControl cacheControl) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
    }
    Utils.checkNotNull(cacheControl, "httpCacheControl == null");
    this.refetchCacheControl = cacheControl;
    return this;
  }

  @Override public void cancel() {
    isActive = false;
    activeCall.cancel();
    cache.unsubscribe(recordChangeSubscriber);
  }

  private void refetch() {
    activeCall.cancel();
    cache.unsubscribe(recordChangeSubscriber);
    activeCall = activeCall.clone().cacheControl(refetchCacheControl);
    activeCall.enqueue(callbackProxy(this.callback, activeCall));
  }

  private ApolloCall.Callback<T> callbackProxy(final ApolloCall.Callback<T> sourceCallback,
      final RealApolloCall<T> call) {
    return new ApolloCall.Callback<T>() {
      @Override public void onResponse(@Nonnull Response<T> response) {
        if (isActive) {
          sourceCallback.onResponse(response);
          dependentKeys = response.dependentKeys();
          cache.subscribe(recordChangeSubscriber);
        }
      }

      @Override public void onFailure(@Nonnull Throwable t) {
        sourceCallback.onFailure(t);
        isActive = false;
      }
    };
  }

}
