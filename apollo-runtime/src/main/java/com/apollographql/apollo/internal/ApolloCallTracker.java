package com.apollographql.apollo.internal;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloPrefetch;

import java.util.ArrayDeque;
import java.util.Deque;

public class ApolloCallTracker {

  private Runnable idleCallback;

  private final Deque<ApolloCall> runningSyncCalls = new ArrayDeque<>();
  private final Deque<RealApolloCall<?>.AsyncCall> runningAsyncCalls = new ArrayDeque<>();
  private final Deque<ApolloPrefetch> runningSyncPrefetches = new ArrayDeque<>();
  private final Deque<RealApolloPrefetch.AsyncCall> runningAsyncPrefetches = new ArrayDeque<>();

  public ApolloCallTracker() {
  }

  void syncPrefetchInProgress(ApolloPrefetch apolloPrefetch) {
    runningSyncPrefetches.add(apolloPrefetch);
  }

  void syncPrefetchFinished(ApolloPrefetch apolloPrefetch) {
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

  synchronized void asyncPrefetchInProgress(RealApolloPrefetch.AsyncCall asyncCall) {
    runningAsyncPrefetches.add(asyncCall);
  }

  void asyncPrefetchFinished(RealApolloPrefetch.AsyncCall asyncCall) {
    Runnable idleCallback;
    int runningCallsCount;
    synchronized (this) {
      if (!runningAsyncPrefetches.remove(asyncCall)) {
        throw new AssertionError("Prefetch call wasn't in progress");
      }
      runningCallsCount = getRunningCallsCount();
      idleCallback = this.idleCallback;
    }
    executeCallBackIfCallsAreFinished(runningCallsCount, idleCallback);
  }

  synchronized void syncCallInProgress(ApolloCall apolloCall) {
    runningSyncCalls.add(apolloCall);
  }

  void syncCallFinished(ApolloCall apolloCall) {
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

  synchronized void asyncCallInProgress(RealApolloCall<?>.AsyncCall asyncCall) {
    runningAsyncCalls.add(asyncCall);
  }

  void asyncCallFinished(RealApolloCall<?>.AsyncCall asyncCall) {
    Runnable idleCallback;
    int runningCallsCount;
    synchronized (this) {
      if (!runningAsyncCalls.remove(asyncCall)) {
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
