package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.apolloStore
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.normalizedCache
import cache.include.GetUserQuery
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class IncludeTest {
  @Test
  fun simple() = runBlocking {
    val client = ApolloClient.Builder()
        .normalizedCache(MemoryCacheFactory())
        .serverUrl("https://unused.com")
        .build()

    val operation = GetUserQuery(withDetails = false)

    val data = GetUserQuery.Data(
        user = GetUserQuery.User(__typename = "User", id = "42", userDetails = null)
    )

    client.apolloStore.writeOperationSync(operation, data)

    val response = client.query(operation).fetchPolicy(FetchPolicy.CacheOnly).execute()

    assertEquals("42", response.data?.user?.id)
    assertEquals(null, response.data?.user?.userDetails)
  }
}
