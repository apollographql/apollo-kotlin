package test.circular_cache_read

import circular_cache_read.GetUserQuery
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.testing.internal.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CircularCacheReadTest {
  @Test
  fun circularReferenceDoesNotStackOverflow() = runTest {
    val store = ApolloStore(MemoryCacheFactory())

    val operation = GetUserQuery()

    /**
     * Create a record that references itself. It should not create a stack overflow
     */
    val data = GetUserQuery.Data(
        GetUserQuery.User(
            "42",
            GetUserQuery.Friend(
                "42",
                "User"
            ),
            "User",
        )
    )

    store.writeOperation(operation, data)
    val result = store.readOperation(operation, customScalarAdapters = CustomScalarAdapters.Empty)
    assertEquals("42", result.user.friend.id)
  }
}
