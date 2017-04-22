package com.apollographql.apollo;

import com.apollographql.apollo.internal.RealApolloCall;
import com.apollographql.apollo.internal.RealApolloPrefetch;

import java.util.ArrayDeque;
import java.util.Deque;

public class ApolloTracker {

  private Runnable idleRunnable;

  private final Deque<RealApolloCall> runningSyncCalls = new ArrayDeque<>();
  private final Deque<ApolloCall.Callback> runningAsyncCalls = new ArrayDeque<>();
  private final Deque<RealApolloPrefetch> runningSyncPrefetches = new ArrayDeque<>();
  private final Deque<ApolloPrefetch.Callback> runningAsyncPrefetches = new ArrayDeque<>();

  public ApolloTracker() {
  }

  public void execute(Runnable runnable) {

  }

  public void syncPrefetchInProgress(RealApolloPrefetch realApolloPrefetch) {
    runningSyncPrefetches.add(realApolloPrefetch);
  }

  public synchronized void syncCallInProgress(RealApolloCall realApolloCall) {
    runningSyncCalls.add(realApolloCall);
  }

  public synchronized void asyncPrefetchInProgress(ApolloPrefetch.Callback responseCallback) {
    runningAsyncPrefetches.add(responseCallback);
  }

  public synchronized void asyncCallInProgress(ApolloCall.Callback callback) {
    runningAsyncCalls.add(callback);
  }

  public void syncPrefetchFinished(RealApolloPrefetch realApolloPrefetch) {
    synchronized (this) {
      if (!runningSyncPrefetches.remove(realApolloPrefetch)) {
        throw new AssertionError("Prefetch call wasn't in progress");
      }
    }
  }

  public void syncCallFinished(RealApolloCall tRealApolloCall) {
    synchronized (this) {
      if (!runningSyncCalls.remove(tRealApolloCall)) {
        throw new AssertionError("Call wasn't in progress");
      }
    }
  }

  public void asyncPrefetchFinished(ApolloPrefetch.Callback responseCallback) {
    synchronized (this) {
      if (!runningAsyncPrefetches.remove(responseCallback)) {
        throw new AssertionError("Prefetch call wasn't in progress");
      }
    }
  }

  public void asyncCallFinished(ApolloCall.Callback callback) {
    synchronized (this) {
      if (!runningAsyncCalls.remove(callback)) {
        throw new AssertionError("Call wasn't in progress");
      }
    }
  }
}
