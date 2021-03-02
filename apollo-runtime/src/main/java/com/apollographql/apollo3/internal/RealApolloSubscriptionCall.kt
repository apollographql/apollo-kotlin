package com.apollographql.apollo3.internal

import com.apollographql.apollo3.ApolloSubscriptionCall
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.api.internal.ApolloLogger
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.exception.ApolloCanceledException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.internal.CallState.IllegalStateMessage.Companion.forCurrentState
import com.apollographql.apollo3.internal.subscription.ApolloSubscriptionException
import com.apollographql.apollo3.internal.subscription.SubscriptionManager
import com.apollographql.apollo3.internal.subscription.SubscriptionResponse
import com.apollographql.apollo3.withCacheInfo
import com.benasher44.uuid.uuid4
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference

class RealApolloSubscriptionCall<D : Subscription.Data>(
    private val subscription: Subscription<D>,
    private val subscriptionManager: SubscriptionManager,
    private val apolloStore: ApolloStore,
    private val cachePolicy: ApolloSubscriptionCall.CachePolicy,
    private val dispatcher: Executor,
    private val logger: ApolloLogger) : ApolloSubscriptionCall<D> {
  private val state = AtomicReference(CallState.IDLE)
  private var subscriptionCallback: SubscriptionManagerCallback<D>? = null

  @Throws(ApolloCanceledException::class)
  override fun execute(callback: ApolloSubscriptionCall.Callback<D>) {
    synchronized(this) {
      when (state.get()) {
        CallState.IDLE -> {
          state.set(CallState.ACTIVE)
          if (cachePolicy == ApolloSubscriptionCall.CachePolicy.CACHE_AND_NETWORK) {
            dispatcher.execute {
              val cachedResponse = resolveFromCache()
              if (cachedResponse != null) {
                callback.onResponse(cachedResponse)
              }
            }
          }
          subscriptionCallback = SubscriptionManagerCallback(callback, this)
          subscriptionManager.subscribe(subscription, subscriptionCallback!!)
        }
        CallState.CANCELED -> throw ApolloCanceledException()
        CallState.TERMINATED, CallState.ACTIVE -> throw IllegalStateException("Already Executed")
        else -> throw IllegalStateException("Unknown state")
      }
    }
  }

  override fun cancel() {
    synchronized(this) {
      when (state.get()) {
        CallState.IDLE -> {
          state.set(CallState.CANCELED)
        }
        CallState.ACTIVE -> {
          try {
            subscriptionManager.unsubscribe(subscription)
          } finally {
            state.set(CallState.CANCELED)
            subscriptionCallback!!.release()
          }
        }
        CallState.CANCELED, CallState.TERMINATED -> {
        }
        else -> throw IllegalStateException("Unknown state")
      }
    }
  }

  override fun clone(): ApolloSubscriptionCall<D> {
    return RealApolloSubscriptionCall(subscription, subscriptionManager, apolloStore, cachePolicy, dispatcher, logger)
  }

  override val isCanceled: Boolean
    get() = state.get() === CallState.CANCELED

  override fun cachePolicy(cachePolicy: ApolloSubscriptionCall.CachePolicy): ApolloSubscriptionCall<D> {
    return RealApolloSubscriptionCall(subscription, subscriptionManager, apolloStore, cachePolicy, dispatcher, logger)
  }

  private fun terminate() {
    synchronized(this) {
      when (state.get()) {
        CallState.ACTIVE -> {
          state.set(CallState.TERMINATED)
          subscriptionCallback!!.release()
        }
        CallState.CANCELED -> {
        }
        CallState.IDLE, CallState.TERMINATED -> throw IllegalStateException(
            forCurrentState(state.get()).expected(CallState.ACTIVE, CallState.CANCELED))
        else -> throw IllegalStateException("Unknown state")
      }
    }
  }

  private fun resolveFromCache(): ApolloResponse<D>? {
    val data = apolloStore.readOperation(
        subscription,
        CacheHeaders.NONE)
    return if (data != null) {
      logger.d("Cache HIT for subscription `%s`", subscription)
      ApolloResponse(
          requestUuid = uuid4(),
          operation = subscription,
          data = data,
      ).withCacheInfo(true)
    } else {
      logger.d("Cache MISS for subscription `%s`", subscription)
      null
    }
  }

  private class SubscriptionManagerCallback<D : Subscription.Data>(private var originalCallback: ApolloSubscriptionCall.Callback<D>?, private var delegate: RealApolloSubscriptionCall<D>?) : SubscriptionManager.Callback<D> {
    override fun onResponse(response: SubscriptionResponse<D>) {
      val callback = originalCallback
      val data = response.response.data
      if (callback != null) {
        if (data != null && delegate!!.cachePolicy != ApolloSubscriptionCall.CachePolicy.NO_CACHE) {
          delegate!!.apolloStore.writeOperation(response.subscription, data)
        }
        callback.onResponse(response.response)
      }
    }

    override fun onError(error: ApolloSubscriptionException) {
      val callback = originalCallback
      callback?.onFailure(error)
      terminate()
    }

    override fun onNetworkError(t: Throwable) {
      val callback = originalCallback
      callback?.onFailure(ApolloNetworkException("Subscription failed", t))
      terminate()
    }

    override fun onCompleted() {
      val callback = originalCallback
      callback?.onCompleted()
      terminate()
    }

    override fun onTerminated() {
      val callback = originalCallback
      callback?.onTerminated()
      terminate()
    }

    override fun onConnected() {
      val callback = originalCallback
      callback?.onConnected()
    }

    fun terminate() {
      val delegate = delegate
      delegate?.terminate()
    }

    fun release() {
      originalCallback = null
      delegate = null
    }
  }
}