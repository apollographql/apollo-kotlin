package incubating

import app.cash.turbine.test
import com.apollographql.apollo3.exception.SubscriptionOperationException
import com.apollographql.apollo3.testing.FooSubscription
import com.apollographql.apollo3.testing.FooSubscription.Companion.completeMessage
import com.apollographql.apollo3.testing.FooSubscription.Companion.nextMessage
import com.apollographql.apollo3.testing.mockServerWebSocketTest
import com.apollographql.apollo3.testing.operationId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.single
import org.junit.Test
import sample.server.OperationErrorSubscription
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SampleServerTest {

  @Test
  fun simple() = mockServerWebSocketTest {
    apolloClient.subscription(FooSubscription())
        .toFlow()
        .test {
          awaitConnectionInit()
          val operationId = serverReader.awaitMessage().operationId()
          repeat(5) {
            serverWriter.enqueueMessage(nextMessage(operationId, it))
            assertEquals(it, awaitItem().data?.foo)
          }
          serverWriter.enqueueMessage(completeMessage(operationId))

          awaitComplete()
        }
  }

  @Test
  fun interleavedSubscriptions() = mockServerWebSocketTest {
    0.until(2).map {
      apolloClient.subscription(FooSubscription()).toFlow()
    }.merge()
        .test {
          awaitConnectionInit()
          val operationId1 = serverReader.awaitMessage().operationId()
          val operationId2 = serverReader.awaitMessage().operationId()

          repeat(3) {
            serverWriter.enqueueMessage(nextMessage(operationId1, it))
            awaitItem().apply {
              assertEquals(operationId1, requestUuid.toString())
              assertEquals(it, data?.foo)
            }
            serverWriter.enqueueMessage(nextMessage(operationId2, it))
            awaitItem().apply {
              assertEquals(operationId2, requestUuid.toString())
              assertEquals(it, data?.foo)
            }
          }

          serverWriter.enqueueMessage(completeMessage(operationId1))
          serverWriter.enqueueMessage(completeMessage(operationId2))

          awaitComplete()
        }
  }

  @Test
  fun slowConsumer() = mockServerWebSocketTest {
    /**
     * Simulate a low read on the first 5 items.
     * During that time, the server should continue sending.
     * Then resume reading as fast as possible and make sure we didn't drop any items.
     */
    apolloClient.subscription(FooSubscription())
        .toFlow()
        .test {
          awaitConnectionInit()
          val operationId = serverReader.awaitMessage().operationId()
          repeat(1000) {
            serverWriter.enqueueMessage(nextMessage(operationId, it))
          }
          delay(3000)
          repeat(1000) {
            assertEquals(it, awaitItem().data?.foo)
          }
          serverWriter.enqueueMessage(completeMessage(operationId))

          awaitComplete()
        }
  }

  @Test
  fun operationError() = mockServerWebSocketTest {
    val response = apolloClient.subscription(OperationErrorSubscription())
        .toFlow()
        .single()
    assertIs<SubscriptionOperationException>(response.exception)
    val error = response.exception.cast<SubscriptionOperationException>().payload
        .cast<Map<String, String>>()
        .get("message")
    assertEquals("Woops", error)
  }
}

@Suppress("UNCHECKED_CAST")
private fun <T> Any?.cast() = this as T

