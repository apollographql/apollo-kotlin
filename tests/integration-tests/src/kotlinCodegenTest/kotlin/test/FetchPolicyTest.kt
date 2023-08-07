package test

import app.cash.turbine.test
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.composeJsonResponse
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.CacheFirstInterceptor
import com.apollographql.apollo3.cache.normalized.CacheOnlyInterceptor
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.isFromCache
import com.apollographql.apollo3.cache.normalized.refetchPolicyInterceptor
import com.apollographql.apollo3.cache.normalized.store
import com.apollographql.apollo3.cache.normalized.watch
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.exception.CacheMissException
import com.apollographql.apollo3.exception.JsonEncodingException
import com.apollographql.apollo3.integration.normalizer.CharacterNameByIdQuery
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.enqueue
import com.apollographql.apollo3.testing.internal.runTest
import com.apollographql.apollo3.testing.receiveOrTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

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
    apolloClient.close()
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
    apolloClient.query(query).execute().exception.let {
      assertIs<CacheMissException>(it)
      assertIs<JsonEncodingException>(it.suppressedExceptions.first())
    }
  }

  @Test
  fun cacheFirstExecuteThrowing() = runTest(before = { setUp() }, after = { tearDown() }) {
    @Suppress("DEPRECATION")
    apolloClient = apolloClient.newBuilder().useV3ExceptionHandling(true).build()
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
      assertIs<CacheMissException>(e)
    }
  }


  @Test
  fun cacheFirstToFlowThrowing() = runTest(before = { setUp() }, after = { tearDown() }) {
    @Suppress("DEPRECATION")
    apolloClient = apolloClient.newBuilder().useV3ExceptionHandling(true).build()

    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))
    mockServer.enqueue(query, data)

    // First query should hit the network and save in cache
    var responses = apolloClient.query(query)
        .fetchPolicy(FetchPolicy.NetworkFirst)
        .toFlow()

    responses.test {
      val response1 = awaitItem()
      assertEquals(data, response1.data)
      assertFalse(response1.isFromCache)
      awaitComplete()
    }

    // Second query should only hit the cache
    responses = apolloClient.query(query)
        .fetchPolicy(FetchPolicy.CacheFirst)
        .toFlow()
    responses.test {
      val response1 = awaitItem()
      assertEquals(data, response1.data)
      assertTrue(response1.isFromCache)
      awaitComplete()
    }

    // Clear the store and offer a malformed response, we should get a composite error
    store.clearAll()
    mockServer.enqueue("malformed")
    responses = apolloClient.query(query)
        .fetchPolicy(FetchPolicy.CacheFirst)
        .toFlow()
    responses.test {
      assertIs<CacheMissException>(awaitError())
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

    call.execute().exception.let {
      assertIs<JsonEncodingException>(it)
      assertIs<CacheMissException>(it.suppressedExceptions.first())
    }
  }

  @Test
  fun networkFirstExecuteThrowing() = runTest(before = { setUp() }, after = { tearDown() }) {
    @Suppress("DEPRECATION")
    apolloClient = apolloClient.newBuilder().useV3ExceptionHandling(true).build()
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
  fun networkFirstToFlowThrowing() = runTest(before = { setUp() }, after = { tearDown() }) {
    @Suppress("DEPRECATION")
    apolloClient = apolloClient.newBuilder().useV3ExceptionHandling(true).build()

    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))

    val call = apolloClient.query(query).fetchPolicy(FetchPolicy.NetworkFirst)

    // First query should hit the network and save in cache
    mockServer.enqueue(query, data)
    var responses = call.toFlow()
    responses.test {
      val response1 = awaitItem()
      assertEquals(data, response1.data)
      assertFalse(response1.isFromCache)
      awaitComplete()
    }

    // Now data is cached but it shouldn't be used since network will go through
    mockServer.enqueue(query, data)
    responses = call.toFlow()
    responses.test {
      val response1 = awaitItem()
      assertEquals(data, response1.data)
      assertFalse(response1.isFromCache)
      awaitComplete()
    }

    // Network error -> we should hit now the cache
    mockServer.enqueue("malformed")
    responses = call.toFlow()
    responses.test {
      val response1 = awaitItem()
      assertEquals(data, response1.data)
      assertTrue(response1.isFromCache)
      awaitComplete()
    }

    // Network error and no cache -> we should get an error
    mockServer.enqueue("malformed")
    store.clearAll()
    responses = call.toFlow()
    responses.test {
      assertIs<JsonEncodingException>(awaitError())
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

    val call = apolloClient.query(query).fetchPolicy(FetchPolicy.NetworkOnly)

    // First query should hit the network and save in cache
    mockServer.enqueue(query, data)
    val response = call.execute()

    assertNotNull(response.data)
    assertFalse(response.isFromCache)

    // Offer a malformed response, it should fail
    mockServer.enqueue("malformed")
    assertNotNull(call.execute().exception)
  }

  @Test
  fun networkOnlyThrowing() = runTest(before = { setUp() }, after = { tearDown() }) {
    @Suppress("DEPRECATION")
    apolloClient = apolloClient.newBuilder().useV3ExceptionHandling(true).build()
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))

    val call = apolloClient.query(query).fetchPolicy(FetchPolicy.NetworkOnly)

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
  fun cacheAndNetwork() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))
    var caught: Throwable? = null

    // Initial state: everything fails
    // Cache Error + Network Error => Error
    mockServer.enqueue(statusCode = 500)
    apolloClient.query(query).fetchPolicy(FetchPolicy.CacheAndNetwork).execute().exception.let {
      assertIs<CacheMissException>(it)
      assertIs<ApolloHttpException>(it.suppressedExceptions.first())
    }

    // Make the network return something
    // Cache Error + Network Success => 2 responses
    mockServer.enqueue(query, data)
    var responses = apolloClient.query(query).fetchPolicy(FetchPolicy.CacheAndNetwork).toFlow().catch { caught = it }.toList()

    assertNull(caught)
    assertEquals(2, responses.size)
    assertNull(responses[0].data)
    assertIs<CacheMissException>(responses[0].exception)
    assertNotNull(responses[1].data)
    assertFalse(responses[1].isFromCache)
    assertEquals("R2-D2", responses[1].data?.hero?.name)

    // Now cache is populated but make the network fail again
    // Cache Success + Network Error => 1 response with cache value + 1 response with network exception
    caught = null
    mockServer.enqueue(statusCode = 500)
    responses = apolloClient.query(query).fetchPolicy(FetchPolicy.CacheAndNetwork).toFlow().catch { caught = it }.toList()

    assertNull(caught)
    assertEquals(2, responses.size)
    assertNotNull(responses[0].data)
    assertTrue(responses[0].isFromCache)
    assertEquals("R2-D2", responses[0].data?.hero?.name)
    assertNull(responses[1].data)
    assertIs<ApolloHttpException>(responses[1].exception)

    // Cache Success + Network Success => 2 responses
    mockServer.enqueue(query, data)
    responses = apolloClient.query(query).fetchPolicy(FetchPolicy.CacheAndNetwork).toFlow().toList()

    assertEquals(2, responses.size)
    assertNotNull(responses[0].data)
    assertTrue(responses[0].isFromCache)
    assertNotNull(responses[1].data)
    assertFalse(responses[1].isFromCache)
  }


  private val refetchPolicyInterceptor = object : ApolloInterceptor {
    var hasSeenValidResponse: Boolean = false
    override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
      return if (!hasSeenValidResponse) {
        CacheOnlyInterceptor.intercept(request, chain).onEach {
          if (it.data != null) {
            // We have valid data, we can now use the network
            hasSeenValidResponse = true
          }
        }
      } else {
        // If for some reason we have a cache miss, get fresh data from the network
        CacheFirstInterceptor.intercept(request, chain)
      }
    }
  }

  @Test
  fun cacheAndNetworkThrowing() = runTest(before = { setUp() }, after = { tearDown() }) {
    @Suppress("DEPRECATION")
    apolloClient = apolloClient.newBuilder().useV3ExceptionHandling(true).build()

    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))
    var caught: Throwable? = null
    // Initial state: everything fails
    // Cache Error + Network Error => Error
    mockServer.enqueue(statusCode = 500)
    assertFailsWith<CacheMissException> {
      apolloClient.query(query).fetchPolicy(FetchPolicy.CacheAndNetwork).toFlow().toList()
    }

    // Make the network return something
    // Cache Error + Network Success => 1 response (no exception)
    mockServer.enqueue(query, data)
    var responses = apolloClient.query(query).fetchPolicy(FetchPolicy.CacheAndNetwork).toFlow().catch { caught = it }.toList()

    assertNull(caught)
    assertEquals(1, responses.size)
    assertNotNull(responses[0].data)
    assertFalse(responses[0].isFromCache)
    assertEquals("R2-D2", responses[0].data?.hero?.name)

    // Now cache is populated but make the network fail again
    // Cache Success + Network Error => 1 response + 1 network exception
    caught = null
    mockServer.enqueue(statusCode = 500)
    responses = apolloClient.query(query).fetchPolicy(FetchPolicy.CacheAndNetwork).toFlow().catch { caught = it }.toList()

    assertIs<ApolloException>(caught)
    assertEquals(1, responses.size)
    assertNotNull(responses[0].data)
    assertTrue(responses[0].isFromCache)
    assertEquals("R2-D2", responses[0].data?.hero?.name)

    // Cache Success + Network Success => 1 response
    mockServer.enqueue(query, data)
    responses = apolloClient.query(query).fetchPolicy(FetchPolicy.CacheAndNetwork).toFlow().toList()

    assertEquals(2, responses.size)
    assertNotNull(responses[0].data)
    assertTrue(responses[0].isFromCache)
    assertNotNull(responses[1].data)
    assertFalse(responses[1].isFromCache)
  }

  /**
   * Uses a refetchPolicy that will not go to the network until it has seen a valid response
   */
  @Test
  fun customRefetchPolicy() = runTest(before = { setUp() }, after = { tearDown() }) {
    val channel = Channel<ApolloResponse<HeroNameQuery.Data>>()

    /**
     * Start the watcher
     */
    val operation1 = HeroNameQuery()
    val job = launch {
      apolloClient.query(operation1)
          .fetchPolicy(FetchPolicy.CacheOnly)
          .refetchPolicyInterceptor(refetchPolicyInterceptor)
          .watch()
          .collect {
            // Don't send the first response, it's a cache miss
            if (it.exception == null) channel.send(it)
          }
    }

    delay(200)

    /**
     * Make a first query that is disjoint from the watcher
     */
    val operation2 = CharacterNameByIdQuery("83")
    mockServer.enqueue(
        buildJsonString {
          operation2.composeJsonResponse(
              this,
              CharacterNameByIdQuery.Data(
                  CharacterNameByIdQuery.Character(
                      "Luke"
                  )
              )
          )
        }
    )

    apolloClient.query(operation2)
        .fetchPolicy(FetchPolicy.NetworkOnly)
        .execute()

    /**
     * Because the query was disjoint, the watcher will see a cache miss and not receive anything.
     * Because initially the refetchPolicy uses CacheOnly, no network request will be made
     */
    try {
      channel.receiveOrTimeout(50)
      error("An exception was expected")
    } catch (_: TimeoutCancellationException) {
    }

    mockServer.enqueue(
        buildJsonString {
          operation1.composeJsonResponse(
              this,
              HeroNameQuery.Data(
                  HeroNameQuery.Hero(
                      "Leila"
                  )
              )
          )
        }
    )

    /**
     * Now we query operation1 from the network and it should update the watcher automatically
     */
    apolloClient.query(operation1)
        .fetchPolicy(FetchPolicy.NetworkOnly)
        .execute()

    var response = channel.receiveOrTimeout()
    assertTrue(response.isFromCache)
    assertEquals("Leila", response.data?.hero?.name)

    /**
     * clear the cache and trigger the watcher again
     */
    store.clearAll()

    mockServer.enqueue(
        buildJsonString {
          operation1.composeJsonResponse(
              this,
              HeroNameQuery.Data(
                  HeroNameQuery.Hero(
                      "Chewbacca"
                  )
              )
          )
        }
    )
    store.publish(setOf("${CacheKey.rootKey().key}.hero"))

    /**
     * This time the watcher should do a network request
     */
    response = channel.receiveOrTimeout()
    assertFalse(response.isFromCache)
    assertEquals("Chewbacca", response.data?.hero?.name)

    /**
     * Check that 3 network requests have been made
     */
    mockServer.takeRequest()
    mockServer.takeRequest()
    mockServer.takeRequest()

    job.cancel()
    channel.cancel()
  }
}
