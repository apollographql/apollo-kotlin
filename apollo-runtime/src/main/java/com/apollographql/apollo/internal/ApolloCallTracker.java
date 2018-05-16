package com.apollographql.apollo.internal;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloMutationCall;
import com.apollographql.apollo.ApolloPrefetch;
import com.apollographql.apollo.ApolloQueryCall;
import com.apollographql.apollo.ApolloQueryWatcher;
import com.apollographql.apollo.IdleResourceCallback;
import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.NotNull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * ApolloCallTracker is responsible for keeping track of running {@link ApolloPrefetch} & {@link ApolloQueryCall}
 * & {@link ApolloMutationCall} & {@link ApolloQueryWatcher} calls.
 */
@SuppressWarnings("WeakerAccess") public final class ApolloCallTracker {
  private final Map<OperationName, Set<ApolloPrefetch>> activePrefetchCalls = new HashMap<>();
  private final Map<OperationName, Set<ApolloQueryCall>> activeQueryCalls = new HashMap<>();
  private final Map<OperationName, Set<ApolloMutationCall>> activeMutationCalls = new HashMap<>();
  private final Map<OperationName, Set<ApolloQueryWatcher>> activeQueryWatchers = new HashMap<>();
  private final AtomicInteger activeCallCount = new AtomicInteger();

  private IdleResourceCallback idleResourceCallback;

  public ApolloCallTracker() {
  }

  /**
   * <p>Adds provided {@link ApolloCall} that is currently in progress.</p>
   *
   * <p><b>Note</b>: This method needs to be called right before an apolloCall is executed.</p>
   */
  void registerCall(@NotNull ApolloCall call) {
    checkNotNull(call, "call == null");
    Operation operation = call.operation();
    if (operation instanceof Query) {
      registerQueryCall((ApolloQueryCall) call);
    } else if (operation instanceof Mutation) {
      registerMutationCall((ApolloMutationCall) call);
    } else {
      throw new IllegalArgumentException("Unknown call type");
    }
  }

  /**
   * <p>Removes provided {@link ApolloCall} that finished his execution, if it is found, else throws an
   * {@link AssertionError}.</p>
   *
   * If the removal operation is successful and no active running calls are found, then the registered
   * {@link ApolloCallTracker#idleResourceCallback} is invoked.
   *
   * <p><b>Note</b>: This method needs to be called right after an apolloCall is completed (whether successful or
   * failed).</p>
   */
  void unregisterCall(@NotNull ApolloCall call) {
    checkNotNull(call, "call == null");
    Operation operation = call.operation();
    if (operation instanceof Query) {
      unregisterQueryCall((ApolloQueryCall) call);
    } else if (operation instanceof Mutation) {
      unregisterMutationCall((ApolloMutationCall) call);
    } else {
      throw new IllegalArgumentException("Unknown call type");
    }
  }

  /**
   * <p>Adds provided {@link ApolloPrefetch} that is currently in progress.</p>
   *
   * <p><b>Note</b>: This method needs to be called right before a prefetch call is executed.</p>
   */
  void registerPrefetchCall(@NotNull ApolloPrefetch apolloPrefetch) {
    checkNotNull(apolloPrefetch, "apolloPrefetch == null");
    OperationName operationName = apolloPrefetch.operation().name();
    registerCall(activePrefetchCalls, operationName, apolloPrefetch);
  }

  /**
   * <p>Removes provided {@link ApolloPrefetch} that finished his execution, if it is found, else throws an
   * {@link AssertionError}.</p>
   *
   * If the removal operation is successful and no active running calls are found, then the registered
   * {@link ApolloCallTracker#idleResourceCallback} is invoked.
   *
   * <p><b>Note</b>: This method needs to be called right after a prefetch call is completed (whether successful or
   * failed).</p>
   */
  void unregisterPrefetchCall(@NotNull ApolloPrefetch apolloPrefetch) {
    checkNotNull(apolloPrefetch, "apolloPrefetch == null");
    OperationName operationName = apolloPrefetch.operation().name();
    unregisterCall(activePrefetchCalls, operationName, apolloPrefetch);
  }

  /**
   * Returns currently active {@link ApolloPrefetch} calls by operation name.
   *
   * @param operationName prefetch operation name
   * @return set of active prefetch calls
   */
  @NotNull Set<ApolloPrefetch> activePrefetchCalls(@NotNull OperationName operationName) {
    return activeCalls(activePrefetchCalls, operationName);
  }

  /**
   * <p>Adds provided {@link ApolloQueryCall} that is currently in progress.</p>
   *
   * <p><b>Note</b>: This method needs to be called right before an apolloCall is executed.</p>
   */
  void registerQueryCall(@NotNull ApolloQueryCall apolloQueryCall) {
    checkNotNull(apolloQueryCall, "apolloQueryCall == null");
    OperationName operationName = apolloQueryCall.operation().name();
    registerCall(activeQueryCalls, operationName, apolloQueryCall);
  }

  /**
   * <p>Removes provided {@link ApolloQueryCall} that finished his execution, if it is found, else throws an
   * {@link AssertionError}.</p>
   *
   * If the removal operation is successful and no active running calls are found, then the registered
   * {@link ApolloCallTracker#idleResourceCallback} is invoked.
   *
   * <p><b>Note</b>: This method needs to be called right after an apolloCall is completed (whether successful or
   * failed).</p>
   */
  void unregisterQueryCall(@NotNull ApolloQueryCall apolloQueryCall) {
    checkNotNull(apolloQueryCall, "apolloQueryCall == null");
    OperationName operationName = apolloQueryCall.operation().name();
    unregisterCall(activeQueryCalls, operationName, apolloQueryCall);
  }

