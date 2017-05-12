package com.apollographql.apollo.internal;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloPrefetch;
import com.apollographql.apollo.IdleResourceCallback;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * ApolloCallTracker is responsible for keeping track of running {@link ApolloCall} & {@link ApolloPrefetch} objects.
 */
public final class ApolloCallTracker {

  private IdleResourceCallback idleResourceCallback;

  private final Set<ApolloCall> runningSyncCalls = new LinkedHashSet<>();
  private final Set<RealApolloCall.AsyncCall> runningAsyncCalls = new LinkedHashSet<>();
  private final Set<ApolloPrefetch> runningSyncPrefetches = new LinkedHashSet<>();
  private final Set<RealApolloPrefetch.AsyncCall> runningAsyncPrefetches = new LinkedHashSet<>();

  public ApolloCallTracker() {
  }

  /**
   * <p>Adds this {@link ApolloPrefetch} to the underlying data structure keeping track of the in progress synchronous
   * prefetch objects.</p>
   *
   * <p><b>Note</b>: This method needs to be called right before a prefetch call is executed.</p>
   */
  synchronized void onSyncPrefetchInProgress(ApolloPrefetch apolloPrefetch) {
    runningSyncPrefetches.add(apolloPrefetch);
  }

  /**
   * <p>Removes this {@link ApolloPrefetch} from the underlying data structure keeping track of the in progress
   * synchronous prefetch objects, if it is found, else throws an {@link AssertionError}.</p> If the removal operation
   * is successful and no active running calls are found, then the registered
   * {@link ApolloCallTracker#idleResourceCallback} is invoked.
   *
   * <p><b>Note</b>: This method needs to be called right after a prefetch call is completed (whether successful or
   * failed).</p>
   */
  void onSyncPrefetchFinished(ApolloPrefetch apolloPrefetch) {
    IdleResourceCallback idleResourceCallback;
    int runningCallsCount;
    synchronized (this) {
      if (!runningSyncPrefetches.remove(apolloPrefetch)) {
        throw new AssertionError("Prefetch call wasn't in progress");
      }
      runningCallsCount = activeCallsCount();
      idleResourceCallback = this.idleResourceCallback;
    }
    executeCallBackIfCallsAreFinished(runningCallsCount, idleResourceCallback);
  }

  /**
   * <p>Adds this asyncCall representing an asynchronous {@link ApolloPrefetch} to the underlying data structure keeping
   * track of the in progress asynchronous prefetch objects.</p>
   *
   * <p><b>Note</b>: This method needs to be called right before a prefetch call is executed.</p>
   */
  synchronized void onAsyncPrefetchInProgress(RealApolloPrefetch.AsyncCall asyncCall) {
    runningAsyncPrefetches.add(asyncCall);
  }

  /**
   * <p>Removes this asyncCall representing an asynchronous {@link ApolloPrefetch} from the underlying data structure
   * keeping track of the in progress asynchronous prefetch objects, if it is found, else throws an {@link
   * AssertionError}.</p> If the removal operation is successful and no active running calls are found, then the
   * registered {@link ApolloCallTracker#idleResourceCallback} is invoked.
   *
   * <p><b>Note</b>: This method needs to be called right after a prefetch call is completed (whether successful or
   * failed).</p>
   */
  void onAsyncPrefetchFinished(RealApolloPrefetch.AsyncCall asyncCall) {
    IdleResourceCallback idleResourceCallback;
    int runningCallsCount;
    synchronized (this) {
      if (!runningAsyncPrefetches.remove(asyncCall)) {
        throw new AssertionError("Prefetch call wasn't in progress");
      }
      runningCallsCount = activeCallsCount();
      idleResourceCallback = this.idleResourceCallback;
    }
    executeCallBackIfCallsAreFinished(runningCallsCount, idleResourceCallback);
  }

  /**
   * <p>Adds this {@link ApolloCall} to the underlying data structure keeping track of the in progress synchronous
   * apolloCall objects.</p>
   *
   * <p><b>Note</b>: This method needs to be called right before an apolloCall is executed.</p>
   */
  synchronized void onSyncCallInProgress(ApolloCall apolloCall) {
    runningSyncCalls.add(apolloCall);
  }

  /**
   * <p>Removes this {@link ApolloCall} from the underlying data structure keeping track of the in progress synchronous
   * apolloCall objects, if it is found, else throws an {@link AssertionError}.</p> If the removal operation is
   * successful and no active running calls are found, then the registered
   * {@link ApolloCallTracker#idleResourceCallback} is invoked.
   *
   * <p><b>Note</b>: This method needs to be called right after an apolloCall is completed (whether successful or
   * failed).</p>
   */
  void onSyncCallFinished(ApolloCall apolloCall) {
    IdleResourceCallback idleResourceCallback;
    int runningCallsCount;
    synchronized (this) {
      if (!runningSyncCalls.remove(apolloCall)) {
        throw new AssertionError("Call wasn't in progress");
      }
      runningCallsCount = activeCallsCount();
      idleResourceCallback = this.idleResourceCallback;
    }
    executeCallBackIfCallsAreFinished(runningCallsCount, idleResourceCallback);
  }

  /**
   * <p>Adds this asyncCall representing an asynchronous {@link ApolloCall} to the underlying data structure keeping
   * track of the in progress asynchronous apolloCall objects.</p>
   *
   * <p><b>Note</b>: This method needs to be called right before an apolloCall is executed.</p>
   */
  synchronized void onAsyncCallInProgress(RealApolloCall<?>.AsyncCall asyncCall) {
    runningAsyncCalls.add(asyncCall);
  }

  /**
   * <p>Removes this asyncCall representing an asynchronous {@link ApolloCall} from the underlying data structure
   * keeping track of the in progress asynchronous apolloCall objects, if it is found, else throws an {@link
   * AssertionError}.</p> If the removal operation is successful and no active running calls are found, then the
   * registered {@link ApolloCallTracker#idleResourceCallback} is invoked.
   *
   * <p><b>Note</b>: This method needs to be called right after an apolloCall is completed (whether successful or
   * failed).</p>
   */
  void onAsyncCallFinished(RealApolloCall<?>.AsyncCall asyncCall) {
    IdleResourceCallback idleResourceCallback;
    int runningCallsCount;
    synchronized (this) {
      if (!runningAsyncCalls.remove(asyncCall)) {
        throw new AssertionError("Call wasn't in progress");
      }
      runningCallsCount = activeCallsCount();
      idleResourceCallback = this.idleResourceCallback;
    }
    executeCallBackIfCallsAreFinished(runningCallsCount, idleResourceCallback);
  }

  /**
   * Registers idleResourceCallback which is invoked when the apolloClient becomes idle.
   */
  public synchronized void setIdleResourceCallback(IdleResourceCallback idleResourceCallback) {
    this.idleResourceCallback = idleResourceCallback;
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

  private void executeCallBackIfCallsAreFinished(int runningCallsCount, IdleResourceCallback idleResourceCallback) {
    if (runningCallsCount == 0 && idleResourceCallback != null) {
      idleResourceCallback.onIdle();
    }
  }
}
