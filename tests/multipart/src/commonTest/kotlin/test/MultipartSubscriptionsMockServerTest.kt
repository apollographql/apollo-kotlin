package test

import app.cash.turbine.test
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.exception.RouterError
import com.apollographql.mockserver.MockResponse
import com.apollographql.mockserver.MockServer
import com.apollographql.apollo.network.http.HttpNetworkTransport
import com.apollographql.apollo.testing.*
import com.apollographql.apollo.testing.internal.runTest
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import multipart.CounterSubscription
import multipart.HelloSubscription
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.use
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * Tests from https://github.com/apollographql/team-prometheus/blob/multipart-subs/rfcs/MultipartSubscriptionsTests.md
 */
class MultipartSubscriptionsMockServerTest {
  @Test
  fun onSuccessfulResponse() = multipartSubsTest {
    enqueue(
        "\r\n--graphql\r\nContent-Type: application/json\r\n\r\n{\"payload\":{\"data\":{\"hello\":\"world\"}}}" +
            "\r\n--graphql--"
    )

    apolloClient.subscription(HelloSubscription()).toFlow().test {
      assertEquals("world", awaitItem().dataOrThrow().hello)
      awaitComplete()
    }
  }

  @Test
  fun graphqlErrorsEmitAResponse() = multipartSubsTest {
    enqueue(
        "\r\n--graphql\r\nContent-Type: application/json\r\n\r\n{\"payload\":{\"data\":null,\"errors\":[{\"message\":\"Validation error\"}]}}" +
            "\r\n--graphql\r\nContent-Type: application/json\r\n\r\n{\"payload\":{\"data\":{\"hello\":\"world\"}}}" +
            "\r\n--graphql--"
    )

    apolloClient.subscription(HelloSubscription()).toFlow().test {
      assertEquals("Validation error", awaitItem().errors?.get(0)?.message)
      assertEquals("world", awaitItem().dataOrThrow().hello)
      awaitComplete()
    }
  }

  @Test
  fun heartbeatsAreIgnored() = multipartSubsTest {
    enqueue(
        "\r\n--graphql\r\nContent-Type: application/json\r\n\r\n{\"payload\":{\"data\":{\"hello\":\"world\"}}}" +
            "\r\n--graphql\r\nContent-Type: application/json\r\n\r\n{}" +
            "\r\n--graphql\r\nContent-Type: application/json\r\n\r\n{\"payload\":{\"data\":{\"hello\":\"what's up?\"}}}" +
            "\r\n--graphql--"
    )

    apolloClient.subscription(HelloSubscription()).toFlow().test {
      assertEquals("world", awaitItem().dataOrThrow().hello)
      assertEquals("what's up?", awaitItem().dataOrThrow().hello)
      awaitComplete()
    }
  }

  @Test
  fun routerError() = multipartSubsTest {
    enqueue(
        "\r\n--graphql\r\nContent-Type: application/json\r\n\r\n{\"payload\":{\"data\":{\"hello\":\"world\"}}}" +
            "\r\n--graphql\r\nContent-Type: application/json\r\n\r\n{\"errors\":[{\"message\":\"Oops something went wrong\"}]}" +
            "\r\n--graphql--"
    )

    apolloClient.subscription(HelloSubscription()).toFlow().test {
      assertEquals("world", awaitItem().dataOrThrow().hello)
      awaitItem().exception.apply {
        assertIs<RouterError>(this)
        assertEquals("Oops something went wrong", this.errors.first().message)
      }
      awaitComplete()
    }
  }

  @Test
  fun clientEmitsResponsesAsSoonAsDelimiterIsReceived() = multipartSubsTest {
    enqueue(
        "\r\n--graphql\r\nContent-Type: application/json\r\n\r\n{\"payload\":{\"data\":{\"hello\":\"world\"}}}" +
            "\r\n--graphql"
    )


    apolloClient.subscription(HelloSubscription()).toFlow().test(timeout = 300.milliseconds) {
      assertEquals("world", awaitItem().dataOrThrow().hello)
      enqueue("\r\nContent-Type: application/json\r\n\r\n{\"payload\":{\"data\":{\"hello\":\"what's up?\"}}}\r\n--graphql--")
      assertEquals("what's up?", awaitItem().dataOrThrow().hello)
      awaitComplete()
    }
  }

