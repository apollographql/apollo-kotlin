package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Adapter
import com.apollographql.apollo.api.CompiledField
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.api.json.JsonWriter
import com.apollographql.apollo.network.websocket.WebSocketNetworkTransport
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import okio.use
import kotlin.test.Test
import kotlin.test.assertTrue

class HeadersTest {
  @Test
  fun headersCannotBeUsedOnWebSockets() = runTest {
    val client = ApolloClient.Builder()
        .serverUrl("https://unused.com")
        .addHttpHeader("foo", "bar")
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl("https://unused.com")
                .build()
        )
        .build()

    client.use {
      val response = it.subscription(NothingSubscription()).toFlow().single()
      assertTrue(response.exception?.message?.contains("Apollo: the WebSocket browser API doesn't allow passing headers") == true)
    }
  }
}

class NothingSubscription : Subscription<Nothing> {
  override fun document(): String {
    return ""
  }

  override fun name(): String {
    return "nothing"
  }

  override fun id(): String {
    return ""
  }

  override fun adapter(): Adapter<Nothing> {
    TODO("Not yet implemented")
  }

  override fun serializeVariables(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, withDefaultValues: Boolean) {
    TODO("Not yet implemented")
  }

  override fun rootField(): CompiledField {
    TODO("Not yet implemented")
  }
}
