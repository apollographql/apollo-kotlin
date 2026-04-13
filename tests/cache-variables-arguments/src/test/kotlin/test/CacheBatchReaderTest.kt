@file:Suppress("DEPRECATION")

package test

import cache.include.TwoFieldsQuery
import cache.include.fragment.UserId
import cache.include.fragment.UserName
import com.apollographql.apollo.cache.normalized.ApolloStore
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * See https://github.com/apollographql/apollo-kotlin/issues/6901
 */
class CacheBatchReaderTest {
  @Test
  fun fieldsWithIncludeDirectiveDoNotOverwriteEachOther(): Unit = runBlocking {
    val operation = TwoFieldsQuery(true)
    val store = ApolloStore(MemoryCacheFactory())
    store.writeOperation(operation, TwoFieldsQuery.Data(__typename = "Query", userId = UserId(UserId.User("42")), userName = UserName(UserName.User("foo"))))

    val data = store.readOperation(operation)
    assertEquals("foo", data.userName.user!!.name)
  }
}
