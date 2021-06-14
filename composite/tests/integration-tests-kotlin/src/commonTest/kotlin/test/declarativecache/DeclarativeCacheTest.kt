package test.declarativecache

import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.CacheResolver
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.testing.runWithMainLoop
import declarativecache.GetOtherBookQuery
import declarativecache.GetPromoBookQuery
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeclarativeCacheTest {
  private lateinit var store: ApolloStore

  @BeforeTest
  fun setUp() {
    store = ApolloStore(MemoryCacheFactory(), CacheResolver.DEFAULT)
  }


  @Test
  fun atKeyIsWorking() = runWithMainLoop {
    val promoOperation = GetPromoBookQuery()
    val promoData = GetPromoBookQuery.Data(book = GetPromoBookQuery.Data.Book(title = "Promo", isbn = "42"))

    store.writeOperation(promoOperation, promoData)

    val otherOperation = GetOtherBookQuery()
    val otherData = GetOtherBookQuery.Data(otherBook = GetOtherBookQuery.Data.OtherBook(title = "Other", isbn = "42"))

    store.writeOperation(otherOperation, otherData)

    val data = store.readOperation(promoOperation, CustomScalarAdapters.Empty)

    assertEquals("Promo", data?.book?.title)
  //    store.
  }
}