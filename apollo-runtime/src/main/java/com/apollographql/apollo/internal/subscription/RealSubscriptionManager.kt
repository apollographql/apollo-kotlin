package com.apollographql.apollo.internal.subscription

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.api.internal.Utils.__checkNotNull
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.internal.ResponseNormalizer
import com.apollographql.apollo.cache.normalized.internal.dependentKeys
import com.apollographql.apollo.cache.normalized.internal.normalize
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.response.OperationResponseParser
import com.apollographql.apollo.subscription.OnSubscriptionManagerStateChangeListener
import com.apollographql.apollo.subscription.OperationClientMessage
import com.apollographql.apollo.subscription.OperationServerMessage
import com.apollographql.apollo.subscription.SubscriptionConnectionParamsProvider
import com.apollographql.apollo.subscription.SubscriptionManagerState
import com.apollographql.apollo.subscription.SubscriptionTransport
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class RealSubscriptionManager(customScalarAdapters: CustomScalarAdapters,
                              transportFactory: SubscriptionTransport.Factory, connectionParams: SubscriptionConnectionParamsProvider,
                              dispatcher: Executor, connectionHeartbeatTimeoutMs: Long,
                              responseNormalizer: Function0<ResponseNormalizer<Map<String, Any>>>, autoPersistSubscription: Boolean) : SubscriptionManager {
  @JvmField
  var subscriptions: MutableMap<UUID, SubscriptionRecord> = LinkedHashMap()

  @Volatile
  override var state: SubscriptionManagerState = SubscriptionManagerState.DISCONNECTED

  val timer = AutoReleaseTimer()
  private val customScalarAdapters: CustomScalarAdapters
  private val transport: SubscriptionTransport
  private val connectionParams: SubscriptionConnectionParamsProvider
  private val dispatcher: Executor
  private val connectionHeartbeatTimeoutMs: Long
  private val responseNormalizer: Function0<ResponseNormalizer<Map<String, Any>>>
  private val connectionAcknowledgeTimeoutTimerTask = Runnable { onConnectionAcknowledgeTimeout() }
  private val inactivityTimeoutTimerTask = Runnable { onInactivityTimeout() }
  private val connectionHeartbeatTimeoutTimerTask = Runnable { onConnectionHeartbeatTimeout() }
  private val onStateChangeListeners: MutableList<OnSubscriptionManagerStateChangeListener> = CopyOnWriteArrayList()
  private val autoPersistSubscription: Boolean
  override fun <D : Operation.Data> subscribe(subscription: Subscription<D>, callback: SubscriptionManager.Callback<D>) {
    __checkNotNull(subscription, "subscription == null")
    __checkNotNull(callback, "callback == null")
    dispatcher.execute { doSubscribe(subscription, callback) }
  }

  override fun unsubscribe(subscription: Subscription<*>) {
    __checkNotNull(subscription, "subscription == null")
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
    onStateChangeListeners.add(__checkNotNull(onStateChangeListener, "onStateChangeListener == null"))
  }

  override fun removeOnStateChangeListener(onStateChangeListener: OnSubscriptionManagerStateChangeListener) {
    onStateChangeListeners.remove(__checkNotNull(onStateChangeListener, "onStateChangeListener == null"))
  }

  fun doSubscribe(subscription: Subscription<*>, callback: SubscriptionManager.Callback<*>) {
    var oldState: SubscriptionManagerState
    synchronized(this) {
      oldState = state
      if (state != SubscriptionManagerState.STOPPING && state != SubscriptionManagerState.STOPPED) {
        timer.cancelTask(INACTIVITY_TIMEOUT_TIMER_TASK_ID)
        val subscriptionId = UUID.randomUUID()
        subscriptions[subscriptionId] = SubscriptionRecord(subscriptionId, subscription as Subscription<Operation.Data>, callback as SubscriptionManager.Callback<Operation.Data>)
        if (state == SubscriptionManagerState.DISCONNECTED) {
          state = SubscriptionManagerState.CONNECTING
          transport.connect()
        } else if (state == SubscriptionManagerState.ACTIVE) {
          transport.send(
              OperationClientMessage.Start(subscriptionId.toString(), subscription, customScalarAdapters, autoPersistSubscription, false)
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
    if (message is OperationServerMessage.ConnectionAcknowledge) {
      onConnectionAcknowledgeServerMessage()
    } else if (message is OperationServerMessage.Data) {
      onOperationDataServerMessage(message)
    } else if (message is OperationServerMessage.Error) {
      onErrorServerMessage(message)
    } else if (message is OperationServerMessage.Complete) {
      onCompleteServerMessage(message)
    } else if (message is OperationServerMessage.ConnectionError) {
      disconnect(true)
    } else if (message is OperationServerMessage.ConnectionKeepAlive) {
      resetConnectionKeepAliveTimerTask()
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
      val normalizer = responseNormalizer.invoke()
      val subscription = subscriptionRecord!!.subscription
      val parser = OperationResponseParser(
          subscription,
          customScalarAdapters,
      )
      val response: Response<*>
      try {
        response = parser.parse(message.payload).let {
          val records = it.data?.let { subscription.normalize(it, customScalarAdapters, normalizer as ResponseNormalizer<Map<String, Any>?>) }
          it.copy(dependentKeys = records.dependentKeys())
        }

      } catch (e: Exception) {
        subscriptionRecord = removeSubscriptionById(subscriptionId)
        if (subscriptionRecord != null) {
          subscriptionRecord!!.notifyOnError(ApolloSubscriptionException("Failed to parse server message", e))
        }
        return
      }
      subscriptionRecord!!.notifyOnResponse(response, normalizer.records())
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
              OperationClientMessage.Start(subscriptionRecord.id.toString(), subscriptionRecord.subscription, customScalarAdapters,
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
      val error = OperationResponseParser.parseError(message.payload)
      (PROTOCOL_NEGOTIATION_ERROR_NOT_FOUND.equals(error.message, ignoreCase = true)
          || PROTOCOL_NEGOTIATION_ERROR_NOT_SUPPORTED.equals(error.message, ignoreCase = true))
    } else {
      false
    }
    if (resendSubscriptionWithDocument) {
      synchronized(this) {
        subscriptions[subscriptionRecord.id] = subscriptionRecord
        transport.send(OperationClientMessage.Start(
            subscriptionRecord.id.toString(), subscriptionRecord.subscription, customScalarAdapters, true, true
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

  class SubscriptionRecord internal constructor(val id: UUID, val subscription: Subscription<Operation.Data>, val callback: SubscriptionManager.Callback<Operation.Data>) {
    fun notifyOnResponse(response: Response<*>?, cacheRecords: Collection<Record>) {
      callback.onResponse(SubscriptionResponse(subscription, response as Response<Operation.Data>, cacheRecords))
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

  init {
    __checkNotNull(customScalarAdapters, "scalarTypeAdapters == null")
    __checkNotNull(transportFactory, "transportFactory == null")
    __checkNotNull(dispatcher, "dispatcher == null")
    __checkNotNull(responseNormalizer, "responseNormalizer == null")
    this.customScalarAdapters = __checkNotNull(customScalarAdapters, "scalarTypeAdapters == null")
    this.connectionParams = __checkNotNull(connectionParams, "connectionParams == null")
    transport = transportFactory.create(SubscriptionTransportCallback(this, dispatcher))
    this.dispatcher = dispatcher
    this.connectionHeartbeatTimeoutMs = connectionHeartbeatTimeoutMs
    this.responseNormalizer = responseNormalizer
    this.autoPersistSubscription = autoPersistSubscription
  }
}