package com.apollographql.apollo;

import com.apollographql.apollo.internal.RealApolloCall;
import com.apollographql.apollo.internal.RealApolloPrefetch;

import java.util.ArrayDeque;
import java.util.Deque;

public class ApolloTracker {

  private Runnable idleCallback;

  private final Deque<ApolloCall> runningSyncCalls = new ArrayDeque<>();
  private final Deque<ApolloCall.Callback> runningAsyncCalls = new ArrayDeque<>();
  private final Deque<ApolloPrefetch> runningSyncPrefetches = new ArrayDeque<>();
  private final Deque<ApolloPrefetch.Callback> runningAsyncPrefetches = new ArrayDeque<>();

  public ApolloTracker() {
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
    Runnable idleCallback;
    int runningCallsCount;
    synchronized (this) {
      if (!runningSyncPrefetches.remove(realApolloPrefetch)) {
        throw new AssertionError("Prefetch call wasn't in progress");
      }
      runningCallsCount = getRunningCallsCount();
      idleCallback = this.idleCallback;
    }
    executeCallBackIfCallsAreFinished(runningCallsCount, idleCallback);
  }

  public void syncCallFinished(RealApolloCall tRealApolloCall) {
    Runnable idleCallback;
    int runningCallsCount;
    synchronized (this) {
      if (!runningSyncCalls.remove(tRealApolloCall)) {
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

  private int getRunningCallsCount() {
    return runningAsyncCalls.size() +
        runningSyncCalls.size() +
        runningSyncPrefetches.size() +
        runningAsyncPrefetches.size();
  }
}
