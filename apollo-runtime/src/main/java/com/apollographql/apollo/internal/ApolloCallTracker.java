package com.apollographql.apollo.internal;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloPrefetch;
import com.apollographql.apollo.IdleCallback;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * ApolloCallTracker is responsible for keeping track of running {@link ApolloCall} & {@link ApolloPrefetch} objects.
 */
public final class ApolloCallTracker {

  private IdleCallback idleCallback;

  private final Deque<ApolloCall> runningSyncCalls = new ArrayDeque<>();
  private final Deque<RealApolloCall.AsyncCall> runningAsyncCalls = new ArrayDeque<>();
  private final Deque<ApolloPrefetch> runningSyncPrefetches = new ArrayDeque<>();
  private final Deque<RealApolloPrefetch.AsyncCall> runningAsyncPrefetches = new ArrayDeque<>();

  public ApolloCallTracker() {
  }

  /**
   * <p>Adds this {@link ApolloPrefetch} to the underlying data structure keeping track of the in progress synchronous
   * prefetch objects.</p>
   *
   * <p><b>Note</b>: This method needs to be called right before a prefetch call is executed.</p>
   */
  synchronized void syncPrefetchInProgress(ApolloPrefetch apolloPrefetch) {
    runningSyncPrefetches.add(apolloPrefetch);
  }

  /**
   * <p>Removes this {@link ApolloPrefetch} from the underlying data structure keeping track of the in progress
   * synchronous prefetch objects, if it is found, else throws an {@link AssertionError}.</p> If the removal operation
   * is successful and no active running calls are found, then the registered {@link ApolloCallTracker#idleCallback} is
   * invoked.
   *
   * <p><b>Note</b>: This method needs to be called right after a prefetch call is completed (whether successful or
   * failed).</p>
   */
  void syncPrefetchFinished(ApolloPrefetch apolloPrefetch) {
    IdleCallback idleCallback;
    int runningCallsCount;
    synchronized (this) {
      if (!runningSyncPrefetches.remove(apolloPrefetch)) {
        throw new AssertionError("Prefetch call wasn't in progress");
      }
      runningCallsCount = activeCallsCount();
      idleCallback = this.idleCallback;
    }
    executeCallBackIfCallsAreFinished(runningCallsCount, idleCallback);
  }

  /**
   * <p>Adds this asyncCall representing an asynchronous {@link ApolloPrefetch} to the underlying data structure keeping
   * track of the in progress asynchronous prefetch objects.</p>
   *
   * <p><b>Note</b>: This method needs to be called right before a prefetch call is executed.</p>
   */
  synchronized void asyncPrefetchInProgress(RealApolloPrefetch.AsyncCall asyncCall) {
    runningAsyncPrefetches.add(asyncCall);
  }

  /**
   * <p>Removes this asyncCall representing an asynchronous {@link ApolloPrefetch} from the underlying data structure
   * keeping track of the in progress asynchronous prefetch objects, if it is found, else throws an {@link
   * AssertionError}.</p> If the removal operation is successful and no active running calls are found, then the
   * registered {@link ApolloCallTracker#idleCallback} is invoked.
   *
   * <p><b>Note</b>: This method needs to be called right after a prefetch call is completed (whether successful or
   * failed).</p>
   */
  void asyncPrefetchFinished(RealApolloPrefetch.AsyncCall asyncCall) {
    IdleCallback idleCallback;
    int runningCallsCount;
    synchronized (this) {
      if (!runningAsyncPrefetches.remove(asyncCall)) {
        throw new AssertionError("Prefetch call wasn't in progress");
      }
      runningCallsCount = activeCallsCount();
      idleCallback = this.idleCallback;
    }
    executeCallBackIfCallsAreFinished(runningCallsCount, idleCallback);
  }

  /**
   * <p>Adds this {@link ApolloCall} to the underlying data structure keeping track of the in progress synchronous
   * apolloCall objects.</p>
   *
   * <p><b>Note</b>: This method needs to be called right before an apolloCall is executed.</p>
   */
  synchronized void syncCallInProgress(ApolloCall apolloCall) {
    runningSyncCalls.add(apolloCall);
  }

  /**
   * <p>Removes this {@link ApolloCall} from the underlying data structure keeping track of the in progress synchronous
   * apolloCall objects, if it is found, else throws an {@link AssertionError}.</p> If the removal operation is
   * successful and no active running calls are found, then the registered {@link ApolloCallTracker#idleCallback} is
   * invoked.
   *
   * <p><b>Note</b>: This method needs to be called right after an apolloCall is completed (whether successful or
   * failed).</p>
   */
  void syncCallFinished(ApolloCall apolloCall) {
    IdleCallback idleCallback;
    int runningCallsCount;
    synchronized (this) {
      if (!runningSyncCalls.remove(apolloCall)) {
        throw new AssertionError("Call wasn't in progress");
      }
      runningCallsCount = activeCallsCount();
      idleCallback = this.idleCallback;
    }
    executeCallBackIfCallsAreFinished(runningCallsCount, idleCallback);
  }

  /**
   * <p>Adds this asyncCall representing an asynchronous {@link ApolloCall} to the underlying data structure keeping
   * track of the in progress asynchronous apolloCall objects.</p>
   *
   * <p><b>Note</b>: This method needs to be called right before an apolloCall is executed.</p>
   */
  synchronized void asyncCallInProgress(RealApolloCall<?>.AsyncCall asyncCall) {
    runningAsyncCalls.add(asyncCall);
  }

  /**
   * <p>Removes this asyncCall representing an asynchronous {@link ApolloCall} from the underlying data structure
   * keeping track of the in progress asynchronous apolloCall objects, if it is found, else throws an {@link
   * AssertionError}.</p> If the removal operation is successful and no active running calls are found, then the
   * registered {@link ApolloCallTracker#idleCallback} is invoked.
   *
   * <p><b>Note</b>: This method needs to be called right after an apolloCall is completed (whether successful or
   * failed).</p>
   */
  void asyncCallFinished(RealApolloCall<?>.AsyncCall asyncCall) {
    IdleCallback idleCallback;
    int runningCallsCount;
    synchronized (this) {
      if (!runningAsyncCalls.remove(asyncCall)) {
        throw new AssertionError("Call wasn't in progress");
      }
      runningCallsCount = activeCallsCount();
      idleCallback = this.idleCallback;
    }
    executeCallBackIfCallsAreFinished(runningCallsCount, idleCallback);
  }

  /**
   * Registers idleCallback which is invoked when the apolloClient becomes idle.
   */
  public synchronized void setIdleCallback(IdleCallback idleCallback) {
    this.idleCallback = idleCallback;
  }

  /**
   * Returns a total count of in progress {@link ApolloCall} & {@link ApolloPrefetch} objects.
   */
  public int activeCallsCount() {
    return runningAsyncCalls.size()
        + runningSyncCalls.size()
        + runningSyncPrefetches.size()
        + runningAsyncPrefetches.size();
  }

  private void executeCallBackIfCallsAreFinished(int runningCallsCount, IdleCallback idleCallback) {
    if (runningCallsCount == 0 && idleCallback != null) {
      idleCallback.onIdle();
    }
  }
}
