package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloAdapter
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.ScalarAdapters
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
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
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl("https://unused.com")
                .addHeader("foo", "bar")
                .build()
        )
        .build()

    client.use {
      val response = it.subscription(NothingSubscription()).toFlow().single()
      assertTrue(response.exception?.cause?.message?.contains("Apollo: the WebSocket browser API doesn't allow passing headers") == true)
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

  override fun adapter(): ApolloAdapter<Nothing> {
    TODO("Not yet implemented")
  }

  override fun serializeVariables(writer: JsonWriter, scalarAdapters: ScalarAdapters) {
    TODO("Not yet implemented")
  }

  override fun rootField(): CompiledField {
    TODO("Not yet implemented")
  }
}
