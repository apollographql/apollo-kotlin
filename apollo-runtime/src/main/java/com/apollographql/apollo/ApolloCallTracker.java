package com.apollographql.apollo;

import java.util.ArrayDeque;
import java.util.Deque;

public class ApolloCallTracker {

  private Runnable idleCallback;

  private final Deque<ApolloCall> runningSyncCalls = new ArrayDeque<>();
  private final Deque<ApolloCall.Callback> runningAsyncCalls = new ArrayDeque<>();
  private final Deque<ApolloPrefetch> runningSyncPrefetches = new ArrayDeque<>();
  private final Deque<ApolloPrefetch.Callback> runningAsyncPrefetches = new ArrayDeque<>();

  public ApolloCallTracker() {
  }

  public void syncPrefetchInProgress(ApolloPrefetch apolloPrefetch) {
    runningSyncPrefetches.add(apolloPrefetch);
  }

  public synchronized void syncCallInProgress(ApolloCall apolloCall) {
    runningSyncCalls.add(apolloCall);
  }

  public synchronized void asyncPrefetchInProgress(ApolloPrefetch.Callback responseCallback) {
    runningAsyncPrefetches.add(responseCallback);
  }

  public synchronized void asyncCallInProgress(ApolloCall.Callback callback) {
    runningAsyncCalls.add(callback);
  }

  public void syncPrefetchFinished(ApolloPrefetch apolloPrefetch) {
    Runnable idleCallback;
    int runningCallsCount;
    synchronized (this) {
      if (!runningSyncPrefetches.remove(apolloPrefetch)) {
        throw new AssertionError("Prefetch call wasn't in progress");
      }
      runningCallsCount = getRunningCallsCount();
      idleCallback = this.idleCallback;
    }
    executeCallBackIfCallsAreFinished(runningCallsCount, idleCallback);
  }

  public void syncCallFinished(ApolloCall apolloCall) {
    Runnable idleCallback;
    int runningCallsCount;
    synchronized (this) {
      if (!runningSyncCalls.remove(apolloCall)) {
        throw new AssertionError("Call wasn't in progress");
      }
      runningCallsCount = getRunningCallsCount();
      idleCallback = this.idleCallback;
    }
    executeCallBackIfCallsAreFinished(runningCallsCount, idleCallback);
  }

  public void asyncPrefetchFinished(ApolloPrefetch.Callback responseCallback) {
    Runnable idleCallback;
    int runningCallsCount;
    synchronized (this) {
      if (!runningAsyncPrefetches.remove(responseCallback)) {
        throw new AssertionError("Prefetch call wasn't in progress");
      }
      runningCallsCount = getRunningCallsCount();
      idleCallback = this.idleCallback;
    }
    executeCallBackIfCallsAreFinished(runningCallsCount, idleCallback);
  }

  public void asyncCallFinished(ApolloCall.Callback callback) {
    Runnable idleCallback;
    int runningCallsCount;
    synchronized (this) {
      if (!runningAsyncCalls.remove(callback)) {
        throw new AssertionError("Call wasn't in progress");
      }
      runningCallsCount = getRunningCallsCount();
      idleCallback = this.idleCallback;
    }
    executeCallBackIfCallsAreFinished(runningCallsCount, idleCallback);
  }

  public synchronized void setIdleCallback(Runnable idleCallback) {
    this.idleCallback = idleCallback;
  }

  private void executeCallBackIfCallsAreFinished(int runningCallsCount, Runnable idleCallback) {
    if (runningCallsCount == 0 && idleCallback != null) {
      idleCallback.run();
    }
  }

  public int getRunningCallsCount() {
    return runningAsyncCalls.size() +
        runningSyncCalls.size() +
        runningSyncPrefetches.size() +
        runningAsyncPrefetches.size();
  }
}
