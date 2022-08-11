package test.root_fragment

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.enqueueData
import com.apollographql.apollo3.testing.internal.runTest
import root_fragment.SearchSomethingQuery
import root_fragment.type.buildSearch
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
    apolloClient.close()
  }

  @Test
  fun test() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .build()

    mockServer.enqueueData(
        SearchSomethingQuery.Data {
          searchSomething = buildSearch {
            name = "foo"
          }
        }
    )

    val fragments = apolloClient.query(SearchSomethingQuery()).execute().data?.fragments
    assertEquals("foo", fragments?.something?.searchSomething?.name)
    assertEquals("foo", fragments?.something2?.searchSomething?.name)
  }
}
