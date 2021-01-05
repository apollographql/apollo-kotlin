package com.apollographql.apollo.internal

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloMutationCall
import com.apollographql.apollo.ApolloPrefetch
import com.apollographql.apollo.ApolloQueryCall
import com.apollographql.apollo.ApolloQueryWatcher
import com.apollographql.apollo.IdleResourceCallback
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.internal.Utils.__checkNotNull
import java.util.HashMap
import java.util.HashSet
import java.util.concurrent.atomic.AtomicInteger

/**
 * ApolloCallTracker is responsible for keeping track of running [ApolloPrefetch] & [ApolloQueryCall]
 * & [ApolloMutationCall] & [ApolloQueryWatcher] calls.
 */
class ApolloCallTracker {
  private val activePrefetchCalls: MutableMap<OperationName, MutableSet<ApolloPrefetch>> = HashMap()
  private val activeQueryCalls: MutableMap<OperationName, MutableSet<ApolloQueryCall<*>>> = HashMap()
  private val activeMutationCalls: MutableMap<OperationName, MutableSet<ApolloMutationCall<*>>> = HashMap()
  private val activeQueryWatchers: MutableMap<OperationName, MutableSet<ApolloQueryWatcher<*>>> = HashMap()
  private val activeCallCount = AtomicInteger()
  private var idleResourceCallback: IdleResourceCallback? = null

  /**
   *
   * Adds provided [ApolloCall] that is currently in progress.
   *
   *
   * **Note**: This method needs to be called right before an apolloCall is executed.
   */
  fun registerCall(call: ApolloCall<*>) {
    __checkNotNull(call, "call == null")
    val operation = call.operation()
    if (operation is Query<*>) {
      registerQueryCall(call as ApolloQueryCall<*>)
    } else if (operation is Mutation<*>) {
      registerMutationCall(call as ApolloMutationCall<*>)
    } else {
      throw IllegalArgumentException("Unknown call type")
    }
  }

  /**
   *
   * Removes provided [ApolloCall] that finished his execution, if it is found, else throws an
   * [AssertionError].
   *
   *
   * If the removal operation is successful and no active running calls are found, then the registered
   * [ApolloCallTracker.idleResourceCallback] is invoked.
   *
   *
   * **Note**: This method needs to be called right after an apolloCall is completed (whether successful or
   * failed).
   */
  fun unregisterCall(call: ApolloCall<*>) {
    __checkNotNull(call, "call == null")
    val operation = call.operation()
    if (operation is Query<*>) {
      unregisterQueryCall(call as ApolloQueryCall<*>)
    } else if (operation is Mutation<*>) {
      unregisterMutationCall(call as ApolloMutationCall<*>)
    } else {
      throw IllegalArgumentException("Unknown call type")
    }
  }

  /**
   *
   * Adds provided [ApolloPrefetch] that is currently in progress.
   *
   *
   * **Note**: This method needs to be called right before a prefetch call is executed.
   */
  fun registerPrefetchCall(apolloPrefetch: ApolloPrefetch) {
    __checkNotNull(apolloPrefetch, "apolloPrefetch == null")
    val operationName = apolloPrefetch.operation().name()
    registerCall(activePrefetchCalls, operationName, apolloPrefetch)
    activeCallCount.incrementAndGet()
  }

  /**
   *
   * Removes provided [ApolloPrefetch] that finished his execution, if it is found, else throws an
   * [AssertionError].
   *
   *
   * If the removal operation is successful and no active running calls are found, then the registered
   * [ApolloCallTracker.idleResourceCallback] is invoked.
   *
   *
   * **Note**: This method needs to be called right after a prefetch call is completed (whether successful or
   * failed).
   */
  fun unregisterPrefetchCall(apolloPrefetch: ApolloPrefetch) {
    __checkNotNull(apolloPrefetch, "apolloPrefetch == null")
    val operationName = apolloPrefetch.operation().name()
    unregisterCall(activePrefetchCalls, operationName, apolloPrefetch)
    decrementActiveCallCountAndNotify()
  }

  /**
   * Returns currently active [ApolloPrefetch] calls by operation name.
   *
   * @param operationName prefetch operation name
   * @return set of active prefetch calls
   */
  fun activePrefetchCalls(operationName: OperationName): Set<ApolloPrefetch> {
    return activeCalls(activePrefetchCalls, operationName)
  }

  /**
   *
   * Adds provided [ApolloQueryCall] that is currently in progress.
   *
   *
   * **Note**: This method needs to be called right before an apolloCall is executed.
   */
  fun registerQueryCall(apolloQueryCall: ApolloQueryCall<*>) {
    __checkNotNull(apolloQueryCall, "apolloQueryCall == null")
    val operationName = apolloQueryCall.operation().name()
    registerCall(activeQueryCalls, operationName, apolloQueryCall)
    activeCallCount.incrementAndGet()
  }

  /**
   *
   * Removes provided [ApolloQueryCall] that finished his execution, if it is found, else throws an
   * [AssertionError].
   *
   *
   * If the removal operation is successful and no active running calls are found, then the registered
   * [ApolloCallTracker.idleResourceCallback] is invoked.
   *
   *
   * **Note**: This method needs to be called right after an apolloCall is completed (whether successful or
   * failed).
   */
  fun unregisterQueryCall(apolloQueryCall: ApolloQueryCall<*>) {
    __checkNotNull(apolloQueryCall, "apolloQueryCall == null")
    val operationName = apolloQueryCall.operation().name()
    unregisterCall(activeQueryCalls, operationName, apolloQueryCall)
    decrementActiveCallCountAndNotify()
  }

