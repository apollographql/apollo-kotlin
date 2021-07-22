package test.circular_cache_read

import circular_cache_read.GetUserQuery
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.testing.runWithMainLoop
import kotlin.test.Test
import kotlin.test.assertEquals

class CircularCacheReadTest {
  @Test
  fun circularReferenceDoesNotStackOverflow() {
    val store = ApolloStore(MemoryCacheFactory())

    val operation = GetUserQuery()
    val data = GetUserQuery.Data(
        user = GetUserQuery.User(
            __typename = "User",
            id = "42",
            friend = GetUserQuery.Friend(
                __typename = "User",
                id = "42"
            )
        )
    )

    runWithMainLoop {
      store.writeOperation(operation, data)
      val result = store.readOperation(operation, customScalarAdapters = CustomScalarAdapters.Empty)
      assertEquals("42", result!!.user.friend.id)
    }
  }
}