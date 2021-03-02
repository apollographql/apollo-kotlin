package com.apollographql.apollo3.internal.subscription

import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.api.internal.MapResponseParser
import com.apollographql.apollo3.cache.normalized.CacheKeyResolver
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.subscription.OnSubscriptionManagerStateChangeListener
import com.apollographql.apollo3.subscription.OperationClientMessage
import com.apollographql.apollo3.subscription.OperationServerMessage
import com.apollographql.apollo3.subscription.SubscriptionConnectionParamsProvider
import com.apollographql.apollo3.subscription.SubscriptionManagerState
import com.apollographql.apollo3.subscription.SubscriptionTransport
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class RealSubscriptionManager(private val responseAdapterCache: ResponseAdapterCache,
                              transportFactory: SubscriptionTransport.Factory,
                              private val connectionParams: SubscriptionConnectionParamsProvider,
                              private val dispatcher: Executor,
                              private val connectionHeartbeatTimeoutMs: Long,
                              private val cacheKeyResolver: CacheKeyResolver,
                              private val autoPersistSubscription: Boolean
) : SubscriptionManager {
  @JvmField
  var subscriptions: MutableMap<UUID, SubscriptionRecord> = LinkedHashMap()

  @Volatile
  override var state: SubscriptionManagerState = SubscriptionManagerState.DISCONNECTED

  val timer = AutoReleaseTimer()
  private val transport: SubscriptionTransport = transportFactory.create(SubscriptionTransportCallback(this, dispatcher))
  private val connectionAcknowledgeTimeoutTimerTask = Runnable { onConnectionAcknowledgeTimeout() }
  private val inactivityTimeoutTimerTask = Runnable { onInactivityTimeout() }
  private val connectionHeartbeatTimeoutTimerTask = Runnable { onConnectionHeartbeatTimeout() }
  private val onStateChangeListeners: MutableList<OnSubscriptionManagerStateChangeListener> = CopyOnWriteArrayList()

  override fun <D : Subscription.Data> subscribe(subscription: Subscription<D>, callback: SubscriptionManager.Callback<D>) {
    dispatcher.execute { doSubscribe(subscription, callback) }
  }

  override fun unsubscribe(subscription: Subscription<*>) {
    dispatcher.execute { doUnsubscribe(subscription) }
  }

  /**
   * Set the [RealSubscriptionManager] to a connectible state. It is safe to call this method
   * at any time.  Does nothing unless we are in the stopped state.
   */
  override fun start() {
    var oldState: SubscriptionManagerState
    synchronized(this) {
      oldState = state
      if (state == SubscriptionManagerState.STOPPED) {
        state = SubscriptionManagerState.DISCONNECTED
      }
    }
    notifyStateChanged(oldState, state)
  }

  /**
   * Unsubscribe from all active subscriptions, and disconnect the web socket.  It will not be
   * possible to add new subscriptions while the [SubscriptionManager] is stopping
   * because we check the state in [.doSubscribe].  We pass true to
   * [.disconnect] because we want to disconnect even if, somehow, a new subscription
   * is added while or after we are doing the [.doUnsubscribe] loop.
   */
  override fun stop() {
    dispatcher.execute { doStop() }
  }

  override fun addOnStateChangeListener(onStateChangeListener: OnSubscriptionManagerStateChangeListener) {
    onStateChangeListeners.add(onStateChangeListener)
  }

  override fun removeOnStateChangeListener(onStateChangeListener: OnSubscriptionManagerStateChangeListener) {
    onStateChangeListeners.remove(onStateChangeListener)
  }

  fun doSubscribe(subscription: Subscription<*>, callback: SubscriptionManager.Callback<*>) {
    var oldState: SubscriptionManagerState
    synchronized(this) {
      oldState = state
      if (state != SubscriptionManagerState.STOPPING && state != SubscriptionManagerState.STOPPED) {
        timer.cancelTask(INACTIVITY_TIMEOUT_TIMER_TASK_ID)
        val subscriptionId = UUID.randomUUID()
        subscriptions[subscriptionId] = SubscriptionRecord(subscriptionId, subscription as Subscription<Subscription.Data>, callback as SubscriptionManager.Callback<Subscription.Data>)
        if (state == SubscriptionManagerState.DISCONNECTED) {
          state = SubscriptionManagerState.CONNECTING
          transport.connect()
        } else if (state == SubscriptionManagerState.ACTIVE) {
          transport.send(
              OperationClientMessage.Start(subscriptionId.toString(), subscription, responseAdapterCache, autoPersistSubscription, false)
          )
        }
      }
    }
    if (oldState == SubscriptionManagerState.STOPPING || oldState == SubscriptionManagerState.STOPPED) {
      callback.onError(ApolloSubscriptionException(
          "Illegal state: " + state.name + " for subscriptions to be created."
              + " SubscriptionManager.start() must be called to re-enable subscriptions."))
    } else if (oldState == SubscriptionManagerState.CONNECTED) {
      callback.onConnected()
    }
    notifyStateChanged(oldState, state)
  }

  fun doUnsubscribe(subscription: Subscription<*>) {
    synchronized(this) {
      var subscriptionRecord: SubscriptionRecord? = null
      for (record in subscriptions.values) {
        if (record.subscription === subscription) {
          subscriptionRecord = record
        }
      }
      if (subscriptionRecord != null) {
        subscriptions.remove(subscriptionRecord.id)
        if (state == SubscriptionManagerState.ACTIVE || state == SubscriptionManagerState.STOPPING) {
          transport.send(OperationClientMessage.Stop(subscriptionRecord.id.toString()))
        }
      }
      if (subscriptions.isEmpty() && state != SubscriptionManagerState.STOPPING) {
        startInactivityTimer()
      }
    }
  }

  fun doStop() {
    var subscriptionRecords: Collection<SubscriptionRecord>
    var oldState: SubscriptionManagerState
    synchronized(this) {
      oldState = state
      state = SubscriptionManagerState.STOPPING
      subscriptionRecords = subscriptions.values
      if (oldState == SubscriptionManagerState.ACTIVE) {
        for (subscriptionRecord in subscriptionRecords) {
          transport.send(OperationClientMessage.Stop(subscriptionRecord.id.toString()))
        }
      }
      state = SubscriptionManagerState.STOPPED
      transport.disconnect(OperationClientMessage.Terminate())
      subscriptions = LinkedHashMap()
    }
    for (record in subscriptionRecords) {
      record.notifyOnCompleted()
    }
    notifyStateChanged(oldState, SubscriptionManagerState.STOPPING)
    notifyStateChanged(SubscriptionManagerState.STOPPING, state)
  }

  fun onTransportConnected() {
    val subscriptionRecords: MutableCollection<SubscriptionRecord> = ArrayList()
    var oldState: SubscriptionManagerState
    synchronized(this) {
      oldState = state
      if (state == SubscriptionManagerState.CONNECTING) {
        subscriptionRecords.addAll(subscriptions.values)
        state = SubscriptionManagerState.CONNECTED
        transport.send(OperationClientMessage.Init(connectionParams.provide()))
      }
      if (state == SubscriptionManagerState.CONNECTED) {
        timer.schedule(CONNECTION_ACKNOWLEDGE_TIMEOUT_TIMER_TASK_ID, connectionAcknowledgeTimeoutTimerTask, CONNECTION_ACKNOWLEDGE_TIMEOUT)
      }
    }
    for (record in subscriptionRecords) {
      record.callback.onConnected()
    }
    notifyStateChanged(oldState, state)
  }

  fun onConnectionAcknowledgeTimeout() {
    timer.cancelTask(CONNECTION_ACKNOWLEDGE_TIMEOUT_TIMER_TASK_ID)
    dispatcher.execute { onTransportFailure(ApolloNetworkException("Subscription server is not responding")) }
  }

  fun onInactivityTimeout() {
    timer.cancelTask(INACTIVITY_TIMEOUT_TIMER_TASK_ID)
    dispatcher.execute { disconnect(false) }
  }

  fun onTransportFailure(t: Throwable?) {
    val subscriptionRecords = disconnect(true)
    for (record in subscriptionRecords) {
      record.notifyOnNetworkError(t)
    }
  }

  fun onOperationServerMessage(message: OperationServerMessage?) {
    when (message) {
      is OperationServerMessage.ConnectionAcknowledge -> {
        onConnectionAcknowledgeServerMessage()
      }
      is OperationServerMessage.Data -> {
        onOperationDataServerMessage(message)
      }
      is OperationServerMessage.Error -> {
        onErrorServerMessage(message)
      }
      is OperationServerMessage.Complete -> {
        onCompleteServerMessage(message)
      }
      is OperationServerMessage.ConnectionError -> {
        disconnect(true)
      }
      is OperationServerMessage.ConnectionKeepAlive -> {
        resetConnectionKeepAliveTimerTask()
      }
    }
  }

  /**
   * Disconnect the web socket and update the state.  If we are stopping, set the state to
   * [State.STOPPED] so that new subscription requests will **not** automatically re-open
   * the web socket.  If we are not stopping, set the state to [State.DISCONNECTED] so that
   * new subscription requests **will** automatically re-open the web socket.
   *
   * @param force if true, always disconnect web socket, regardless of the status of [.subscriptions]
   */
  fun disconnect(force: Boolean): Collection<SubscriptionRecord> {
    var oldState: SubscriptionManagerState
    var subscriptionRecords: Collection<SubscriptionRecord>
    synchronized(this) {
      oldState = state
      subscriptionRecords = subscriptions.values
      if (force || subscriptions.isEmpty()) {
        transport.disconnect(OperationClientMessage.Terminate())
        state = if (state == SubscriptionManagerState.STOPPING) SubscriptionManagerState.STOPPED else SubscriptionManagerState.DISCONNECTED
        subscriptions = LinkedHashMap()
      }
    }
    notifyStateChanged(oldState, state)
    return subscriptionRecords
  }

  override fun reconnect() {
    var oldState: SubscriptionManagerState
    synchronized(this) {
      oldState = state
      state = SubscriptionManagerState.DISCONNECTED
      transport.disconnect(OperationClientMessage.Terminate())
      state = SubscriptionManagerState.CONNECTING
      transport.connect()
    }
    notifyStateChanged(oldState, SubscriptionManagerState.DISCONNECTED)
    notifyStateChanged(SubscriptionManagerState.DISCONNECTED, SubscriptionManagerState.CONNECTING)
  }

  fun onConnectionHeartbeatTimeout() {
    reconnect()
  }

  fun onConnectionClosed() {
    var subscriptionRecords: Collection<SubscriptionRecord>
    var oldState: SubscriptionManagerState
    synchronized(this) {
      oldState = state
      subscriptionRecords = subscriptions.values
      state = SubscriptionManagerState.DISCONNECTED
      subscriptions = LinkedHashMap()
    }
    for (record in subscriptionRecords) {
      record.callback.onTerminated()
    }
    notifyStateChanged(oldState, state)
  }

  private fun resetConnectionKeepAliveTimerTask() {
    if (connectionHeartbeatTimeoutMs <= 0) {
      return
    }
    synchronized(this) { timer.schedule(CONNECTION_KEEP_ALIVE_TIMEOUT_TIMER_TASK_ID, connectionHeartbeatTimeoutTimerTask, connectionHeartbeatTimeoutMs) }
  }

  private fun startInactivityTimer() {
    timer.schedule(INACTIVITY_TIMEOUT_TIMER_TASK_ID, inactivityTimeoutTimerTask, INACTIVITY_TIMEOUT)
  }

  private fun onOperationDataServerMessage(message: OperationServerMessage.Data) {
    val subscriptionId = message.id ?: ""
    var subscriptionRecord: SubscriptionRecord?
    synchronized(this) {
      subscriptionRecord = try {
        subscriptions[UUID.fromString(subscriptionId)]
      } catch (e: IllegalArgumentException) {
        null
      }
    }
    if (subscriptionRecord != null) {
      val subscription = subscriptionRecord!!.subscription
      try {
        val response = MapResponseParser.parse(message.payload, subscription, responseAdapterCache)
        subscriptionRecord!!.notifyOnResponse(response)
      } catch (e: Exception) {
        subscriptionRecord = removeSubscriptionById(subscriptionId)
        if (subscriptionRecord != null) {
          subscriptionRecord!!.notifyOnError(ApolloSubscriptionException("Failed to parse server message", e))
        }
        return
      }
    }
  }

  private fun onConnectionAcknowledgeServerMessage() {
    var oldState: SubscriptionManagerState
    synchronized(this) {
      oldState = state
      timer.cancelTask(CONNECTION_ACKNOWLEDGE_TIMEOUT_TIMER_TASK_ID)
      if (state == SubscriptionManagerState.CONNECTED) {
        state = SubscriptionManagerState.ACTIVE
        for (subscriptionRecord in subscriptions.values) {
          transport.send(
              OperationClientMessage.Start(subscriptionRecord.id.toString(), subscriptionRecord.subscription, responseAdapterCache,
                  autoPersistSubscription, false)
          )
        }
      }
    }
    notifyStateChanged(oldState, state)
  }

  private fun onErrorServerMessage(message: OperationServerMessage.Error) {
    val subscriptionId = message.id ?: ""
    val subscriptionRecord = removeSubscriptionById(subscriptionId)
    val resendSubscriptionWithDocument: Boolean
    resendSubscriptionWithDocument = if (autoPersistSubscription) {
      val error = MapResponseParser.parseError(message.payload)
      (PROTOCOL_NEGOTIATION_ERROR_NOT_FOUND.equals(error.message, ignoreCase = true)
          || PROTOCOL_NEGOTIATION_ERROR_NOT_SUPPORTED.equals(error.message, ignoreCase = true))
    } else {
      false
    }
    if (resendSubscriptionWithDocument) {
      synchronized(this) {
        subscriptions[subscriptionRecord.id] = subscriptionRecord
        transport.send(OperationClientMessage.Start(
            subscriptionRecord.id.toString(), subscriptionRecord.subscription, responseAdapterCache, true, true
        ))
      }
    } else {
      subscriptionRecord.notifyOnError(ApolloSubscriptionServerException(message.payload))
    }
  }

  private fun onCompleteServerMessage(message: OperationServerMessage.Complete) {
    val subscriptionId = message.id ?: ""
    val subscriptionRecord = removeSubscriptionById(subscriptionId)
    subscriptionRecord.notifyOnCompleted()
  }

  private fun removeSubscriptionById(subscriptionId: String?): SubscriptionRecord {
    var subscriptionRecord: SubscriptionRecord?
    synchronized(this) {
      subscriptionRecord = try {
        subscriptions.remove(UUID.fromString(subscriptionId))
      } catch (e: IllegalArgumentException) {
        null
      }
      if (subscriptions.isEmpty()) {
        startInactivityTimer()
      }
    }
    return subscriptionRecord!!
  }

  private fun notifyStateChanged(oldState: SubscriptionManagerState, newState: SubscriptionManagerState) {
    if (oldState == newState) {
      return
    }
    for (onStateChangeListener in onStateChangeListeners) {
      onStateChangeListener.onStateChange(oldState, newState)
    }
  }

  class SubscriptionRecord internal constructor(val id: UUID, val subscription: Subscription<Subscription.Data>, val callback: SubscriptionManager.Callback<Subscription.Data>) {
    fun notifyOnResponse(response: ApolloResponse<*>?) {
      callback.onResponse(SubscriptionResponse(subscription, response as ApolloResponse<Subscription.Data>))
    }

    fun notifyOnError(error: ApolloSubscriptionException?) {
      callback.onError(error!!)
    }

    fun notifyOnNetworkError(t: Throwable?) {
      callback.onNetworkError(t!!)
    }

    fun notifyOnCompleted() {
      callback.onCompleted()
    }
  }

  private class SubscriptionTransportCallback(private val delegate: RealSubscriptionManager, private val dispatcher: Executor) : SubscriptionTransport.Callback {
    override fun onConnected() {
      dispatcher.execute { delegate.onTransportConnected() }
    }

    override fun onFailure(t: Throwable) {
      dispatcher.execute { delegate.onTransportFailure(t) }
    }

    override fun onMessage(message: OperationServerMessage) {
      dispatcher.execute { delegate.onOperationServerMessage(message) }
    }

    override fun onClosed() {
      dispatcher.execute { delegate.onConnectionClosed() }
    }
  }

  class AutoReleaseTimer {
    val tasks: MutableMap<Int, TimerTask> = LinkedHashMap()
    var timer: Timer? = null
    fun schedule(taskId: Int, task: Runnable, delay: Long) {
      val timerTask: TimerTask = object : TimerTask() {
        override fun run() {
          try {
            task.run()
          } finally {
            cancelTask(taskId)
          }
        }
      }
      synchronized(this) {
        val previousTimerTask = tasks.put(taskId, timerTask)
        previousTimerTask?.cancel()
        if (timer == null) {
          timer = Timer("Subscription SmartTimer", true)
        }
        timer!!.schedule(timerTask, delay)
      }
    }

    fun cancelTask(taskId: Int) {
      synchronized(this) {
        val timerTask = tasks.remove(taskId)
        timerTask?.cancel()
        if (tasks.isEmpty() && timer != null) {
          timer!!.cancel()
          timer = null
        }
      }
    }
  }

  companion object {
    const val CONNECTION_ACKNOWLEDGE_TIMEOUT_TIMER_TASK_ID = 1
    const val INACTIVITY_TIMEOUT_TIMER_TASK_ID = 2
    const val CONNECTION_KEEP_ALIVE_TIMEOUT_TIMER_TASK_ID = 3
    val CONNECTION_ACKNOWLEDGE_TIMEOUT = TimeUnit.SECONDS.toMillis(5)
    val INACTIVITY_TIMEOUT = TimeUnit.SECONDS.toMillis(10)
    const val PROTOCOL_NEGOTIATION_ERROR_NOT_FOUND = "PersistedQueryNotFound"
    const val PROTOCOL_NEGOTIATION_ERROR_NOT_SUPPORTED = "PersistedQueryNotSupported"
  }

}