  /**
   * Returns currently active [ApolloQueryCall] calls by operation name.
   *
   * @param operationName query operation name
   * @return set of active query calls
   */
  fun activeQueryCalls(operationName: OperationName): Set<ApolloQueryCall<*>> {
    return activeCalls(activeQueryCalls, operationName)
  }

  /**
   *
   * Adds provided [ApolloMutationCall] that is currently in progress.
   *
   *
   * **Note**: This method needs to be called right before an apolloCall is executed.
   */
  fun registerMutationCall(apolloMutationCall: ApolloMutationCall<*>) {
    __checkNotNull(apolloMutationCall, "apolloMutationCall == null")
    val operationName = apolloMutationCall.operation().name()
    registerCall(activeMutationCalls, operationName, apolloMutationCall)
    activeCallCount.incrementAndGet()
  }

  /**
   *
   * Removes provided [ApolloMutationCall] that finished his execution, if it is found, else throws an
   * [AssertionError].
   *
   *
   * If the removal operation is successful and no active running calls are found, then the registered
   * [ApolloCallTracker.idleResourceCallback] is invoked.
   *
   *
   * **Note**: This method needs to be called right after an apolloCall is completed (whether successful or
   * failed).
   */
  fun unregisterMutationCall(apolloMutationCall: ApolloMutationCall<*>) {
    __checkNotNull(apolloMutationCall, "apolloMutationCall == null")
    val operationName = apolloMutationCall.operation().name()
    unregisterCall(activeMutationCalls, operationName, apolloMutationCall)
    decrementActiveCallCountAndNotify()
  }

  /**
   * Returns currently active [ApolloMutationCall] calls by operation name.
   *
   * @param operationName query operation name
   * @return set of active mutation calls
   */
  fun activeMutationCalls(operationName: OperationName): Set<ApolloMutationCall<*>> {
    return activeCalls(activeMutationCalls, operationName)
  }

  /**
   *
   * Adds provided [ApolloQueryWatcher] that is currently in progress.
   *
   *
   * **Note**: This method needs to be called right before
   * [ApolloQueryWatcher.enqueueAndWatch].
   */
  fun registerQueryWatcher(queryWatcher: ApolloQueryWatcher<*>) {
    __checkNotNull(queryWatcher, "queryWatcher == null")
    val operationName = queryWatcher.operation().name()
    registerCall(activeQueryWatchers, operationName, queryWatcher)
  }

  /**
   *
   * Removes provided [ApolloQueryWatcher] that finished his execution, if it is found, else throws an
   * [AssertionError].
   *
   *
   * If the removal operation is successful and no active running calls are found, then the registered
   * [ApolloCallTracker.idleResourceCallback] is invoked.
   *
   *
   * **Note**: This method needs to be called right after an apolloCall is completed (whether successful or
   * failed).
   */
  fun unregisterQueryWatcher(queryWatcher: ApolloQueryWatcher<*>) {
    __checkNotNull(queryWatcher, "queryWatcher == null")
    val operationName = queryWatcher.operation().name()
    unregisterCall(activeQueryWatchers, operationName, queryWatcher)
  }

  /**
   * Returns currently active [ApolloQueryWatcher] query watchers by operation name.
   *
   * @param operationName query watcher operation name
   * @return set of active query watchers
   */
  fun activeQueryWatchers(operationName: OperationName): Set<ApolloQueryWatcher<*>> {
    return activeCalls(activeQueryWatchers, operationName)
  }

  /**
   * Registers idleResourceCallback which is invoked when the apolloClient becomes idle.
   */
  @Synchronized
  fun setIdleResourceCallback(idleResourceCallback: IdleResourceCallback?) {
    this.idleResourceCallback = idleResourceCallback
  }

  /**
   * Returns a total count of in progress [ApolloCall] & [ApolloPrefetch] objects.
   */
  fun activeCallsCount(): Int {
    return activeCallCount.get()
  }

  private fun <CALL> registerCall(registry: MutableMap<OperationName, MutableSet<CALL>>, operationName: OperationName, call: CALL) {
    synchronized(registry) {
      var calls = registry[operationName]
      if (calls == null) {
        calls = HashSet()
        registry[operationName] = calls
      }
      calls.add(call)
    }
  }

  private fun <CALL> unregisterCall(registry: MutableMap<OperationName, MutableSet<CALL>>, operationName: OperationName, call: CALL) {
    synchronized(registry) {
      val calls = registry[operationName]
      if (calls == null || !calls.remove(call)) {
        throw AssertionError("Call wasn't registered before")
      }
      if (calls.isEmpty()) {
        registry.remove(operationName)
      }
    }
  }

  private fun <CALL> activeCalls(registry: Map<OperationName, MutableSet<CALL>>, operationName: OperationName): Set<CALL> {
    __checkNotNull(operationName, "operationName == null")
    synchronized(registry) {
      val calls: Set<CALL>? = registry[operationName]
      return if (calls != null) HashSet(calls) else emptySet()
    }
  }

  private fun decrementActiveCallCountAndNotify() {
    if (activeCallCount.decrementAndGet() == 0) {
      val callback = idleResourceCallback
      callback?.onIdle()
    }
  }
}