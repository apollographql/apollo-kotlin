package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Error.Builder
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.cache.normalized.ApolloStore
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.api.CacheHeaders
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.apolloStore
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.optimisticUpdates
import com.apollographql.apollo.cache.normalized.store
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.exception.CacheMissException
import com.apollographql.apollo.network.NetworkTransport
import com.apollographql.apollo.testing.internal.runTest
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.assertNoRequest
import com.apollographql.mockserver.awaitRequest
import com.apollographql.mockserver.enqueueError
import com.apollographql.mockserver.enqueueMultipart
import com.apollographql.mockserver.enqueueStrings
import com.benasher44.uuid.uuid4
import defer.SimpleDeferQuery
import defer.WithFragmentSpreadsMutation
import defer.WithFragmentSpreadsQuery
import defer.fragment.ComputerFields
import defer.fragment.ScreenFields
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import okio.ByteString.Companion.encodeUtf8
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

  private fun tearDown() {
    mockServer.close()
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
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"},{"__typename":"Computer","id":"Computer2"}]},"pending":[{"id":"0","path":["computers",0]},{"id":"1","path":["computers",1]}],"hasNext":true}""",
        """{"hasNext":true,"pending":[{"id":"2","path":["computers",0,"screen"],"label":"a"},{"id":"3","path":["computers",1,"screen"],"label":"a"}],"incremental":[{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"id":"0"},{"data":{"cpu":"486","year":1996,"screen":{"__typename":"Screen","resolution":"800x600"}},"id":"1"},{"data":{"isColor":false},"id":"2"},{"data":{"isColor":true},"id":"3"}],"completed":[{"id":"0"},{"id":"1"},{"id":"2"},{"id":"3"}]}""",
    )
    mockServer.enqueueMultipart("application/json").enqueueStrings(jsonList)
    apolloClient.query(WithFragmentSpreadsQuery()).fetchPolicy(FetchPolicy.NetworkOnly).toFlow().collect()
    mockServer.awaitRequest()

    // Cache is not empty, so this doesn't go to the server
    val cacheActual = apolloClient.query(WithFragmentSpreadsQuery()).execute().dataOrThrow()
    mockServer.assertNoRequest()

    // We get the last/fully formed data
    val cacheExpected = WithFragmentSpreadsQuery.Data(
        listOf(
            WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
            ComputerFields.Screen("Screen", "640x480",
                ScreenFields(false)
            )
            )
            ),
            WithFragmentSpreadsQuery.Computer("Computer", "Computer2", ComputerFields("486", 1996,
                ComputerFields.Screen("Screen", "800x600",
                    ScreenFields(true)
                )
            )
            ),
        )
    )
    assertEquals(cacheExpected, cacheActual)
  }

  @Test
  fun networkOnly() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient = apolloClient.newBuilder().fetchPolicy(FetchPolicy.NetworkOnly).build()

    // Fill the cache by doing a first request
    val jsonList = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"},{"__typename":"Computer","id":"Computer2"}]},"pending":[{"id":"0","path":["computers",0]},{"id":"1","path":["computers",1]}],"hasNext":true}""",
        """{"hasNext":true,"pending":[{"id":"2","path":["computers",0,"screen"],"label":"a"},{"id":"3","path":["computers",1,"screen"],"label":"a"}],"incremental":[{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"id":"0"},{"data":{"cpu":"486","year":1996,"screen":{"__typename":"Screen","resolution":"800x600"}},"id":"1"},{"data":{"isColor":false},"id":"2"},{"data":{"isColor":true},"id":"3"}],"completed":[{"id":"0"},{"id":"1"},{"id":"2"},{"id":"3"}]}""",
    )
    mockServer.enqueueMultipart("application/json").enqueueStrings(jsonList)
    apolloClient.query(WithFragmentSpreadsQuery()).fetchPolicy(FetchPolicy.NetworkOnly).toFlow().collect()
    mockServer.awaitRequest()

    // Cache is not empty, but NetworkOnly still goes to the server
    mockServer.enqueueMultipart("application/json").enqueueStrings(jsonList)
    val networkActual = apolloClient.query(WithFragmentSpreadsQuery()).toFlow().toList().map { it.dataOrThrow() }
    mockServer.awaitRequest()

    val networkExpected = listOf(
        WithFragmentSpreadsQuery.Data(
            listOf(
                WithFragmentSpreadsQuery.Computer("Computer", "Computer1", null),
                WithFragmentSpreadsQuery.Computer("Computer", "Computer2", null),
            )
        ),
        WithFragmentSpreadsQuery.Data(
            listOf(
                WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                    ComputerFields.Screen("Screen", "640x480",
                        ScreenFields(false)
                    )
                )
                ),
                WithFragmentSpreadsQuery.Computer("Computer", "Computer2", ComputerFields("486", 1996,
                    ComputerFields.Screen("Screen", "800x600",
                        ScreenFields(true)
                    )
                )
                ),
            )
        ),
    )
    assertEquals(networkExpected, networkActual)
  }

  @Test
  fun cacheFirst() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient = apolloClient.newBuilder().fetchPolicy(FetchPolicy.CacheFirst).build()

    val jsonList = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"},{"__typename":"Computer","id":"Computer2"}]},"pending":[{"id":"0","path":["computers",0]},{"id":"1","path":["computers",1]}],"hasNext":true}""",
        """{"hasNext":true,"pending":[{"id":"2","path":["computers",0,"screen"],"label":"a"},{"id":"3","path":["computers",1,"screen"],"label":"a"}],"incremental":[{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"id":"0"},{"data":{"cpu":"486","year":1996,"screen":{"__typename":"Screen","resolution":"800x600"}},"id":"1"},{"data":{"isColor":false},"id":"2"},{"data":{"isColor":true},"id":"3"}],"completed":[{"id":"0"},{"id":"1"},{"id":"2"},{"id":"3"}]}""",
    )
    mockServer.enqueueMultipart("application/json").enqueueStrings(jsonList)

    // Cache is empty, so this goes to the server
    val responses = apolloClient.query(WithFragmentSpreadsQuery()).toFlow().toList()
    assertTrue(responses[0].exception is CacheMissException)
    val networkActual = responses.drop(1).map { it.dataOrThrow() }
    mockServer.awaitRequest()

    val networkExpected = listOf(
        WithFragmentSpreadsQuery.Data(
            listOf(
                WithFragmentSpreadsQuery.Computer("Computer", "Computer1", null),
                WithFragmentSpreadsQuery.Computer("Computer", "Computer2", null),
            )
        ),
        WithFragmentSpreadsQuery.Data(
            listOf(
                WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                    ComputerFields.Screen("Screen", "640x480",
                        ScreenFields(false)
                    )
                )
                ),
                WithFragmentSpreadsQuery.Computer("Computer", "Computer2", ComputerFields("486", 1996,
                    ComputerFields.Screen("Screen", "800x600",
                        ScreenFields(true)
                    )
                )
                ),
            )
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
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"},{"__typename":"Computer","id":"Computer2"}]},"pending":[{"id":"0","path":["computers",0]},{"id":"1","path":["computers",1]}],"hasNext":true}""",
        """{"hasNext":true,"pending":[{"id":"2","path":["computers",0,"screen"],"label":"a"},{"id":"3","path":["computers",1,"screen"],"label":"a"}],"incremental":[{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"id":"0"},{"data":{"cpu":"486","year":1996,"screen":{"__typename":"Screen","resolution":"800x600"}},"id":"1"},{"data":{"isColor":false},"id":"2"},{"data":{"isColor":true},"id":"3"}],"completed":[{"id":"0"},{"id":"1"},{"id":"2"},{"id":"3"}]}""",
    )
    mockServer.enqueueMultipart("application/json").enqueueStrings(jsonList)

    // Cache is empty, so this goes to the server
    val networkActual = apolloClient.query(WithFragmentSpreadsQuery()).toFlow().toList().map { it.dataOrThrow() }
    mockServer.awaitRequest()

    val networkExpected = listOf(
        WithFragmentSpreadsQuery.Data(
            listOf(
                WithFragmentSpreadsQuery.Computer("Computer", "Computer1", null),
                WithFragmentSpreadsQuery.Computer("Computer", "Computer2", null),
            )
        ),
        WithFragmentSpreadsQuery.Data(
            listOf(
                WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                    ComputerFields.Screen("Screen", "640x480",
                        ScreenFields(false)
                    )
                )
                ),
                WithFragmentSpreadsQuery.Computer("Computer", "Computer2", ComputerFields("486", 1996,
                    ComputerFields.Screen("Screen", "800x600",
                        ScreenFields(true)
                    )
                )
                ),
            )
        ),
    )
    assertEquals(networkExpected, networkActual)

    mockServer.enqueueError(statusCode = 500)
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
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"}]},"pending":[{"id":"0","path":["computers",0]}],"hasNext":true}""",
        """{"hasNext":true,"pending":[{"id":"2","path":["computers",0,"screen"],"label":"a"}],"incremental":[{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"id":"0"},{"data":{"isColor":false},"id":"2"}],"completed":[{"id":"0"},{"id":"2"}]}""",
    )
    mockServer.enqueueMultipart("application/json").enqueueStrings(jsonList1)

    // Cache is empty
    val responses = apolloClient.query(WithFragmentSpreadsQuery()).toFlow().toList()
    assertTrue(responses[0].exception is CacheMissException)
    val networkActual = responses.drop(1).map { it.dataOrThrow() }
    mockServer.awaitRequest()

    val networkExpected = listOf(
        WithFragmentSpreadsQuery.Data(
            listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer1", null))
        ),
        WithFragmentSpreadsQuery.Data(
            listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                ComputerFields.Screen("Screen", "640x480",
                    ScreenFields(false)))))
        ),
    )
    assertEquals(networkExpected, networkActual)

    val jsonList2 = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer2"}]},"pending":[{"id":"0","path":["computers",0]}],"hasNext":true}""",
        """{"hasNext":true,"pending":[{"id":"2","path":["computers",0,"screen"],"label":"a"}],"incremental":[{"data":{"cpu":"486","year":1996,"screen":{"__typename":"Screen","resolution":"800x600"}},"id":"0"},{"data":{"isColor":true},"id":"2"}],"completed":[{"id":"0"},{"id":"2"}]}""",
    )
    mockServer.enqueueMultipart("application/json").enqueueStrings(jsonList2)

    // Cache is not empty
    val cacheAndNetworkActual = apolloClient.query(WithFragmentSpreadsQuery()).toFlow().toList().map { it.dataOrThrow() }
    mockServer.awaitRequest()

    // We get a combination of the last/fully formed data from the cache + the new network data
    val cacheAndNetworkExpected = listOf(
        networkExpected.last(),

        WithFragmentSpreadsQuery.Data(
            listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer2", null))
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
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"},{"__typename":"Computer","id":"Computer2"}]},"pending":[{"id":"0","path":["computers",0]},{"id":"1","path":["computers",1]}],"hasNext":true}""",
        """{"hasNext":false,"pending":[{"id":"2","path":["computers",0,"screen"],"label":"a"},{"id":"3","path":["computers",1,"screen"],"label":"a"}],"incremental":[{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"id":"0"},{"data":{"cpu":"486","year":1996,"screen":{"__typename":"Screen","resolution":"800x600"}},"id":"1"},{"data":{"isColor":true},"id":"3"}],"completed":[{"id":"0"},{"id":"1"},{"id":"2","errors":[{"message":"Error field","locations":[{"line":3,"column":35}],"path":["computers",0,"screen","isColor"]}]},{"id":"3"}]}""",
    )
    mockServer.enqueueMultipart("application/json").enqueueStrings(jsonList)

    // Cache is empty, so this goes to the server
    val networkActual = apolloClient.query(WithFragmentSpreadsQuery()).toFlow().toList().drop(1)
    mockServer.awaitRequest()

    val query = WithFragmentSpreadsQuery()
    val uuid = uuid4()

    val networkExpected = listOf(
        ApolloResponse.Builder(
            query,
            uuid,
        ).data(WithFragmentSpreadsQuery.Data(
            listOf(
                WithFragmentSpreadsQuery.Computer("Computer", "Computer1", null),
                WithFragmentSpreadsQuery.Computer("Computer", "Computer2", null),
            )
        )
        ).build(),


        ApolloResponse.Builder(
            query,
            uuid,
        ).data(
            WithFragmentSpreadsQuery.Data(
                listOf(
                    WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                        ComputerFields.Screen("Screen", "640x480", null)
                    )
                    ),
                    WithFragmentSpreadsQuery.Computer("Computer", "Computer2", ComputerFields("486", 1996,
                        ComputerFields.Screen("Screen", "800x600",
                            ScreenFields(true)
                        )
                    )
                    ),
                )
            )
        ).errors(
            listOf(
                Builder("Error field")
                    .locations(listOf(Error.Location(3, 35)))
                    .path(listOf("computers", 0, "screen", "isColor"))
                    .build()
            )
        ).build()
    )
    assertResponseListEquals(networkExpected, networkActual)

    mockServer.enqueueError(statusCode = 500)
    // Because of the error the cache is missing some fields, so we get a cache miss, and fallback to the network (which also fails)
    val exception = apolloClient.query(WithFragmentSpreadsQuery()).execute().exception
    check(exception is CacheMissException)
    assertIs<ApolloHttpException>(exception.suppressedExceptions.first())
    assertEquals("Object 'computers.0' has no field named 'cpu'", exception.message)
    mockServer.awaitRequest()
  }

  @Test
  fun networkFirstWithNetworkError() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = WithFragmentSpreadsQuery()
    val uuid = uuid4()
    val networkResponses = listOf(
        ApolloResponse.Builder(
            query,
            uuid,
        ).data(WithFragmentSpreadsQuery.Data(
            listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer1", null))
        )).build(),

        ApolloResponse.Builder(
            query,
            uuid,
        ).data(WithFragmentSpreadsQuery.Data(
            listOf(WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                ComputerFields.Screen("Screen", "640x480", null))))
        )).build(),
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
                  emit(ApolloResponse.Builder(requestUuid = uuid, operation = query)
                      .exception(ApolloNetworkException("Network error"))
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
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"},{"__typename":"Computer","id":"Computer2"}]},"pending":[{"id":"0","path":["computers",0],"label":"c"},{"id":"1","path":["computers",1],"label":"c"}],"hasNext":true}""",
        """{"hasNext":false,"pending":[{"id":"2","path":["computers",0,"screen"],"label":"a"},{"id":"3","path":["computers",1,"screen"],"label":"a"}],"incremental":[{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"id":"0"},{"data":{"cpu":"486","year":1996,"screen":{"__typename":"Screen","resolution":"800x600"}},"id":"1"},{"data":{"isColor":false},"id":"2"},{"data":{"isColor":true},"id":"3"}],"completed":[{"id":"0"},{"id":"1"},{"id":"2"},{"id":"3"}]}""",
    )
    mockServer.enqueueMultipart("application/json").enqueueStrings(jsonList)
    val networkActual = apolloClient.mutation(WithFragmentSpreadsMutation()).toFlow().toList().map { it.dataOrThrow() }
    mockServer.awaitRequest()

    val networkExpected = listOf(
        WithFragmentSpreadsMutation.Data(
            listOf(
                WithFragmentSpreadsMutation.Computer("Computer", "Computer1", null),
                WithFragmentSpreadsMutation.Computer("Computer", "Computer2", null),
            )
        ),
        WithFragmentSpreadsMutation.Data(
            listOf(WithFragmentSpreadsMutation.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                ComputerFields.Screen("Screen", "640x480",
                    ScreenFields(false)
                )
            )
            ),
                WithFragmentSpreadsMutation.Computer("Computer", "Computer2", ComputerFields("486", 1996,
                    ComputerFields.Screen("Screen", "800x600",
                        ScreenFields(true)
                    )
                )
                )
            )
        ),
    )
    assertEquals(networkExpected, networkActual)

    // Now cache is not empty
    val cacheActual = apolloClient.query(WithFragmentSpreadsQuery()).fetchPolicy(FetchPolicy.CacheOnly).execute().dataOrThrow()

    // We get the last/fully formed data
    val cacheExpected = WithFragmentSpreadsQuery.Data(
        listOf(
            WithFragmentSpreadsQuery.Computer("Computer", "Computer1", ComputerFields("386", 1993,
                ComputerFields.Screen("Screen", "640x480",
                    ScreenFields(false)
                )
            )
            ),
            WithFragmentSpreadsQuery.Computer("Computer", "Computer2", ComputerFields("486", 1996,
                ComputerFields.Screen("Screen", "800x600",
                    ScreenFields(true)
                )
            )
            ),
        )
    )
    assertEquals(cacheExpected, cacheActual)
  }

  @Test
  fun mutationWithOptimisticDataFails() = runTest(before = { setUp() }, after = { tearDown() }) {
    val jsonList = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"},{"__typename":"Computer","id":"Computer2"}]},"pending":[{"id":"0","path":["computers",0],"label":"c"},{"id":"1","path":["computers",1],"label":"c"}],"hasNext":true}""",
        """{"hasNext":false,"pending":[{"id":"2","path":["computers",0,"screen"],"label":"a"},{"id":"3","path":["computers",1,"screen"],"label":"a"}],"incremental":[{"data":{"cpu":"386","year":1993,"screen":{"__typename":"Screen","resolution":"640x480"}},"id":"0"},{"data":{"cpu":"486","year":1996,"screen":{"__typename":"Screen","resolution":"800x600"}},"id":"1"},{"data":{"isColor":false},"id":"2"},{"data":{"isColor":true},"id":"3"}],"completed":[{"id":"0"},{"id":"1"},{"id":"2"},{"id":"3"}]}""",
    )
    mockServer.enqueueMultipart("application/json").enqueueStrings(jsonList)
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

  @Test
  fun intermediatePayloadsAreCached() = runTest(before = { setUp() }, after = { tearDown() }) {
    @Suppress("DEPRECATION")
    if (com.apollographql.apollo.testing.platform() == com.apollographql.apollo.testing.Platform.Js) {
      // TODO For now chunked is not supported on JS - remove this check when it is
      return@runTest
    }
    val jsonList = listOf(
        """{"data":{"computers":[{"__typename":"Computer","id":"Computer1"},{"__typename":"Computer","id":"Computer2"}]},"pending":[{"id":"0","path":["computers",0]},{"id":"1","path":["computers",1]}],"hasNext":true}""",
        """{"hasNext":false,"incremental":[{"data":{"cpu":"386"},"id":"0"},{"data":{"cpu":"486"},"id":"1"}],"completed":[{"id":"0"},{"id":"1"}]}""",
    )
    val multipartBody = mockServer.enqueueMultipart("application/json")
    multipartBody.enqueuePart(jsonList[0].encodeUtf8(), false)
    val recordFields = apolloClient.query(SimpleDeferQuery()).fetchPolicy(FetchPolicy.NetworkOnly).toFlow().map {
      apolloClient.apolloStore.accessCache { it.loadRecord("computers.0", CacheHeaders.NONE)!!.fields }.also {
        multipartBody.enqueuePart(jsonList[1].encodeUtf8(), true)
      }
    }.toList()
    assertEquals(mapOf("__typename" to "Computer", "id" to "Computer1"), recordFields[0])
    assertEquals(mapOf("__typename" to "Computer", "id" to "Computer1", "cpu" to "386"), recordFields[1])
  }
}
