package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.executeCacheAndNetwork
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.isFromCache
import com.apollographql.apollo3.cache.normalized.store
import com.apollographql.apollo3.exception.ApolloCompositeException
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.enqueue
import com.apollographql.apollo3.testing.runTest
import kotlinx.coroutines.flow.toList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

@OptIn(ApolloExperimental::class)
class FetchPolicyTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient
  private lateinit var store: ApolloStore

  private suspend fun setUp() {
    store = ApolloStore(MemoryCacheFactory())
    mockServer = MockServer()
    apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).store(store).build()
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun cacheFirst() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))
    mockServer.enqueue(query, data)

    // First query should hit the network and save in cache
    var response = apolloClient.query(query)
        .fetchPolicy(FetchPolicy.NetworkFirst)
        .execute()

    assertNotNull(response.data)
    assertFalse(response.isFromCache)

    // Second query should only hit the cache
    response = apolloClient.query(query).execute()

    assertNotNull(response.data)
    assertTrue(response.isFromCache)

    // Clear the store and offer a malformed response, we should get a composite error
    store.clearAll()
    mockServer.enqueue("malformed")
    try {
      apolloClient.query(query).execute()
      fail("we expected the query to fail")
    } catch (e: Exception) {
      assertTrue(e is ApolloCompositeException)
    }
  }

  @Test
  fun networkFirst() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))

    val call = apolloClient.query(query).fetchPolicy(FetchPolicy.NetworkFirst)

    // First query should hit the network and save in cache
    mockServer.enqueue(query, data)
    var response = call.execute()

    assertNotNull(response.data)
    assertFalse(response.isFromCache)

    // Now data is cached but it shouldn't be used since network will go through
    mockServer.enqueue(query, data)
    response = call.execute()

    assertNotNull(response.data)
    assertFalse(response.isFromCache)

    // Network error -> we should hit now the cache
    mockServer.enqueue("malformed")
    response = call.execute()

    assertNotNull(response.data)
    assertTrue(response.isFromCache)

    // Network error and no cache -> we should get an error
    mockServer.enqueue("malformed")
    store.clearAll()
    try {
      call.execute()
      fail("NETWORK_FIRST should throw the network exception if nothing is in the cache")
    } catch (e: Exception) {

    }
  }

  @Test
  fun cacheOnly() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))


    // First query should hit the network and save in cache
    mockServer.enqueue(query, data)
    var response = apolloClient.query(query).execute()

    assertNotNull(response.data)
    assertFalse(response.isFromCache)

    // Second query should only hit the cache
    response = apolloClient.query(query).fetchPolicy(FetchPolicy.CacheOnly).execute()

    // And make sure we don't read the network
    assertNotNull(response.data)
    assertTrue(response.isFromCache)
  }

  @Test
  fun networkOnly() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))

    val call =  apolloClient.query(query).fetchPolicy(FetchPolicy.NetworkOnly)

    // First query should hit the network and save in cache
    mockServer.enqueue(query, data)
    val response = call.execute()

    assertNotNull(response.data)
    assertFalse(response.isFromCache)

    // Offer a malformed response, it should fail
    mockServer.enqueue("malformed")
    try {
      call.execute()
      fail("we expected a failure")
    } catch (e: Exception) {

    }
  }

  @Test
  fun queryCacheAndNetwork() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))

    // Initial state: everything fails
    // Cache Error + Network Error => Error
    mockServer.enqueue(MockResponse(statusCode = 500))
    assertFailsWith(ApolloCompositeException::class) {
      apolloClient.query(query).executeCacheAndNetwork().toList()
    }

    // Make the network return something
    // Cache Error + Nework Success => 1 response
    mockServer.enqueue(query, data)
    var responses = apolloClient.query(query).executeCacheAndNetwork().toList()

    assertEquals(1, responses.size)
    assertNotNull(responses[0].data)
    assertFalse(responses[0].isFromCache)
    assertEquals("R2-D2", responses[0].data?.hero?.name)

    // Now cache is populated but make the network fail again
    // Cache Success + Network Error => 1 response
    mockServer.enqueue(MockResponse(statusCode = 500))
    responses = apolloClient.query(query).executeCacheAndNetwork().toList()

    assertEquals(1, responses.size)
    assertNotNull(responses[0].data)
    assertTrue(responses[0].isFromCache)
    assertEquals("R2-D2", responses[0].data?.hero?.name)

    // Cache Success + Network Success => 1 response
    mockServer.enqueue(query, data)
    responses = apolloClient.query(query).executeCacheAndNetwork().toList()

    assertEquals(2, responses.size)
    assertNotNull(responses[0].data)
    assertTrue(responses[0].isFromCache)
    assertNotNull(responses[1].data)
    assertFalse(responses[1].isFromCache)
  }
}