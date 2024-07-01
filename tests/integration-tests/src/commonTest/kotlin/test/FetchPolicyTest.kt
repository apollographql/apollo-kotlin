@file:OptIn(ApolloInternal::class)
@file:Suppress("DEPRECATION")

package test

import app.cash.turbine.test
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.composeJsonResponse
import com.apollographql.apollo.api.json.buildJsonString
import com.apollographql.apollo.cache.normalized.ApolloStore
import com.apollographql.apollo.cache.normalized.CacheFirstInterceptor
import com.apollographql.apollo.cache.normalized.CacheOnlyInterceptor
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.api.CacheKey
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.isFromCache
import com.apollographql.apollo.cache.normalized.refetchPolicyInterceptor
import com.apollographql.apollo.cache.normalized.store
import com.apollographql.apollo.cache.normalized.watch
import com.apollographql.apollo.exception.ApolloCompositeException
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.CacheMissException
import com.apollographql.apollo.exception.JsonEncodingException
import com.apollographql.apollo.integration.normalizer.CharacterNameByIdQuery
import com.apollographql.apollo.integration.normalizer.HeroNameQuery
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.apollographql.apollo.testing.assertNoElement
import com.apollographql.apollo.testing.awaitElement
import com.apollographql.apollo.testing.internal.runTest
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.awaitRequest
import com.apollographql.mockserver.enqueueError
import com.apollographql.mockserver.enqueueString
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
    apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).store(store = store).build()
  }

  private fun tearDown() {
    mockServer.close()
    apolloClient.close()
  }

  @Test
  fun cacheFirst() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))
    mockServer.enqueueString(query.composeJsonResponse(data))

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
    mockServer.enqueueString("malformed")
    apolloClient.query(query).execute().exception.let {
      assertIs<CacheMissException>(it)
      assertIs<JsonEncodingException>(it.suppressedExceptions.first())
    }
  }

  @Test
  fun cacheFirstExecuteThrowing() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient = apolloClient.newBuilder().build()
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))
    mockServer.enqueueString(query.composeJsonResponse(data))

    // First query should hit the network and save in cache
    @Suppress("DEPRECATION")
    var response = apolloClient.query(query)
        .fetchPolicy(FetchPolicy.NetworkFirst)
        .executeV3()

    assertNotNull(response.data)
    assertFalse(response.isFromCache)

    // Second query should only hit the cache
    @Suppress("DEPRECATION")
    response = apolloClient.query(query)
        .executeV3()

    assertNotNull(response.data)
    assertTrue(response.isFromCache)

    // Clear the store and offer a malformed response, we should get a composite error
    store.clearAll()
    mockServer.enqueueString("malformed")
    try {
      @Suppress("DEPRECATION")
      apolloClient.query(query)
          .executeV3()
      fail("we expected the query to fail")
    } catch (e: Exception) {
      @Suppress("DEPRECATION")
      assertIs<ApolloCompositeException>(e)
    }
  }


  @Test
  fun cacheFirstToFlowThrowing() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient = apolloClient.newBuilder().build()

    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))
    mockServer.enqueueString(query.composeJsonResponse(data))

    // First query should hit the network and save in cache
    @Suppress("DEPRECATION")
    var responses = apolloClient.query(query)
        .fetchPolicy(FetchPolicy.NetworkFirst)
        .toFlowV3()

    responses.test {
      val response1 = awaitItem()
      assertEquals(data, response1.data)
      assertFalse(response1.isFromCache)
      awaitComplete()
    }

    // Second query should only hit the cache
    @Suppress("DEPRECATION")
    responses = apolloClient.query(query)
        .fetchPolicy(FetchPolicy.CacheFirst)
        .toFlowV3()
    responses.test {
      val response1 = awaitItem()
      assertEquals(data, response1.data)
      assertTrue(response1.isFromCache)
      awaitComplete()
    }

    // Clear the store and offer a malformed response, we should get a composite error
    store.clearAll()
    mockServer.enqueueString("malformed")
    @Suppress("DEPRECATION")
    responses = apolloClient.query(query)
        .fetchPolicy(FetchPolicy.CacheFirst)
        .toFlowV3()
    responses.test {
      @Suppress("DEPRECATION")
      assertIs<ApolloCompositeException>(awaitError())
    }
  }

  @Test
  fun networkFirst() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))

    val call = apolloClient.query(query).fetchPolicy(FetchPolicy.NetworkFirst)

    // First query should hit the network and save in cache
    mockServer.enqueueString(query.composeJsonResponse(data))
    var response = call.execute()

    assertNotNull(response.data)
    assertFalse(response.isFromCache)

    // Now data is cached but it shouldn't be used since network will go through
    mockServer.enqueueString(query.composeJsonResponse(data))
    response = call.execute()

    assertNotNull(response.data)
    assertFalse(response.isFromCache)

    // Network error -> we should hit now the cache
    mockServer.enqueueString("malformed")
    response = call.execute()

    assertNotNull(response.data)
    assertTrue(response.isFromCache)

    // Network error and no cache -> we should get an error
    mockServer.enqueueString("malformed")
    store.clearAll()

    call.execute().exception.let {
      assertIs<JsonEncodingException>(it)
      assertIs<CacheMissException>(it.suppressedExceptions.first())
    }
  }

  @Test
  fun networkFirstExecuteThrowing() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient = apolloClient.newBuilder().build()
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))

    val call = apolloClient.query(query).fetchPolicy(FetchPolicy.NetworkFirst)

    // First query should hit the network and save in cache
    mockServer.enqueueString(query.composeJsonResponse(data))
    var response = call.execute()

    assertNotNull(response.data)
    assertFalse(response.isFromCache)

    // Now data is cached but it shouldn't be used since network will go through
    mockServer.enqueueString(query.composeJsonResponse(data))
    response = call.execute()

    assertNotNull(response.data)
    assertFalse(response.isFromCache)

    // Network error -> we should hit now the cache
    mockServer.enqueueString("malformed")
    response = call.execute()

    assertNotNull(response.data)
    assertTrue(response.isFromCache)

    // Network error and no cache -> we should get an error
    mockServer.enqueueString("malformed")
    store.clearAll()
    try {
      @Suppress("DEPRECATION")
      call.executeV3()
      fail("NETWORK_FIRST should throw the network exception if nothing is in the cache")
    } catch (e: Exception) {

    }
  }

  @Test
  fun networkFirstToFlowThrowing() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient = apolloClient.newBuilder().build()

    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))

    val call = apolloClient.query(query).fetchPolicy(FetchPolicy.NetworkFirst)

    // First query should hit the network and save in cache
    mockServer.enqueueString(query.composeJsonResponse(data))
    @Suppress("DEPRECATION")
    var responses = call.toFlowV3()
    responses.test {
      val response1 = awaitItem()
      assertEquals(data, response1.data)
      assertFalse(response1.isFromCache)
      awaitComplete()
    }

    // Now data is cached but it shouldn't be used since network will go through
    mockServer.enqueueString(query.composeJsonResponse(data))
    @Suppress("DEPRECATION")
    responses = call.toFlowV3()
    responses.test {
      val response1 = awaitItem()
      assertEquals(data, response1.data)
      assertFalse(response1.isFromCache)
      awaitComplete()
    }

    // Network error -> we should hit now the cache
    mockServer.enqueueString("malformed")
    @Suppress("DEPRECATION")
    responses = call.toFlowV3()
    responses.test {
      val response1 = awaitItem()
      assertEquals(data, response1.data)
      assertTrue(response1.isFromCache)
      awaitComplete()
    }

    // Network error and no cache -> we should get an error
    mockServer.enqueueString("malformed")
    store.clearAll()
    @Suppress("DEPRECATION")
    responses = call.toFlowV3()
    responses.test {
      @Suppress("DEPRECATION")
      assertIs<ApolloCompositeException>(awaitError())
    }
  }

  @Test
  fun cacheOnly() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))

    // First query should hit the network and save in cache
    mockServer.enqueueString(query.composeJsonResponse(data))
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
    mockServer.enqueueString(query.composeJsonResponse(data))
    val response = call.execute()

    assertNotNull(response.data)
    assertFalse(response.isFromCache)

    // Offer a malformed response, it should fail
    mockServer.enqueueString("malformed")
    assertNotNull(call.execute().exception)
  }

  @Test
  fun networkOnlyThrowing() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient = apolloClient.newBuilder().build()
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))

    val call = apolloClient.query(query).fetchPolicy(FetchPolicy.NetworkOnly)

    // First query should hit the network and save in cache
    mockServer.enqueueString(query.composeJsonResponse(data))
    @Suppress("DEPRECATION")
    val response = call.executeV3()

    assertNotNull(response.data)
    assertFalse(response.isFromCache)

    // Offer a malformed response, it should fail
    mockServer.enqueueString("malformed")
    try {
      @Suppress("DEPRECATION")
      call.executeV3()
      fail("we expected a failure")
    } catch (_: Exception) {

    }
  }

  @Test
  fun cacheAndNetwork() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))
    var caught: Throwable? = null

    // Initial state: everything fails
    // Cache Error + Network Error => Error
    mockServer.enqueueError(statusCode = 500)
    apolloClient.query(query).fetchPolicy(FetchPolicy.CacheAndNetwork).execute().exception.let {
      assertIs<CacheMissException>(it)
      assertIs<ApolloHttpException>(it.suppressedExceptions.first())
    }

    // Make the network return something
    // Cache Error + Network Success => 2 responses
    mockServer.enqueueString(query.composeJsonResponse(data))
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
    mockServer.enqueueError(statusCode = 500)
    responses = apolloClient.query(query).fetchPolicy(FetchPolicy.CacheAndNetwork).toFlow().catch { caught = it }.toList()

    assertNull(caught)
    assertEquals(2, responses.size)
    assertNotNull(responses[0].data)
    assertTrue(responses[0].isFromCache)
    assertEquals("R2-D2", responses[0].data?.hero?.name)
    assertNull(responses[1].data)
    assertIs<ApolloHttpException>(responses[1].exception)

    // Cache Success + Network Success => 2 responses
    mockServer.enqueueString(query.composeJsonResponse(data))
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
    apolloClient = apolloClient.newBuilder().build()

    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))
    var caught: Throwable? = null
    // Initial state: everything fails
    // Cache Error + Network Error => Error
    mockServer.enqueueError(statusCode = 500)

    @Suppress("DEPRECATION")
    assertFailsWith<ApolloCompositeException> {
      @Suppress("DEPRECATION")
      apolloClient.query(query)
          .fetchPolicy(FetchPolicy.CacheAndNetwork)
          .toFlowV3()
          .toList()
    }

    // Make the network return something
    // Cache Error + Network Success => 1 response (no exception)
    mockServer.enqueueString(query.composeJsonResponse(data))
    @Suppress("DEPRECATION")
    var responses = apolloClient.query(query).fetchPolicy(FetchPolicy.CacheAndNetwork)
        .toFlowV3().catch { caught = it }.toList()

    assertNull(caught)
    assertEquals(1, responses.size)
    assertNotNull(responses[0].data)
    assertFalse(responses[0].isFromCache)
    assertEquals("R2-D2", responses[0].data?.hero?.name)

    // Now cache is populated but make the network fail again
    // Cache Success + Network Error => 1 response + 1 network exception
    caught = null
    mockServer.enqueueError(statusCode = 500)
    @Suppress("DEPRECATION")
    responses = apolloClient.query(query).fetchPolicy(FetchPolicy.CacheAndNetwork).toFlowV3().catch { caught = it }.toList()

    assertIs<ApolloException>(caught)
    assertEquals(1, responses.size)
    assertNotNull(responses[0].data)
    assertTrue(responses[0].isFromCache)
    assertEquals("R2-D2", responses[0].data?.hero?.name)

    // Cache Success + Network Success => 1 response
    mockServer.enqueueString(query.composeJsonResponse(data))
    @Suppress("DEPRECATION")
    responses = apolloClient.query(query).fetchPolicy(FetchPolicy.CacheAndNetwork).toFlowV3().toList()

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
    mockServer.enqueueString(
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
    channel.assertNoElement()

    mockServer.enqueueString(
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

    var response = channel.awaitElement()
    assertTrue(response.isFromCache)
    assertEquals("Leila", response.data?.hero?.name)

    /**
     * clear the cache and trigger the watcher again
     */
    store.clearAll()

    mockServer.enqueueString(
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
    response = channel.awaitElement()
    assertFalse(response.isFromCache)
    assertEquals("Chewbacca", response.data?.hero?.name)

    /**
     * Check that 3 network requests have been made
     */
    mockServer.awaitRequest()
    mockServer.awaitRequest()
    mockServer.awaitRequest()

    job.cancel()
    channel.cancel()
  }

  @Test
  fun isFromCache() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))
    mockServer.enqueueString(query.composeJsonResponse(data))

    // NetworkOnly / hit
    var response = apolloClient.query(query)
        .fetchPolicy(FetchPolicy.NetworkOnly)
        .execute()
    assertNotNull(response.data)
    assertFalse(response.isFromCache)

    // CacheOnly / hit
    response = apolloClient.query(query)
        .fetchPolicy(FetchPolicy.CacheOnly)
        .execute()
    assertNotNull(response.data)
    assertTrue(response.isFromCache)

    // CacheOnly / miss
    store.clearAll()
    response = apolloClient.query(query)
        .fetchPolicy(FetchPolicy.CacheOnly)
        .execute()
    assertNull(response.data)
    assertTrue(response.isFromCache)

    // NetworkOnly / miss
    mockServer.enqueueString("malformed")
    response = apolloClient.query(query)
        .fetchPolicy(FetchPolicy.NetworkOnly)
        .execute()
    assertNull(response.data)
    assertFalse(response.isFromCache)

    // CacheFirst / miss / miss
    store.clearAll()
    mockServer.enqueueString("malformed")
    var responses = apolloClient.query(query)
        .fetchPolicy(FetchPolicy.CacheFirst)
        .toFlow()
        .toList()
    assertTrue(responses[0].isFromCache)
    assertFalse(responses[1].isFromCache)

    // NetworkFirst / miss / miss
    store.clearAll()
    mockServer.enqueueString("malformed")
    responses = apolloClient.query(query)
        .fetchPolicy(FetchPolicy.NetworkFirst)
        .toFlow()
        .toList()
    assertFalse(responses[0].isFromCache)
    assertTrue(responses[1].isFromCache)

    // CacheAndNetwork / hit / hit
    mockServer.enqueueString(query.composeJsonResponse(data))
    responses = apolloClient.query(query)
        .fetchPolicy(FetchPolicy.CacheAndNetwork)
        .toFlow()
        .toList()
    assertTrue(responses[0].isFromCache)
    assertFalse(responses[1].isFromCache)
  }
}
