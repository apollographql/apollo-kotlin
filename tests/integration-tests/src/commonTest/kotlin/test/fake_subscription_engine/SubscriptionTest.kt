package test.fake_subscription_engine

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.AnyApolloAdapter
import com.apollographql.apollo3.api.ScalarAdapters
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
import com.apollographql.apollo3.testing.internal.runTest
import fake_subscription_engine.RandomSubscription
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class SubscriptionTest {
  @Test
  fun unknownMessagesDoNotStopTheFlows() = runTest {
    val queuedMessages = Channel<String>(64)

    val webSocketEngine = FakeWebSocketEngine(
        onReceive = {
          queuedMessages.receive()
        },
        onSend = {
          val map = it.toMessageMap()
          when (map?.get("type")) {
            "start" -> {
              launch {
                queuedMessages.trySend(data(map["id"].toString(), 42))

                queuedMessages.trySend("\"Ignored Message\"")

                queuedMessages.trySend(data(map["id"].toString(), 41))

                queuedMessages.trySend(complete(map["id"].toString()))
              }
            }
            "connection_init" -> {
              queuedMessages.trySend(mapOf("type" to "connection_ack").toMessageString())
            }
          }
        }
    )
    val subscriptionNetworkTransport = WebSocketNetworkTransport.Builder()
        .serverUrl("unused")
        .webSocketEngine(webSocketEngine)
        .build()

    val apolloClient = ApolloClient.Builder()
        .subscriptionNetworkTransport(subscriptionNetworkTransport)
        .serverUrl("unused")
        .build()


    withTimeout(500) {
      val response = apolloClient.subscription(RandomSubscription()).toFlow().toList()

      assertEquals(listOf(42, 41), response.map { it.data?.random })
    }
  }

  private fun Map<String, Any?>.toMessageString(): String = buildJsonString {
    AnyApolloAdapter.toJson(this, ScalarAdapters.Empty, this@toMessageString)
  }

  private fun data(id: String, random: Int): String {
    return mapOf(
        "type" to "data",
        "id" to id,
        "payload" to mapOf(
            "data" to mapOf(
                "random" to random
            )
        )
    ).toMessageString()
  }

  private fun complete(id: String): String {
    return mapOf(
        "type" to "complete",
        "id" to id,
    ).toMessageString()
  }

  private fun String.toMessageMap(): Map<String, Any?>? = try {
    @Suppress("UNCHECKED_CAST")
    AnyApolloAdapter.fromJson(
        BufferedSourceJsonReader(Buffer().writeUtf8(this)),
        ScalarAdapters.Empty
    ) as? Map<String, Any?>
  } catch (e: Exception) {
    null
  }
}
