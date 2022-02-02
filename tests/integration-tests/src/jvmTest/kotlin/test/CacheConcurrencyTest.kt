package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.store
import com.apollographql.apollo3.integration.normalizer.CharacterNameByIdQuery
import com.apollographql.apollo3.testing.QueueTestNetworkTransport
import com.apollographql.apollo3.testing.enqueueTestResponse
import com.apollographql.apollo3.testing.runTest
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.test.Test

@OptIn(ApolloExperimental::class)
class CacheConcurrencyTest {

  @Test
  fun storeConcurrently() = runTest {
    val store = ApolloStore(MemoryCacheFactory(maxSizeBytes = 1000))
    val executor = Executors.newFixedThreadPool(10)
    val dispatcher = executor.asCoroutineDispatcher()

    val apolloClient = ApolloClient.Builder()
        .networkTransport(QueueTestNetworkTransport())
        .store(store)
        .requestedDispatcher(dispatcher)
        .build()

    val concurrency = 100

    0.until(concurrency).map {
      launch(dispatcher) {
        val query = CharacterNameByIdQuery((it / 2).toString())
        apolloClient.enqueueTestResponse(query, CharacterNameByIdQuery.Data(CharacterNameByIdQuery.Character(name = it.toString())))
        apolloClient.query(query).execute()
      }
    }.joinAll()

    executor.shutdown()
    println(store.dump().values.toList()[1].map { (k, v) -> "$k -> ${v.fields}" }.joinToString("\n"))
  }
}