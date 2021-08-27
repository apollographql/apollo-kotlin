package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.AnyAdapter
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.integration.normalizer.GetJsonScalarQuery
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.withFetchPolicy
import com.apollographql.apollo3.cache.normalized.withStore
import com.apollographql.apollo3.integration.normalizer.type.Json
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runWithMainLoop
import readResource
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class JsonScalarTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient
  private lateinit var store: ApolloStore

  @BeforeTest
  fun setUp() {
    store = ApolloStore(MemoryCacheFactory())
    mockServer = MockServer()
    apolloClient = ApolloClient(mockServer.url())
        .withStore(store)
        .withCustomScalarAdapter(Json.type, AnyAdapter)
  }

  // see https://github.com/apollographql/apollo-android/issues/2854
  @Test
  fun jsonScalar() = runWithMainLoop {
    mockServer.enqueue(readResource("JsonScalar.json"))
    var response = apolloClient.query(GetJsonScalarQuery())

    assertFalse(response.hasErrors())
    var expectedMap = mapOf(
        "obj" to mapOf("key" to "value"),
        "list" to listOf(0, 1, 2)
    )
    assertEquals(expectedMap, response.data!!.json)

    /**
     * Update the json value, it should be replaced, not merged
     */
    mockServer.enqueue(readResource("JsonScalarModified.json"))
    apolloClient.query(ApolloRequest(GetJsonScalarQuery()).withFetchPolicy(FetchPolicy.NetworkFirst))
    response = apolloClient.query(ApolloRequest(GetJsonScalarQuery()).withFetchPolicy(FetchPolicy.CacheOnly))

    assertFalse(response.hasErrors())

    expectedMap = mapOf(
        "obj" to mapOf("key2" to "value2"),
    )
    assertEquals(expectedMap, response.data!!.json)
  }
}
