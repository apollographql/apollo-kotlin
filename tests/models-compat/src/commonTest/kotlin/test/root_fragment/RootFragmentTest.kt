package test.root_fragment

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runTest
import root_fragment.SearchSomethingQuery
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A test to make sure reading root fragments never rewinds
 * See https://github.com/apollographql/apollo-kotlin/issues/3914
 */
class RootFragmentTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient

  private fun setUp() {
    mockServer = MockServer()
  }

  private suspend fun tearDown() {
    mockServer.stop()
    // This is important. JS will hang if the BatchingHttpInterceptor scope is not cancelled
    apolloClient.dispose()
  }

  @Test
  fun test() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .build()

    mockServer.enqueue("""
      {
        "data": {
          "__typename": "Search",
          "searchSomething": {
            "name": "foo"
          }
        }
      }
    """.trimIndent())

    val fragments = apolloClient.query(SearchSomethingQuery()).execute().data?.fragments
    assertEquals("foo", fragments?.something?.searchSomething?.name)
    assertEquals("foo", fragments?.something2?.searchSomething?.name)
  }
}
