package com.apollographql.apollo.internal;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloWatcher;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.internal.Utils;
import com.apollographql.apollo.cache.normalized.CacheControl;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.internal.cache.normalized.Cache;

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
    activeCall.enqueue(callbackProxy(this.callback));
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
    activeCall.enqueue(callbackProxy(this.callback));
  }

  private ApolloCall.Callback<T> callbackProxy(final ApolloCall.Callback<T> sourceCallback) {
    return new ApolloCall.Callback<T>() {
      @Override public void onResponse(@Nonnull Response<T> response) {
        if (isActive) {
          sourceCallback.onResponse(response);
          dependentKeys = response.dependentKeys();
          cache.subscribe(recordChangeSubscriber);
        }
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        isActive = false;
        if (e instanceof ApolloHttpException) {
          sourceCallback.onHttpError((ApolloHttpException) e);
        } else if (e instanceof ApolloParseException) {
          sourceCallback.onParseError((ApolloParseException) e);
        } else if (e instanceof ApolloNetworkException) {
          sourceCallback.onNetworkError((ApolloNetworkException) e);
        } else {
          sourceCallback.onFailure(e);
        }
      }
    };
  }
}