  @OptIn(DelicateCoroutinesApi::class)
  @Test
  fun clientDoesNotCompleteUntilCloseDelimiterIsReceived() = multipartSubsTest {
    enqueue(
        "\r\n--graphql\r\nContent-Type: application/json\r\n\r\n{\"payload\":{\"data\":{\"hello\":\"world\"}}}" +
            "\r\n--graphql"
    )

    val channel = Channel<ApolloResponse<HelloSubscription.Data>>(Channel.UNLIMITED)

    /**
     * Using [GlobalScope] because the coroutine is stuck on reading the network and only completes
     * after the 10s OkHttpTimeout
     */
    GlobalScope.launch {
      apolloClient.subscription(HelloSubscription()).toFlow().collect {
        channel.trySend(it)
      }
    }

    @Suppress("DEPRECATION")
    assertEquals("world", channel.awaitElement().dataOrThrow().hello)
    @Suppress("DEPRECATION")
    channel.assertNoElement()
  }

  @Test
  fun clientCompletesIfHttpMessageTerminatesEarly() = multipartSubsTest {
    enqueue(
        "\r\n--graphql\r\nContent-Type: application/json\r\n\r\n{\"payload\":{\"data\":{\"hello\":\"world\"}}}" +
            "\r\n--graphql"
    )

    apolloClient.subscription(HelloSubscription()).toFlow().test(timeout = 300.milliseconds) {
      assertEquals("world", awaitItem().dataOrThrow().hello)
      terminateHttpBody()
      assertEquals("premature end of multipart body", awaitItem().exception?.message)
      awaitComplete()
    }
  }

  @Test
  fun clientIgnoresUnknownProperties() = multipartSubsTest {
    enqueue(
        "\r\n--graphql\r\nContent-Type: application/json\r\n\r\n{\"foo\":\"bar\",\"payload\":{\"data\":{\"hello\":\"world\"}}}" +
            "\r\n--graphql\r\nContent-Type: application/json\r\n\r\n{\"foo\":\"bar\"}" +
            "\r\n--graphql\r\nContent-Type: application/json\r\n\r\n{\"payload\":{\"data\":{\"hello\":\"what's up?\"}}}" +
            "\r\n--graphql--"
    )

    apolloClient.subscription(HelloSubscription()).toFlow().test(timeout = 300.milliseconds) {
      assertEquals("world", awaitItem().dataOrThrow().hello)
      assertEquals("what's up?", awaitItem().dataOrThrow().hello)
      awaitComplete()
    }
  }

  @Test
  fun malformedJsonTriggersAnError() = multipartSubsTest {
    enqueue(
        "\r\n--graphql\r\nContent-Type: application/json\r\n\r\ngarbage" +
            "\r\n--graphql--"
    )

    apolloClient.subscription(HelloSubscription()).toFlow().test(timeout = 300.milliseconds) {
      assertEquals("Malformed JSON at path []", awaitItem().exception?.message)
      awaitComplete()
    }
  }

  @Test
  fun routerErrorsMayContainLocation() = multipartSubsTest {
    enqueue(
        "\r\n--graphql\r\nContent-Type: application/json\r\n\r\n{\"errors\":[{\"message\":\"Oops something went wrong\",\"path\":[\"hello\"],\"foo\":\"bar\"}]}" +
            "\r\n--graphql--"
    )

    apolloClient.subscription(HelloSubscription()).toFlow().test(timeout = 300.milliseconds) {
      awaitItem().exception.apply {
        assertIs<RouterError>(this)
        assertEquals("Oops something went wrong", errors.first().message)
      }
      awaitComplete()
    }
  }

  @Test
  fun nullPayloadIsIgnored() = multipartSubsTest {
    enqueue(
        "\r\n--graphql\r\nContent-Type: application/json\r\n\r\n{\"payload\":null}" +
            "\r\n--graphql--"
    )

    apolloClient.subscription(HelloSubscription()).toFlow().test(timeout = 300.milliseconds) {
      awaitComplete()
    }
  }

