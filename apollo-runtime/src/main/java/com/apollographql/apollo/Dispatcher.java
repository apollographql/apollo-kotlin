package com.apollographql.apollo;

import com.apollographql.apollo.internal.RealApolloCall;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutorService;

public class Dispatcher {

  private Runnable idleRunnable;

  private final Deque<RealApolloCall> runningSyncCalls = new ArrayDeque<>();

  private ExecutorService executorService;

  public Dispatcher(ExecutorService executorService) {
    this.executorService = executorService;
  }

  public synchronized void executed(RealApolloCall realApolloCall) {
    runningSyncCalls.add(realApolloCall);
  }


  public <T> void finished(RealApolloCall<T> tRealApolloCall) {
    int runningCallsCount;
    Runnable runnable;
    synchronized (this) {
      if (!runningSyncCalls.remove(tRealApolloCall)) {
        throw new AssertionError("Call wasn't inflight");
      }

      runningCallsCount = getRunningCallsCount();
      runnable = idleRunnable;
    }

    if (runningCallsCount == 0 && runnable != null) {
      runnable.run();
    }

  }

  private int getRunningCallsCount() {
    return runningSyncCalls.size();
  }

  public void execute(Runnable runnable) {

  }
}
