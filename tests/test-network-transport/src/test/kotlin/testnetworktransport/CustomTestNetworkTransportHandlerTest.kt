package testnetworktransport

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.testing.TestNetworkTransport
import com.apollographql.apollo3.testing.TestNetworkTransportHandler
import com.apollographql.apollo3.testing.runTest
import com.apollographql.apollo3.testing.testNetworkTransport
import com.benasher44.uuid.uuid4
import testnetworktransport.test.GetHeroQuery_TestBuilder.Data
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ApolloExperimental::class)
class CustomTestNetworkTransportHandlerTest {
  private lateinit var handler: CustomTestNetworkTransportHandler
  private lateinit var apolloClient: ApolloClient

  private fun setUp() {
    handler = CustomTestNetworkTransportHandler()
    apolloClient = ApolloClient.Builder()
        .networkTransport(TestNetworkTransport(handler))
        .build()
  }

  private fun tearDown() {
    apolloClient.dispose()
  }

  private class CustomTestNetworkTransportHandler : TestNetworkTransportHandler {
    private var counter = 0
    override fun handle(request: ApolloRequest<*>): ApolloResponse<*> {
      return ApolloResponse.Builder(
          operation = GetHeroQuery("mock"),
          requestUuid = request.requestUuid,
          data = GetHeroQuery.Data {
            hero = droidHero {
              name = "Droid ${counter++}"
            }
          }
      ).build()
    }
  }

  @Test
  fun customHandler() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = GetHeroQuery("001")
    val actual0 = apolloClient.query(query).execute()
    val actual1 = apolloClient.query(query).execute()
    val actual2 = apolloClient.query(query).execute()
    assertEquals("Droid 0", actual0.dataAssertNoErrors.hero.name)
    assertEquals("Droid 1", actual1.dataAssertNoErrors.hero.name)
    assertEquals("Droid 2", actual2.dataAssertNoErrors.hero.name)
  }

  @Test
  fun registerAndQueueMethodsFail() = runTest(before = { setUp() }, after = { tearDown() }) {
    assertFailsWith(IllegalStateException::class) {
      apolloClient.testNetworkTransport.enqueue(ApolloResponse.Builder(GetHeroQuery("id"), uuid4(), null).build())
    }
    assertFailsWith(IllegalStateException::class) {
      apolloClient.testNetworkTransport.register(GetHeroQuery("id"), null)
    }
  }
}
