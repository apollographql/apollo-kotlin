package com.apollographql.apollo3.internal

import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.ApolloQueryWatcher
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.internal.ApolloLogger
import com.apollographql.apollo3.api.internal.Optional
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.ApolloStore.RecordChangeSubscriber
import com.apollographql.apollo3.cache.normalized.Record
import com.apollographql.apollo3.cache.normalized.internal.dependentKeys
import com.apollographql.apollo3.exception.ApolloCanceledException
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.exception.ApolloParseException
import com.apollographql.apollo3.fetcher.ResponseFetcher
import com.apollographql.apollo3.internal.CallState.IllegalStateMessage.Companion.forCurrentState
import java.util.concurrent.atomic.AtomicReference

class RealApolloQueryWatcher<D : Operation.Data>(
    private var activeCall: RealApolloCall<D>,
    val apolloStore: ApolloStore,
    private val responseAdapterCache: ResponseAdapterCache,
    val logger: ApolloLogger,
    private val tracker: ApolloCallTracker,
    private var refetchResponseFetcher: ResponseFetcher
) : ApolloQueryWatcher<D> {
  var dependentKeys = emptySet<String>()
  val recordChangeSubscriber: RecordChangeSubscriber = object : RecordChangeSubscriber {
    override fun onCacheRecordsChanged(changedRecordKeys: Set<String>) {
      if (dependentKeys.isEmpty() || !areDisjoint(dependentKeys, changedRecordKeys)) {
        refetch()
      }
    }
  }
  private val state = AtomicReference(CallState.IDLE)
  private val originalCallback = AtomicReference<ApolloCall.Callback<D>?>()
  override fun enqueueAndWatch(callback: ApolloCall.Callback<D>): ApolloQueryWatcher<D> {
    try {
      activate(Optional.fromNullable(callback))
    } catch (e: ApolloCanceledException) {
      callback.onCanceledError(e)
      return this
    }
    activeCall.enqueue(callbackProxy())
    return this
  }

  @Synchronized
  override fun refetchResponseFetcher(fetcher: ResponseFetcher): RealApolloQueryWatcher<D> {
    check(!(state.get() !== CallState.IDLE)) { "Already Executed" }
    refetchResponseFetcher = fetcher
    return this
  }

  @Synchronized
  override fun cancel() {
    when (state.get()) {
      CallState.ACTIVE -> try {
        activeCall.cancel()
        apolloStore.unsubscribe(recordChangeSubscriber)
      } finally {
        tracker.unregisterQueryWatcher(this)
        originalCallback.set(null)
        state.set(CallState.CANCELED)
      }
      CallState.IDLE -> state.set(CallState.CANCELED)
      CallState.CANCELED, CallState.TERMINATED -> {
      }
      else -> throw IllegalStateException("Unknown state")
    }
  }

  override val isCanceled: Boolean
    get() = state.get() == CallState.CANCELED

  override fun operation(): Operation<*> {
    return activeCall.operation()
  }

  @Synchronized
  override fun refetch() {
    when (state.get()) {
      CallState.ACTIVE -> {
        apolloStore.unsubscribe(recordChangeSubscriber)
        activeCall.cancel()
        activeCall = activeCall.clone().responseFetcher(refetchResponseFetcher)
        activeCall.enqueue(callbackProxy())
      }
      CallState.IDLE -> throw IllegalStateException("Cannot refetch a watcher which has not first called enqueueAndWatch.")
      CallState.CANCELED -> throw IllegalStateException("Cannot refetch a canceled watcher,")
      CallState.TERMINATED -> throw IllegalStateException("Cannot refetch a watcher which has experienced an error.")
      else -> throw IllegalStateException("Unknown state")
    }
  }

  override fun clone(): ApolloQueryWatcher<D> {
    return RealApolloQueryWatcher(activeCall.clone(), apolloStore, responseAdapterCache, logger, tracker, refetchResponseFetcher)
  }

  private fun callbackProxy(): ApolloCall.Callback<D> {
    return object : ApolloCall.Callback<D>() {
      override fun onResponse(response: ApolloResponse<D>) {
        val callback = responseCallback()
        if (!callback.isPresent) {
          logger.d("onResponse for watched operation: %s. No callback present.", operation().name())
          return
        }
        apolloStore.subscribe(recordChangeSubscriber)
        callback.get().onResponse(response)
      }

      override fun onCached(records: List<Record>) {
        val callback = responseCallback()
        if (!callback.isPresent) {
          logger.d("onCached for watched operation: %s. No callback present.", operation().name())
          return
        }
        dependentKeys = records.dependentKeys()
      }

      override fun onFailure(e: ApolloException) {
        val callback = terminate()
        if (!callback.isPresent) {
          logger.d(e, "onFailure for operation: %s. No callback present.", operation().name())
          return
        }
        if (e is ApolloHttpException) {
          callback.get().onHttpError(e)
        } else if (e is ApolloParseException) {
          callback.get().onParseError(e)
        } else if (e is ApolloNetworkException) {
          callback.get().onNetworkError(e)
        } else {
          callback.get().onFailure(e)
        }
      }

      override fun onStatusEvent(event: ApolloCall.StatusEvent) {
        val callback = originalCallback.get()
        if (callback == null) {
          logger.d("onStatusEvent for operation: %s. No callback present.", operation().name())
          return
        }
        callback.onStatusEvent(event)
      }
    }
  }

  @Synchronized
  @Throws(ApolloCanceledException::class)
  private fun activate(callback: Optional<ApolloCall.Callback<D>>) {
    when (state.get()) {
      CallState.IDLE -> {
        originalCallback.set(callback.orNull())
        tracker.registerQueryWatcher(this)
      }
      CallState.CANCELED -> throw ApolloCanceledException()
      CallState.TERMINATED, CallState.ACTIVE -> throw IllegalStateException("Already Executed")
      else -> throw IllegalStateException("Unknown state")
    }
    state.set(CallState.ACTIVE)
  }

  @Synchronized
  fun responseCallback(): Optional<ApolloCall.Callback<D>> {
    return when (state.get()) {
      CallState.ACTIVE, CallState.CANCELED -> Optional.fromNullable(originalCallback.get())
      CallState.IDLE, CallState.TERMINATED -> throw IllegalStateException(
          forCurrentState(state.get()).expected(CallState.ACTIVE, CallState.CANCELED))
      else -> throw IllegalStateException("Unknown state")
    }
  }

  @Synchronized
  fun terminate(): Optional<ApolloCall.Callback<D>> {
    return when (state.get()) {
      CallState.ACTIVE -> {
        tracker.unregisterQueryWatcher(this)
        state.set(CallState.TERMINATED)
        Optional.fromNullable(originalCallback.getAndSet(null))
      }
      CallState.CANCELED -> Optional.fromNullable(originalCallback.getAndSet(null))
      CallState.IDLE, CallState.TERMINATED -> throw IllegalStateException(
          forCurrentState(state.get()).expected(CallState.ACTIVE, CallState.CANCELED))
      else -> throw IllegalStateException("Unknown state")
    }
  }

  companion object {
    /**
     * Checks if two [Set] are disjoint. Returns true if the sets don't have a single common element. Also returns
     * true if either of the sets is null.
     *
     * @param setOne the first set
     * @param setTwo the second set
     * @param <E> the value type contained within the sets
     * @return True if the sets don't have a single common element or if either of the sets is null.
    </E> */
    private fun <E> areDisjoint(setOne: Set<E>?, setTwo: Set<E>?): Boolean {
      if (setOne == null || setTwo == null) {
        return true
      }
      var smallerSet: Set<E> = setOne
      var largerSet: Set<E> = setTwo
      if (setOne.size > setTwo.size) {
        smallerSet = setTwo
        largerSet = setOne
      }
      for (el in smallerSet) {
        if (largerSet.contains(el)) {
          return false
        }
      }
      return true
    }
  }
}
