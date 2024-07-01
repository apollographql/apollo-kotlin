package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.AnyAdapter
import com.apollographql.apollo.cache.normalized.ApolloStore
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.store
import com.apollographql.apollo.integration.normalizer.GetJsonScalarQuery
import com.apollographql.apollo.integration.normalizer.type.Json
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.testing.internal.runTest
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
        .addCustomScalarAdapter(Json.type, AnyAdapter)
        .build()
  }

  private suspend fun tearDown() {
    mockServer.close()
  }

  // see https://github.com/apollographql/apollo-kotlin/issues/2854
  @Test
  fun jsonScalar() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(testFixtureToUtf8("JsonScalar.json"))
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
    mockServer.enqueueString(testFixtureToUtf8("JsonScalarModified.json"))
    apolloClient.query(GetJsonScalarQuery()).fetchPolicy(FetchPolicy.NetworkFirst).execute()
    response = apolloClient.query(GetJsonScalarQuery()).fetchPolicy(FetchPolicy.CacheOnly).execute()

    assertFalse(response.hasErrors())

    expectedMap = mapOf(
        "obj" to mapOf("key2" to "value2"),
    )
    assertEquals(expectedMap, response.data!!.json)
  }
}
