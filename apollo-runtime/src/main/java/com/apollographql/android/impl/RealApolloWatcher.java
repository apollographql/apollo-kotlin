package com.apollographql.android.impl;

import com.apollographql.android.ApolloCall;
import com.apollographql.android.ApolloWatcher;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.api.graphql.util.Utils;
import com.apollographql.android.cache.normalized.Cache;
import com.apollographql.android.cache.normalized.CacheControl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class RealApolloWatcher<T extends Operation.Data> implements ApolloWatcher<T> {

  private RealApolloCall<T> activeCall;
  @Nullable private ApolloCall.Callback<T> callback = null;
  private CacheControl refetchCacheControl = CacheControl.CACHE_FIRST;
  private volatile boolean isActive = true;
  private boolean executed = false;
  private final Cache cache;
  private final Cache.RecordChangeSubscriber recordChangeSubscriber = new Cache.RecordChangeSubscriber() {
    @Override public void onDependentKeysChanged() {
      refetch();
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
          cache.subscribe(recordChangeSubscriber, call.dependentKeys());
        }
      }

      @Override public void onFailure(@Nonnull Exception e) {
        sourceCallback.onFailure(e);
        isActive = false;
      }
    };
  }

}
