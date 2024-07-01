package testnetworktransport

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.exception.DefaultApolloException
import com.apollographql.apollo.network.NetworkTransport
import com.apollographql.apollo.testing.enqueueTestResponse
import com.apollographql.apollo.testing.internal.runTest
import com.apollographql.apollo.testing.registerTestResponse
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import testnetworktransport.type.buildDroid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CustomTestNetworkTransportHandlerTest {
  private lateinit var networkTransport: CustomTestNetworkTransport
  private lateinit var apolloClient: ApolloClient

  private fun setUp() {
    networkTransport = CustomTestNetworkTransport()
    apolloClient = ApolloClient.Builder()
        .networkTransport(CustomTestNetworkTransport())
        .build()
  }

  private fun tearDown() {
    apolloClient.close()
  }

  private class CustomTestNetworkTransport : NetworkTransport {
    private var counter = 0

    override fun <D : Operation.Data> execute(request: ApolloRequest<D>): Flow<ApolloResponse<D>> {
      @Suppress("UNCHECKED_CAST")
      return flowOf(
          ApolloResponse.Builder(
              operation = GetHeroQuery("mock"),
              requestUuid = request.requestUuid,
          ).data(
              GetHeroQuery.Data {
                hero = buildDroid {
                  name = "Droid ${counter++}"
                }
              }
          ).build() as ApolloResponse<D>
      )
    }

    override fun dispose() {}
  }

  @Test
  fun customHandler() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = GetHeroQuery("001")
    val actual0 = apolloClient.query(query).execute()
    val actual1 = apolloClient.query(query).execute()
    val actual2 = apolloClient.query(query).execute()
    assertEquals("Droid 0", actual0.dataOrThrow().hero.name)
    assertEquals("Droid 1", actual1.dataOrThrow().hero.name)
    assertEquals("Droid 2", actual2.dataOrThrow().hero.name)
  }

  @Test
  fun registerAndQueueMethodsFail() = runTest(before = { setUp() }, after = { tearDown() }) {
    assertFailsWith(IllegalStateException::class) {
      apolloClient.enqueueTestResponse(ApolloResponse.Builder(GetHeroQuery("id"), uuid4()).exception( DefaultApolloException()).build())
    }
    assertFailsWith(IllegalStateException::class) {
      apolloClient.registerTestResponse(GetHeroQuery("id"), null)
    }
  }
}