  /**
   * Returns currently active {@link ApolloQueryCall} calls by operation name.
   *
   * @param operationName query operation name
   * @return set of active query calls
   */
  @NotNull Set<ApolloQueryCall> activeQueryCalls(@NotNull OperationName operationName) {
    return activeCalls(activeQueryCalls, operationName);
  }

  /**
   * <p>Adds provided {@link ApolloMutationCall} that is currently in progress.</p>
   *
   * <p><b>Note</b>: This method needs to be called right before an apolloCall is executed.</p>
   */
  void registerMutationCall(@NotNull ApolloMutationCall apolloMutationCall) {
    checkNotNull(apolloMutationCall, "apolloMutationCall == null");
    OperationName operationName = apolloMutationCall.operation().name();
    registerCall(activeMutationCalls, operationName, apolloMutationCall);
  }

  /**
   * <p>Removes provided {@link ApolloMutationCall} that finished his execution, if it is found, else throws an
   * {@link AssertionError}.</p>
   *
   * If the removal operation is successful and no active running calls are found, then the registered
   * {@link ApolloCallTracker#idleResourceCallback} is invoked.
   *
   * <p><b>Note</b>: This method needs to be called right after an apolloCall is completed (whether successful or
   * failed).</p>
   */
  void unregisterMutationCall(@NotNull ApolloMutationCall apolloMutationCall) {
    checkNotNull(apolloMutationCall, "apolloMutationCall == null");
    OperationName operationName = apolloMutationCall.operation().name();
    unregisterCall(activeMutationCalls, operationName, apolloMutationCall);
  }

  /**
   * Returns currently active {@link ApolloMutationCall} calls by operation name.
   *
   * @param operationName query operation name
   * @return set of active mutation calls
   */
  @NotNull Set<ApolloMutationCall> activeMutationCalls(@NotNull OperationName operationName) {
    return activeCalls(activeMutationCalls, operationName);
  }

  /**
   * <p>Adds provided {@link ApolloQueryWatcher} that is currently in progress.</p>
   *
   * <p><b>Note</b>: This method needs to be called right before
   * {@link ApolloQueryWatcher#enqueueAndWatch(ApolloCall.Callback)}.</p>
   */
  void registerQueryWatcher(@NotNull ApolloQueryWatcher queryWatcher) {
    checkNotNull(queryWatcher, "queryWatcher == null");
    OperationName operationName = queryWatcher.operation().name();
    registerCall(activeQueryWatchers, operationName, queryWatcher);
  }

  /**
   * <p>Removes provided {@link ApolloQueryWatcher} that finished his execution, if it is found, else throws an
   * {@link AssertionError}.</p>
   *
   * If the removal operation is successful and no active running calls are found, then the registered
   * {@link ApolloCallTracker#idleResourceCallback} is invoked.
   *
   * <p><b>Note</b>: This method needs to be called right after an apolloCall is completed (whether successful or
   * failed).</p>
   */
  void unregisterQueryWatcher(@NotNull ApolloQueryWatcher queryWatcher) {
    checkNotNull(queryWatcher, "queryWatcher == null");
    OperationName operationName = queryWatcher.operation().name();
    unregisterCall(activeQueryWatchers, operationName, queryWatcher);
  }

  /**
   * Returns currently active {@link ApolloQueryWatcher} query watchers by operation name.
   *
   * @param operationName query watcher operation name
   * @return set of active query watchers
   */
  @NotNull Set<ApolloQueryWatcher> activeQueryWatchers(@NotNull OperationName operationName) {
    return activeCalls(activeQueryWatchers, operationName);
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
    return activeCallCount.get();
  }

  private <CALL> void registerCall(Map<OperationName, Set<CALL>> registry, OperationName operationName, CALL call) {
    //noinspection SynchronizationOnLocalVariableOrMethodParameter
    synchronized (registry) {
      Set<CALL> calls = registry.get(operationName);
      if (calls == null) {
        calls = new HashSet<>();
        registry.put(operationName, calls);
      }
      calls.add(call);
    }
    activeCallCount.incrementAndGet();
  }

  private <CALL> void unregisterCall(Map<OperationName, Set<CALL>> registry, OperationName operationName, CALL call) {
    //noinspection SynchronizationOnLocalVariableOrMethodParameter
    synchronized (registry) {
      Set<CALL> calls = registry.get(operationName);
      if (calls == null || !calls.remove(call)) {
        throw new AssertionError("Call wasn't registered before");
      }

      if (calls.isEmpty()) {
        registry.remove(operationName);
      }
    }

    if (activeCallCount.decrementAndGet() == 0) {
      notifyIdleResource();
    }
  }

  private <CALL> Set<CALL> activeCalls(Map<OperationName, Set<CALL>> registry, @NotNull OperationName operationName) {
    checkNotNull(operationName, "operationName == null");

    //noinspection SynchronizationOnLocalVariableOrMethodParameter
    synchronized (registry) {
      Set<CALL> calls = registry.get(operationName);
      return calls != null ? new HashSet<>(calls) : Collections.<CALL>emptySet();
    }
  }

  private void notifyIdleResource() {
    IdleResourceCallback callback = idleResourceCallback;
    if (callback != null) {
      callback.onIdle();
    }
  }
}