  @Test
  fun nullPayloadAndErrorsEmitsError() = multipartSubsTest {
    enqueue(
        "\r\n--graphql\r\nContent-Type: application/json\r\n\r\n{\"payload\":null,\"errors\":[{\"message\":\"Oops something went wrong\",\"path\":[\"hello\"]}]}" +
            "\r\n--graphql--"
    )

    apolloClient.subscription(HelloSubscription()).toFlow().test(timeout = 300.milliseconds) {
      awaitItem().exception.apply {
        assertIs<RouterError>(this)
        assertEquals("Oops something went wrong", errors.first().message)
      }
      awaitComplete()
    }
  }

  @Test
  fun nullErrorsIsIgnored() = multipartSubsTest {
    enqueue(
        "\r\n--graphql\r\nContent-Type: application/json\r\n\r\n{\"errors\":null}" +
            "\r\n--graphql--"
    )

    apolloClient.subscription(HelloSubscription()).toFlow().test(timeout = 300.milliseconds) {
      awaitComplete()
    }
  }

  @Test
  fun errorsTakePrecedenceOverPayload() = multipartSubsTest {
    enqueue(
        "\r\n--graphql\r\nContent-Type: application/json\r\n\r\n{\"payload\":{\"data\":{\"hello\":\"world\"}},\"errors\":[{\"message\":\"Oops something went wrong\",\"path\":[\"hello\"]}]}" +
            "\r\n--graphql--"
    )

    apolloClient.subscription(HelloSubscription()).toFlow().test(timeout = 300.milliseconds) {
      awaitItem().exception.apply {
        assertIs<RouterError>(this)
        assertEquals("Oops something went wrong", errors.first().message)
      }
      awaitComplete()
    }
  }

  @Test
  fun trailingHeartbeatIsIgnored() = multipartSubsTest {
    enqueue("--graphql\r\nContent-Type: application/json\r\n\r\n{}\r\n--graphql--\r\n")

    apolloClient.subscription(CounterSubscription()).toFlow().toList().apply {
      assertEquals(0, size)
    }
  }

  @Test
  fun malformedMultipartTriggersReturnsErrorResponse() = multipartSubsTest {
    // No end boundary
    enqueue("--graphql\r\n")
    terminateHttpBody()

    apolloClient.subscription(CounterSubscription()).toFlow().toList().apply {
      assertEquals(1, size)
      assertTrue(get(0).exception?.message?.contains("unexpected characters after boundary") == true)
    }
  }
}

private class Context(
    val apolloClient: ApolloClient,
    private val channel: Channel<String>,
) {
  fun enqueue(text: String) {
    channel.trySend(text)
  }

  fun terminateHttpBody() {
    channel.close()
  }
}

private fun multipartSubsTest(block: suspend Context.() -> Unit) = runTest {
  @Suppress("DEPRECATION")
  if (platform() != Platform.Js) {
    MockServer().use { mockServer ->
      ApolloClient.Builder()
          .serverUrl(mockServer.url())
          .subscriptionNetworkTransport(
              HttpNetworkTransport.Builder()
                  .serverUrl(mockServer.url())
                  .build()
          )
          .build()
          .use { apolloClient ->
            val channel = Channel<String>(Channel.UNLIMITED)
            val context = Context(apolloClient, channel)

            mockServer.enqueue(
                MockResponse.Builder()
                    .addHeader("Content-Type", "multipart/mixed; boundary=\"graphql\"")
                    .addHeader("Transfer-Encoding", "chunked")
                    .body(channel.consumeAsFlow().map { it.encodeUtf8() }.asChunked())
                    .build()
            )
            context.block()
          }
    }
  }
}


/**
 * Turns a Flow<ByteString> into a `Transfer-Encoding: chunked` compatible one.
 */
private fun Flow<ByteString>.asChunked(): Flow<ByteString> {
  // Chunked format is a sequence of:
  // - chunk-size (in hexadecimal) + CRLF
  // - chunk-data + CRLF
  // Ended with a chunk-size of 0 + CRLF + CRLF
  return map { payload ->
    val buffer = Buffer().apply {
      writeHexadecimalUnsignedLong(payload.size.toLong())
      writeUtf8("\r\n")
      write(payload)
      writeUtf8("\r\n")
    }
    buffer.readByteString()
  }
      .onCompletion { emit("0\r\n\r\n".encodeUtf8()) }
}
