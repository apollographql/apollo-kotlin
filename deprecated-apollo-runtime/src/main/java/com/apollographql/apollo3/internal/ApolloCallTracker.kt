package com.apollographql.apollo3.internal

import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.ApolloMutationCall
import com.apollographql.apollo3.ApolloPrefetch
import com.apollographql.apollo3.ApolloQueryCall
import com.apollographql.apollo3.ApolloQueryWatcher
import com.apollographql.apollo3.IdleResourceCallback
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Query
import java.util.HashMap
import java.util.HashSet
import java.util.concurrent.atomic.AtomicInteger

/**
 * ApolloCallTracker is responsible for keeping track of running [ApolloPrefetch] & [ApolloQueryCall]
 * & [ApolloMutationCall] & [ApolloQueryWatcher] calls.
 */
class ApolloCallTracker {
  private val activePrefetchCalls: MutableMap<String, MutableSet<ApolloPrefetch>> = HashMap()
  private val activeQueryCalls: MutableMap<String, MutableSet<ApolloQueryCall<*>>> = HashMap()
  private val activeMutationCalls: MutableMap<String, MutableSet<ApolloMutationCall<*>>> = HashMap()
  private val activeQueryWatchers: MutableMap<String, MutableSet<ApolloQueryWatcher<*>>> = HashMap()
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
  fun activePrefetchCalls(operationName: String): Set<ApolloPrefetch> {
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
  fun activeQueryCalls(operationName: String): Set<ApolloQueryCall<*>> {
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
  fun activeMutationCalls(operationName: String): Set<ApolloMutationCall<*>> {
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
    val operationName = queryWatcher.operation().name()
    unregisterCall(activeQueryWatchers, operationName, queryWatcher)
  }

  /**
   * Returns currently active [ApolloQueryWatcher] query watchers by operation name.
   *
   * @param operationName query watcher operation name
   * @return set of active query watchers
   */
  fun activeQueryWatchers(operationName: String): Set<ApolloQueryWatcher<*>> {
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

  private fun <CALL> registerCall(registry: MutableMap<String, MutableSet<CALL>>, operationName: String, call: CALL) {
    synchronized(registry) {
      var calls = registry[operationName]
      if (calls == null) {
        calls = HashSet()
        registry[operationName] = calls
      }
      calls.add(call)
    }
  }

  private fun <CALL> unregisterCall(registry: MutableMap<String, MutableSet<CALL>>, operationName: String, call: CALL) {
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

  private fun <CALL> activeCalls(registry: Map<String, MutableSet<CALL>>, operationName: String): Set<CALL> {
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
