package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.optimisticUpdates
import com.apollographql.apollo3.cache.normalized.store
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.exception.CacheMissException
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.mockserver.enqueueMultipart
import com.apollographql.apollo3.network.NetworkTransport
import com.apollographql.apollo3.testing.internal.runTest
import com.benasher44.uuid.uuid4
import defer.WithFragmentSpreadsMutation
import defer.WithFragmentSpreadsQuery
import defer.fragment.ComputerFields
import defer.fragment.ScreenFields
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DeferNormalizedCacheTest {
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
  fun cacheOnly() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient = apolloClient.newBuilder().fetchPolicy(FetchPolicy.CacheOnly).build()

    // Cache is empty
    assertIs<CacheMissException>(
        apolloClient.query(WithFragmentSpreadsQuery()).execute().exception
    )

    // Fill the cache by doing a network only request
    val jsonList = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"}]},"hasNext":true}""",
        """{"incremental": [{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"path":["computers",0]}],"hasNext":true}""",
        """{"incremental": [{"data":{"isColor":false},"path":["computers",0,"screen"],"label":"a"}],"hasNext":false}""",
    )
    mockServer.enqueueMultipart(jsonList)
    apolloClient.query(WithFragmentSpreadsQuery()).fetchPolicy(FetchPolicy.NetworkOnly).toFlow().collect()
    mockServer.takeRequest()

    // Cache is not empty, so this doesn't go to the server
    val cacheActual = apolloClient.query(WithFragmentSpreadsQuery()).execute().dataOrThrow()
    assertFails { mockServer.takeRequest() }

    // We get the last/fully formed data
    val cacheExpected = WithFragmentSpreadsQuery.Data(
        listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
            ComputerFields.Screen("Screen", "640x480",
                ScreenFields(false)))))
    )
    assertEquals(cacheExpected, cacheActual)
  }

  @Test
  fun networkOnly() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient = apolloClient.newBuilder().fetchPolicy(FetchPolicy.NetworkOnly).build()

    // Fill the cache by doing a first request
    val jsonList = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"}]},"hasNext":true}""",
        """{"incremental": [{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"path":["computers",0]}],"hasNext":true}""",
        """{"incremental": [{"data":{"isColor":false},"path":["computers",0,"screen"],"label":"a"}],"hasNext":false}""",
    )
    mockServer.enqueueMultipart(jsonList)
    apolloClient.query(WithFragmentSpreadsQuery()).fetchPolicy(FetchPolicy.NetworkOnly).toFlow().collect()
    mockServer.takeRequest()

    // Cache is not empty, but NetworkOnly still goes to the server
    mockServer.enqueueMultipart(jsonList)
    val networkActual = apolloClient.query(WithFragmentSpreadsQuery()).toFlow().toList().map { it.dataOrThrow() }
    mockServer.takeRequest()

    val networkExpected = listOf(
        WithFragmentSpreadsQuery.Data(
            listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer1", null))
        ),
        WithFragmentSpreadsQuery.Data(
            listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                ComputerFields.Screen("Screen", "640x480", null))))
        ),
        WithFragmentSpreadsQuery.Data(
            listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                ComputerFields.Screen("Screen", "640x480",
                    ScreenFields(false)))))
        ),
    )
    assertEquals(networkExpected, networkActual)
  }

  @Test
  fun cacheFirst() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient = apolloClient.newBuilder().fetchPolicy(FetchPolicy.CacheFirst).build()

    val jsonList = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"}]},"hasNext":true}""",
        """{"incremental": [{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"path":["computers",0]}],"hasNext":true}""",
        """{"incremental": [{"data":{"isColor":false},"path":["computers",0,"screen"],"label":"a"}],"hasNext":false}""",
    )
    mockServer.enqueueMultipart(jsonList)

    // Cache is empty, so this goes to the server
    val responses = apolloClient.query(WithFragmentSpreadsQuery()).toFlow().toList()
    assertTrue(responses[0].exception is CacheMissException)
    val networkActual = responses.drop(1).map { it.dataOrThrow() }
    mockServer.takeRequest()

    val networkExpected = listOf(
        WithFragmentSpreadsQuery.Data(
            listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer1", null))
        ),
        WithFragmentSpreadsQuery.Data(
            listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                ComputerFields.Screen("Screen", "640x480", null))))
        ),
        WithFragmentSpreadsQuery.Data(
            listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                ComputerFields.Screen("Screen", "640x480",
                    ScreenFields(false)))))
        ),
    )
    assertEquals(networkExpected, networkActual)

    // Cache is not empty, so this doesn't go to the server
    val cacheActual = apolloClient.query(WithFragmentSpreadsQuery()).execute().dataOrThrow()
    assertFails { mockServer.takeRequest() }

    // We get the last/fully formed data
    val cacheExpected = networkExpected.last()
    assertEquals(cacheExpected, cacheActual)
  }

  @Test
  fun networkFirst() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient = apolloClient.newBuilder().fetchPolicy(FetchPolicy.NetworkFirst).build()

    val jsonList = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"}]},"hasNext":true}""",
        """{"incremental": [{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"path":["computers",0]}],"hasNext":true}""",
        """{"incremental": [{"data":{"isColor":false},"path":["computers",0,"screen"],"label":"a"}],"hasNext":false}""",
    )
    mockServer.enqueueMultipart(jsonList)

    // Cache is empty, so this goes to the server
    val networkActual = apolloClient.query(WithFragmentSpreadsQuery()).toFlow().toList().map { it.dataOrThrow() }
    mockServer.takeRequest()

    val networkExpected = listOf(
        WithFragmentSpreadsQuery.Data(
            listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer1", null))
        ),
        WithFragmentSpreadsQuery.Data(
            listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                ComputerFields.Screen("Screen", "640x480", null))))
        ),
        WithFragmentSpreadsQuery.Data(
            listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                ComputerFields.Screen("Screen", "640x480",
                    ScreenFields(false)))))
        ),
    )
    assertEquals(networkExpected, networkActual)

    mockServer.enqueue(statusCode = 500)
    // Network will fail, so we get the cached version
    val cacheActual = apolloClient.query(WithFragmentSpreadsQuery()).execute().dataOrThrow()

    // We get the last/fully formed data
    val cacheExpected = networkExpected.last()
    assertEquals(cacheExpected, cacheActual)
  }

  @Test
  fun cacheAndNetwork() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient = apolloClient.newBuilder().fetchPolicy(FetchPolicy.CacheAndNetwork).build()

    val jsonList1 = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"}]},"hasNext":true}""",
        """{"incremental": [{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"path":["computers",0]}],"hasNext":true}""",
        """{"incremental": [{"data":{"isColor":false},"path":["computers",0,"screen"],"label":"a"}],"hasNext":false}""",
    )
    mockServer.enqueueMultipart(jsonList1)

    // Cache is empty
    val responses = apolloClient.query(WithFragmentSpreadsQuery()).toFlow().toList()
    assertTrue(responses[0].exception is CacheMissException)
    val networkActual = responses.drop(1).map { it.dataOrThrow() }
    mockServer.takeRequest()

    val networkExpected = listOf(
        WithFragmentSpreadsQuery.Data(
            listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer1", null))
        ),
        WithFragmentSpreadsQuery.Data(
            listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                ComputerFields.Screen("Screen", "640x480", null))))
        ),
        WithFragmentSpreadsQuery.Data(
            listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                ComputerFields.Screen("Screen", "640x480",
                    ScreenFields(false)))))
        ),
    )
    assertEquals(networkExpected, networkActual)

    val jsonList2 = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer2"}]},"hasNext":true}""",
        """{"incremental": [{"data":{"cpu":"486","year":1996,"screen":{"__typename":"Screen","resolution":"800x600"}},"path":["computers",0]}],"hasNext":true}""",
        """{"incremental": [{"data":{"isColor":true},"path":["computers",0,"screen"],"label":"a"}],"hasNext":false}""",
    )
    mockServer.enqueueMultipart(jsonList2)

    // Cache is not empty
    val cacheAndNetworkActual = apolloClient.query(WithFragmentSpreadsQuery()).toFlow().toList().map { it.dataOrThrow() }
    mockServer.takeRequest()

    // We get a combination of the last/fully formed data from the cache + the new network data
    val cacheAndNetworkExpected = listOf(
        networkExpected.last(),

        WithFragmentSpreadsQuery.Data(
            listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer2", null))
        ),
        WithFragmentSpreadsQuery.Data(
            listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer2", ComputerFields("486", 1996,
                ComputerFields.Screen("Screen", "800x600", null))))
        ),
        WithFragmentSpreadsQuery.Data(
            listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer2", ComputerFields("486", 1996,
                ComputerFields.Screen("Screen", "800x600",
                    ScreenFields(true)))))
        ),
    )

    assertEquals(cacheAndNetworkExpected, cacheAndNetworkActual)
  }

  @Test
  fun cacheFirstWithMissingFragmentDueToError() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient = apolloClient.newBuilder().fetchPolicy(FetchPolicy.CacheFirst).build()

    val jsonList = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"}]},"hasNext":true}""",
        """{"incremental": [{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"path":["computers",0]}],"hasNext":true}""",
        """{"incremental": [{"data":null,"path":["computers",0,"screen"],"label":"b","errors":[{"message":"Cannot resolve isColor","locations":[{"line":1,"column":119}],"path":["computers",0,"screen","isColor"]}]}],"hasNext":false}""",
    )
    mockServer.enqueueMultipart(jsonList)

    // Cache is empty, so this goes to the server
    val networkActual = apolloClient.query(WithFragmentSpreadsQuery()).toFlow().toList().drop(1)
    mockServer.takeRequest()

    val query = WithFragmentSpreadsQuery()
    val uuid = uuid4()

    val networkExpected = listOf(
        ApolloResponse.Builder(
            query,
            uuid,
            data = WithFragmentSpreadsQuery.Data(
                listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer1", null))
            )
        ).build(),

        ApolloResponse.Builder(
            query,
            uuid,
            data = WithFragmentSpreadsQuery.Data(
                listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                    ComputerFields.Screen("Screen", "640x480", null))))
            )
        ).build(),

        ApolloResponse.Builder(
            query,
            uuid,
            data = WithFragmentSpreadsQuery.Data(
                listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                    ComputerFields.Screen("Screen", "640x480", null))))
            )
        )
            .errors(
                listOf(
                    Error(
                        message = "Cannot resolve isColor",
                        locations = listOf(Error.Location(1, 119)),
                        path = listOf("computers", 0, "screen", "isColor"),
                        extensions = null, nonStandardFields = null
                    )
                )
            )
            .build(),
    )
    assertResponseListEquals(networkExpected, networkActual)

    mockServer.enqueue(statusCode = 500)
    // Because of the error the cache is missing some fields, so we get a cache miss, and fallback to the network (which also fails)
    val exception = assertFailsWith<CacheMissException> {
      apolloClient.query(WithFragmentSpreadsQuery()).execute().dataOrThrow()
    }
    assertIs<ApolloHttpException>(exception.suppressedExceptions.first())
    assertEquals("Object 'computers.0.screen' has no field named 'isColor'", exception.message)
    mockServer.takeRequest()
  }

  @Test
  fun networkFirstWithNetworkError() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = WithFragmentSpreadsQuery()
    val uuid = uuid4()
    val networkResponses = listOf(
        ApolloResponse.Builder(
            query,
            uuid,
            data = WithFragmentSpreadsQuery.Data(
                listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer1", null))
            )
        ).build(),

        ApolloResponse.Builder(
            query,
            uuid,
            data = WithFragmentSpreadsQuery.Data(
                listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                    ComputerFields.Screen("Screen", "640x480", null))))
            )
        ).build(),
    )

    apolloClient = ApolloClient.Builder()
        .store(store)
        .fetchPolicy(FetchPolicy.NetworkFirst)
        .networkTransport(
            object : NetworkTransport {
              @Suppress("UNCHECKED_CAST")
              override fun <D : Operation.Data> execute(request: ApolloRequest<D>): Flow<ApolloResponse<D>> {
                // Emit a few items then an exception
                return flow {
                  for (networkResponse in networkResponses) {
                    emit(networkResponse as ApolloResponse<D>)
                  }
                  delay(10)
                  emit(ApolloResponse.Builder(requestUuid = uuid, operation = query, exception = ApolloNetworkException("Network error"))
                      .isLast(true)
                      .build() as ApolloResponse<D>)
                }
              }

              override fun dispose() {}
            }
        )
        .build()

    // - get the first few responses
    // - an exception happens
    // - fallback to the cache
    // - because of the error the cache is missing some fields, so we get a cache miss
    val actual = apolloClient.query(WithFragmentSpreadsQuery()).toFlow().toList()

    assertResponseListEquals(networkResponses, actual.dropLast(2))
    val networkExceptionResponse = actual[actual.size - 2]
    val cacheExceptionResponse = actual.last()
    assertIs<ApolloNetworkException>(networkExceptionResponse.exception)
    assertIs<CacheMissException>(cacheExceptionResponse.exception)
    assertEquals("Object 'computers.0.screen' has no field named 'isColor'", cacheExceptionResponse.exception!!.message)
  }

  @Test
  fun mutation() = runTest(before = { setUp() }, after = { tearDown() }) {
    val jsonList = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"}]},"hasNext":true}""",
        """{"incremental": [{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"path":["computers",0],"label":"c"}],"hasNext":true}""",
        """{"incremental": [{"data":{"isColor":false},"path":["computers",0,"screen"],"label":"a"}],"hasNext":false}""",
    )
    mockServer.enqueueMultipart(jsonList)
    val networkActual = apolloClient.mutation(WithFragmentSpreadsMutation()).toFlow().toList().map { it.dataOrThrow() }
    mockServer.takeRequest()

    val networkExpected = listOf(
        WithFragmentSpreadsMutation.Data(
            listOf(WithFragmentSpreadsMutation.Computer("Computer", "Computer1", null))
        ),
        WithFragmentSpreadsMutation.Data(
            listOf(WithFragmentSpreadsMutation.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                ComputerFields.Screen("Screen", "640x480", null))))
        ),
        WithFragmentSpreadsMutation.Data(
            listOf(WithFragmentSpreadsMutation.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                ComputerFields.Screen("Screen", "640x480",
                    ScreenFields(false)))))
        ),
    )
    assertEquals(networkExpected, networkActual)

    // Now cache is not empty
    val cacheActual = apolloClient.query(WithFragmentSpreadsQuery()).fetchPolicy(FetchPolicy.CacheOnly).execute().dataOrThrow()

    // We get the last/fully formed data
    val cacheExpected = WithFragmentSpreadsQuery.Data(
        listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
            ComputerFields.Screen("Screen", "640x480",
                ScreenFields(false)))))
    )
    assertEquals(cacheExpected, cacheActual)
  }

  @Test
  fun mutationWithOptimisticDataFails() = runTest(before = { setUp() }, after = { tearDown() }) {
    val jsonList = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"}]},"hasNext":true}""",
        """{"incremental": [{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"path":["computers",0],"label":"c"}],"hasNext":true}""",
        """{"incremental": [{"data":{"isColor":false},"path":["computers",0,"screen"],"label":"a"}],"hasNext":false}""",
    )
    mockServer.enqueueMultipart(jsonList)
    val responses = apolloClient.mutation(WithFragmentSpreadsMutation()).optimisticUpdates(
        WithFragmentSpreadsMutation.Data(
            listOf(WithFragmentSpreadsMutation.Computer("Computer", "Computer1", null))
        )
    ).toFlow()

    val exception = assertFailsWith<ApolloException> {
      responses.collect()
    }
    assertEquals("Apollo: optimistic updates can only be applied with one network response", exception.message)
  }

}
