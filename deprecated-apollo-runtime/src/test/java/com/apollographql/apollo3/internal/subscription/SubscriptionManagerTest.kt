package com.apollographql.apollo3.internal.subscription

import com.apollographql.apollo3.api.CompiledSelection
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.cache.normalized.CacheResolver
import com.apollographql.apollo3.cache.normalized.FieldPolicyCacheResolver
import com.apollographql.apollo3.subscription.OperationClientMessage
import com.apollographql.apollo3.subscription.OperationServerMessage
import com.apollographql.apollo3.subscription.SubscriptionConnectionParams
import com.apollographql.apollo3.subscription.SubscriptionConnectionParamsProvider
import com.apollographql.apollo3.subscription.SubscriptionManagerState
import com.apollographql.apollo3.subscription.SubscriptionTransport
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.util.ArrayList
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class SubscriptionManagerTest {
  private val connectionHeartbeatTimeoutMs = TimeUnit.SECONDS.toMillis(1)
  private val subscriptionTransportFactory: MockSubscriptionTransportFactory = MockSubscriptionTransportFactory()
  private val subscriptionManager: RealSubscriptionManager
  private val subscription1 = MockSubscription("MockSubscription1")
  private val subscription2 = MockSubscription("MockSubscription2")
  private val onStateChangeListener = SubscriptionManagerOnStateChangeListener()

  init {
    subscriptionManager = RealSubscriptionManager(
        CustomScalarAdapters.Empty,
        subscriptionTransportFactory,
        SubscriptionConnectionParamsProvider.Const(SubscriptionConnectionParams()),
        MockExecutor(),
        connectionHeartbeatTimeoutMs,
        FieldPolicyCacheResolver,
        false)
    subscriptionManager.addOnStateChangeListener(onStateChangeListener)
  }

  @Before
  fun setUp() {
    assertThat(subscriptionTransportFactory.subscriptionTransport).isNotNull()
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.DISCONNECTED)
  }

  @Test
  fun connecting() {
    subscriptionManager.subscribe(subscription1, SubscriptionManagerCallbackAdapter())
    assertThat(subscriptionTransportFactory.subscriptionTransport).isNotNull()
    assertThat(subscriptionTransportFactory.subscriptionTransport.connected).isTrue()
    assertThat(subscriptionTransportFactory.subscriptionTransport.lastSentMessage).isNull()
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.CONNECTING)
    subscriptionManager.subscribe(subscription2, SubscriptionManagerCallbackAdapter())
    assertThat(subscriptionTransportFactory.subscriptionTransport.connected).isTrue()
    assertThat(subscriptionTransportFactory.subscriptionTransport.lastSentMessage).isNull()
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.CONNECTING)
    assertThat(subscriptionManager.subscriptions).hasSize(2)
    assertThat(subscriptionManager.timer.tasks).isEmpty()
  }

  @Test
  fun connected() {
    subscriptionManager.subscribe(subscription1, SubscriptionManagerCallbackAdapter())
    subscriptionTransportFactory.callback.onConnected()
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.CONNECTED)
    assertThat(subscriptionTransportFactory.subscriptionTransport.lastSentMessage).isInstanceOf(OperationClientMessage.Init::class.java)
    assertThat(subscriptionManager.timer.tasks).containsKey(RealSubscriptionManager.CONNECTION_ACKNOWLEDGE_TIMEOUT_TIMER_TASK_ID)
  }

  @Test
  fun active() {
    subscriptionManager.subscribe(subscription1, SubscriptionManagerCallbackAdapter())
    subscriptionTransportFactory.callback.onConnected()
    subscriptionTransportFactory.callback.onMessage(OperationServerMessage.ConnectionAcknowledge)
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.ACTIVE)
    assertThat(subscriptionTransportFactory.subscriptionTransport.lastSentMessage).isInstanceOf(OperationClientMessage.Start::class.java)
    assertThat(subscriptionManager.timer.tasks).isEmpty()
  }

  @Test
  @Throws(Exception::class)
  fun disconnected() {
    subscriptionManager.subscribe(subscription1, SubscriptionManagerCallbackAdapter())
    subscriptionTransportFactory.callback.onConnected()
    subscriptionTransportFactory.callback.onMessage(OperationServerMessage.ConnectionAcknowledge)
    subscriptionManager.unsubscribe(subscription1)
    assertThat(subscriptionManager.subscriptions).isEmpty()
    assertThat(subscriptionTransportFactory.subscriptionTransport.lastSentMessage).isInstanceOf(OperationClientMessage.Stop::class.java)
    assertThat(subscriptionManager.timer.tasks).containsKey(RealSubscriptionManager.INACTIVITY_TIMEOUT_TIMER_TASK_ID)
    onStateChangeListener.awaitState(SubscriptionManagerState.DISCONNECTED, RealSubscriptionManager.INACTIVITY_TIMEOUT + 800, TimeUnit.MILLISECONDS)
    assertThat(subscriptionTransportFactory.subscriptionTransport.disconnectMessage).isInstanceOf(OperationClientMessage.Terminate::class.java)
    assertThat(subscriptionManager.timer.tasks).isEmpty()
  }

  @Test
  @Throws(Exception::class)
  fun reconnect() {
    subscriptionManager.subscribe(subscription1, SubscriptionManagerCallbackAdapter())
    subscriptionTransportFactory.callback.onConnected()
    subscriptionTransportFactory.callback.onMessage(OperationServerMessage.ConnectionAcknowledge)
    subscriptionManager.unsubscribe(subscription1)
    onStateChangeListener.awaitState(SubscriptionManagerState.DISCONNECTED, RealSubscriptionManager.INACTIVITY_TIMEOUT + 800, TimeUnit.MILLISECONDS)
    subscriptionManager.subscribe(subscription2, SubscriptionManagerCallbackAdapter())
    subscriptionTransportFactory.callback.onConnected()
    subscriptionTransportFactory.callback.onMessage(OperationServerMessage.ConnectionAcknowledge)
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.ACTIVE)
    assertThat(subscriptionTransportFactory.subscriptionTransport.lastSentMessage).isInstanceOf(OperationClientMessage.Start::class.java)
    assertThat(subscriptionManager.timer.tasks).isEmpty()
  }

  @Test
  @Throws(Exception::class)
  fun disconnectedOnConnectionAcknowledgeTimeout() {
    subscriptionManager.subscribe(subscription1, SubscriptionManagerCallbackAdapter())
    subscriptionTransportFactory.callback.onConnected()
    assertThat(subscriptionManager.timer.tasks).containsKey(RealSubscriptionManager.CONNECTION_ACKNOWLEDGE_TIMEOUT_TIMER_TASK_ID)
    onStateChangeListener.awaitState(SubscriptionManagerState.DISCONNECTED, RealSubscriptionManager.CONNECTION_ACKNOWLEDGE_TIMEOUT + 800, TimeUnit.MILLISECONDS)
    assertThat(subscriptionTransportFactory.subscriptionTransport.disconnectMessage).isInstanceOf(OperationClientMessage.Terminate::class.java)
    assertThat(subscriptionManager.timer.tasks).isEmpty()
    assertThat(subscriptionManager.subscriptions).isEmpty()
  }

  @Test
  fun disconnectedOnTransportFailure() {
    val subscriptionManagerCallback1 = SubscriptionManagerCallbackAdapter<Subscription.Data>()
    subscriptionManager.subscribe(subscription1, subscriptionManagerCallback1)
    val subscriptionManagerCallback2 = SubscriptionManagerCallbackAdapter<Subscription.Data>()
    subscriptionManager.subscribe(subscription2, subscriptionManagerCallback2)
    subscriptionTransportFactory.callback.onConnected()
    subscriptionTransportFactory.callback.onMessage(OperationServerMessage.ConnectionAcknowledge)
    subscriptionTransportFactory.callback.onFailure(UnsupportedOperationException())
    assertThat(subscriptionManagerCallback1.networkError).isInstanceOf(UnsupportedOperationException::class.java)
    assertThat(subscriptionManagerCallback2.networkError).isInstanceOf(UnsupportedOperationException::class.java)
    assertThat(subscriptionTransportFactory.subscriptionTransport.disconnectMessage).isInstanceOf(OperationClientMessage.Terminate::class.java)
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.DISCONNECTED)
    assertThat(subscriptionManager.timer.tasks).isEmpty()
    assertThat(subscriptionManager.subscriptions).isEmpty()
  }

  @Test
  fun unsubscribeOnComplete() {
    val subscriptionManagerCallback1 = SubscriptionManagerCallbackAdapter<Subscription.Data>()
    subscriptionManager.subscribe(subscription1, subscriptionManagerCallback1)
    val subscriptionManagerCallback2 = SubscriptionManagerCallbackAdapter<Subscription.Data>()
    subscriptionManager.subscribe(subscription2, subscriptionManagerCallback2)
    val subscriptionIds: List<UUID> = ArrayList(subscriptionManager.subscriptions.keys)
    subscriptionTransportFactory.callback.onConnected()
    subscriptionTransportFactory.callback.onMessage(OperationServerMessage.ConnectionAcknowledge)
    subscriptionTransportFactory.callback.onMessage(OperationServerMessage.Complete(subscriptionIds[0].toString()))
    assertThat(subscriptionManagerCallback1.completed).isTrue()
    assertThat(subscriptionManager.subscriptions).hasSize(1)
    assertThat(subscriptionManagerCallback2.completed).isFalse()
  }

  @Test
  fun unsubscribeOnError() {
    val subscriptionManagerCallback1 = SubscriptionManagerCallbackAdapter<Subscription.Data>()
    subscriptionManager.subscribe(subscription1, subscriptionManagerCallback1)
    val subscriptionManagerCallback2 = SubscriptionManagerCallbackAdapter<Subscription.Data>()
    subscriptionManager.subscribe(subscription2, subscriptionManagerCallback2)
    val subscriptionIds: List<UUID> = ArrayList(subscriptionManager.subscriptions.keys)
    subscriptionTransportFactory.callback.onConnected()
    subscriptionTransportFactory.callback.onMessage(OperationServerMessage.ConnectionAcknowledge)
    subscriptionTransportFactory.callback.onMessage(OperationServerMessage.Error(subscriptionIds[0].toString(),
        mapOf("key1" to "value1", "key2" to "value2")))
    assertThat(subscriptionManagerCallback1.error).isInstanceOf(ApolloSubscriptionServerException::class.java)
    assertThat((subscriptionManagerCallback1.error as ApolloSubscriptionServerException?)!!.errorPayload).containsEntry("key1", "value1")
    assertThat((subscriptionManagerCallback1.error as ApolloSubscriptionServerException?)!!.errorPayload).containsEntry("key2", "value2")
    assertThat(subscriptionManager.subscriptions).hasSize(1)
    assertThat(subscriptionManagerCallback2.completed).isFalse()
  }

  @Test
  fun notifyOnData() {
    val subscriptionManagerCallback1 = SubscriptionManagerCallbackAdapter<Subscription.Data>()
    subscriptionManager.subscribe(subscription1, subscriptionManagerCallback1)
    val subscriptionIds: List<UUID> = ArrayList(subscriptionManager.subscriptions.keys)
    subscriptionTransportFactory.callback.onConnected()
    subscriptionTransportFactory.callback.onMessage(OperationServerMessage.ConnectionAcknowledge)
    subscriptionTransportFactory.callback.onMessage(OperationServerMessage.Data(subscriptionIds[0].toString(), emptyMap()))
    assertThat(subscriptionManagerCallback1.response).isNotNull()
  }

  @Test
  fun notifyOnConnected() {
    val subscriptionManagerCallback1 = SubscriptionManagerCallbackAdapter<Subscription.Data>()
    subscriptionManager.subscribe(subscription1, subscriptionManagerCallback1)
    subscriptionTransportFactory.callback.onConnected()
    assertThat(subscriptionManagerCallback1.connected).isTrue()
  }

  @Test
  fun duplicateSubscriptions() {
    val subscriptionManagerCallback1 = SubscriptionManagerCallbackAdapter<Subscription.Data>()
    subscriptionManager.subscribe(subscription1, subscriptionManagerCallback1)
    val subscriptionManagerCallback2 = SubscriptionManagerCallbackAdapter<Subscription.Data>()
    subscriptionManager.subscribe(subscription1, subscriptionManagerCallback2)
    assertThat(subscriptionManagerCallback2.error).isNull()
  }

  @Test
  @Throws(Exception::class)
  fun reconnectingAfterHeartbeatTimeout() {
    subscriptionManager.subscribe(subscription1, SubscriptionManagerCallbackAdapter())
    subscriptionTransportFactory.callback.onConnected()
    subscriptionTransportFactory.callback.onMessage(OperationServerMessage.ConnectionAcknowledge)
    subscriptionTransportFactory.callback.onMessage(OperationServerMessage.ConnectionKeepAlive)
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.ACTIVE)
    assertThat(subscriptionManager.timer.tasks).containsKey(RealSubscriptionManager.CONNECTION_KEEP_ALIVE_TIMEOUT_TIMER_TASK_ID)
    onStateChangeListener.awaitState(SubscriptionManagerState.DISCONNECTED, connectionHeartbeatTimeoutMs + 800, TimeUnit.MILLISECONDS)
    onStateChangeListener.awaitState(SubscriptionManagerState.CONNECTING, 800, TimeUnit.MILLISECONDS)
  }

  @Test
  fun startWhenDisconnected() {
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.DISCONNECTED)
    subscriptionManager.start()
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.DISCONNECTED)
  }

  @Test
  fun startWhenConnected() {
    subscriptionManager.subscribe(subscription1, SubscriptionManagerCallbackAdapter())
    subscriptionTransportFactory.callback.onConnected()
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.CONNECTED)
    subscriptionManager.start()
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.CONNECTED)
  }

  @Test
  fun startWhenActive() {
    subscriptionManager.subscribe(subscription1, SubscriptionManagerCallbackAdapter())
    subscriptionTransportFactory.callback.onConnected()
    subscriptionTransportFactory.callback.onMessage(OperationServerMessage.ConnectionAcknowledge)
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.ACTIVE)
    subscriptionManager.start()
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.ACTIVE)
  }

  @Test
  fun startWhenStopped() {
    subscriptionManager.subscribe(subscription1, SubscriptionManagerCallbackAdapter())
    subscriptionTransportFactory.callback.onConnected()
    subscriptionTransportFactory.callback.onMessage(OperationServerMessage.ConnectionAcknowledge)
    subscriptionManager.stop()
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.STOPPED)
    subscriptionManager.start()
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.DISCONNECTED)
  }

  @Test
  fun stopWhenDisconnected() {
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.DISCONNECTED)
    subscriptionManager.stop()
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.STOPPED)
  }

  @Test
  fun stopWhenConnected() {
    subscriptionManager.subscribe(subscription1, SubscriptionManagerCallbackAdapter())
    subscriptionManager.subscribe(subscription2, SubscriptionManagerCallbackAdapter())
    subscriptionTransportFactory.callback.onConnected()
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.CONNECTED)
    subscriptionManager.stop()
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.STOPPED)
  }

  @Test
  fun stopWhenActive() {
    subscriptionManager.subscribe(subscription1, SubscriptionManagerCallbackAdapter())
    subscriptionTransportFactory.callback.onConnected()
    subscriptionTransportFactory.callback.onMessage(OperationServerMessage.ConnectionAcknowledge)
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.ACTIVE)
    subscriptionManager.stop()
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.STOPPED)
  }

  @Test
  fun stopWhenStopped() {
    subscriptionManager.subscribe(subscription1, SubscriptionManagerCallbackAdapter())
    subscriptionTransportFactory.callback.onConnected()
    subscriptionTransportFactory.callback.onMessage(OperationServerMessage.ConnectionAcknowledge)
    subscriptionManager.stop()
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.STOPPED)
    subscriptionManager.stop()
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.STOPPED)
  }

  @Test
  fun subscriptionWhenStopped() {
    subscriptionManager.stop()
    val subscriptionManagerCallback = SubscriptionManagerCallbackAdapter<Subscription.Data>()
    subscriptionManager.subscribe(subscription1, subscriptionManagerCallback)
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.STOPPED)
    assertThat(subscriptionManagerCallback.error).isInstanceOf(ApolloSubscriptionException::class.java)
    assertThat(subscriptionManagerCallback.error!!.message).startsWith("Illegal state: STOPPED")
  }

  @Test
  fun connectionTerminated() {
    val subscriptionManagerCallback = SubscriptionManagerCallbackAdapter<Subscription.Data>()
    subscriptionManager.subscribe(subscription1, subscriptionManagerCallback)
    subscriptionTransportFactory.callback.onConnected()
    subscriptionTransportFactory.callback.onMessage(OperationServerMessage.ConnectionAcknowledge)
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.ACTIVE)
    subscriptionTransportFactory.callback.onClosed()
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.DISCONNECTED)
    assertThat(subscriptionManagerCallback.terminated).isTrue()
  }

  private class MockSubscriptionTransportFactory : SubscriptionTransport.Factory {
    lateinit var subscriptionTransport: MockSubscriptionTransport
    lateinit var callback: SubscriptionTransport.Callback
    override fun create(callback: SubscriptionTransport.Callback): SubscriptionTransport {
      this.callback = callback
      return MockSubscriptionTransport().also { subscriptionTransport = it }
    }
  }

  private class MockSubscriptionTransport : SubscriptionTransport {
    @Volatile
    var lastSentMessage: OperationClientMessage? = null

    @Volatile
    var connected = false

    @Volatile
    var disconnectMessage: OperationClientMessage? = null
    override fun connect() {
      connected = true
    }

    override fun disconnect(message: OperationClientMessage) {
      connected = false
      disconnectMessage = message
    }

    override fun send(message: OperationClientMessage) {
      lastSentMessage = message
    }
  }

  private class MockExecutor : Executor {
    override fun execute(command: Runnable) {
      command.run()
    }
  }

  private class MockSubscription(val operationId: String) : Subscription<Subscription.Data> {
    override fun document(): String {
      return "subscription {\n  commentAdded(repoFullName: \"repo\") {\n    __typename\n    id\n    content\n  }\n}"
    }

    override fun serializeVariables(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters) {
      writer.beginObject()
      writer.endObject()
    }

    override fun adapter() = throw UnsupportedOperationException()

    override fun name(): String = "SomeSubscription"

    override fun id() = operationId
    override fun selections(): List<CompiledSelection> {
      return emptyList()
    }
  }

  private class SubscriptionManagerCallbackAdapter<D : Subscription.Data> : SubscriptionManager.Callback<D> {
    @Volatile
    var response: SubscriptionResponse<D>? = null

    @Volatile
    var error: ApolloSubscriptionException? = null

    @Volatile
    var networkError: Throwable? = null

    @Volatile
    var completed = false

    @Volatile
    var terminated = false

    @Volatile
    var connected = false
    override fun onResponse(response: SubscriptionResponse<D>) {
      this.response = response
    }

    override fun onError(error: ApolloSubscriptionException) {
      this.error = error
    }

    override fun onNetworkError(t: Throwable) {
      networkError = t
    }

    override fun onCompleted() {
      completed = true
    }

    override fun onTerminated() {
      terminated = true
    }

    override fun onConnected() {
      connected = true
    }
  }
}
