import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.composeJsonResponse
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import kotlinx.coroutines.runBlocking
import test.GetAnimalQuery
import kotlin.test.Test

class MainTest {
  @Test
  fun test() {
    runBlocking {
      val operation = GetAnimalQuery()
      val server = MockServer()

      val response = buildJsonString {
        operation.composeJsonResponse(this, GetAnimalQuery.Data(
            __typename = "Query",
            animal = GetAnimalQuery.Animal(
                __typename = "Lion",
                species = "Lion",
                onLion = GetAnimalQuery.OnLion(
                    roar = "Rooooar",
                    owner = GetAnimalQuery.Owner(
                        __typename = "Owner",
                        name = "yoyo"
                    )
                ),
                onCat = null
            )
        ))
      }

      server.enqueue(response)


      val client = ApolloClient.Builder()
          .serverUrl(server.url())
          .normalizedCache(MemoryCacheFactory())
          .build()

      client.query(operation).fetchPolicy(FetchPolicy.NetworkOnly).execute().apply {
        check(exception == null)
        check(data?.animal?.onLion?.owner?.name == "yoyo")
      }
      client.query(operation).fetchPolicy(FetchPolicy.CacheOnly).execute().apply {
        check(exception == null)
        check(data?.animal?.onLion?.owner?.name == "yoyo")
      }
      server.stop()

    }
  }
}

