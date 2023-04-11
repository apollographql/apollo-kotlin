package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.AnyScalarAdapter
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.store
import com.apollographql.apollo3.integration.normalizer.GetJsonScalarQuery
import com.apollographql.apollo3.integration.normalizer.type.Json
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.internal.runTest
import testFixtureToUtf8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class JsonScalarTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient
  private lateinit var store: ApolloStore

  private suspend fun setUp() {
    store = ApolloStore(MemoryCacheFactory())
    mockServer = MockServer()
    apolloClient = ApolloClient.Builder().serverUrl(mockServer.url())
        .store(store)
        .addScalarAdapter(Json.type, AnyScalarAdapter)
        .build()
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  // see https://github.com/apollographql/apollo-android/issues/2854
  @Test
  fun jsonScalar() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue(testFixtureToUtf8("JsonScalar.json"))
    var response = apolloClient.query(GetJsonScalarQuery()).execute()

    assertFalse(response.hasErrors())
    var expectedMap = mapOf(
        "obj" to mapOf("key" to "value"),
        "list" to listOf(0, 1, 2)
    )
    assertEquals(expectedMap, response.data!!.json)

    /**
     * Update the json value, it should be replaced, not merged
     */
    mockServer.enqueue(testFixtureToUtf8("JsonScalarModified.json"))
    apolloClient.query(GetJsonScalarQuery()).fetchPolicy(FetchPolicy.NetworkFirst).execute()
    response = apolloClient.query(GetJsonScalarQuery()).fetchPolicy(FetchPolicy.CacheOnly).execute()

    assertFalse(response.hasErrors())

    expectedMap = mapOf(
        "obj" to mapOf("key2" to "value2"),
    )
    assertEquals(expectedMap, response.data!!.json)
  }
}
