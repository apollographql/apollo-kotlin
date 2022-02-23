package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.composeJsonResponse
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueueMultipart
import com.apollographql.apollo3.testing.runTest
import kotlinx.coroutines.flow.toList
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ApolloExperimental::class)
class DeferTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient

  private suspend fun setUp() {
    mockServer = MockServer()
    apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).build()
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun multipleBodies() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = HeroNameQuery()
    val expectedDataList = listOf(
        HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2")),
        HeroNameQuery.Data(HeroNameQuery.Hero("Artoo")),
    )
    val jsonList = expectedDataList.map { data ->
      buildJsonString {
        query.composeJsonResponse(jsonWriter = this, data = data)
      }
    }
    mockServer.enqueueMultipart(jsonList)

    val actualDataList = apolloClient.query(query).toFlow().toList().map { it.dataAssertNoErrors }
    assertEquals(expectedDataList, actualDataList)
  }
}
