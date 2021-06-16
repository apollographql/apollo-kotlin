package test.declarativecache

import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.CacheResolver
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.testing.runWithMainLoop
import declarativecache.GetBookQuery
import declarativecache.GetOtherBookQuery
import declarativecache.GetOtherLibraryQuery
import declarativecache.GetPromoBookQuery
import declarativecache.GetPromoLibraryQuery
import kotlin.test.Test
import kotlin.test.assertEquals

class DeclarativeCacheTest {

  @Test
  fun atKeyIsWorking() = runWithMainLoop {
    val store = ApolloStore(MemoryCacheFactory(), CacheResolver())

    val promoOperation = GetPromoBookQuery()
    val promoData = GetPromoBookQuery.Data(promoBook = GetPromoBookQuery.PromoBook(__typename =  "Book", title = "Promo", isbn = "42"))
    store.writeOperation(promoOperation, promoData)

    val otherOperation = GetOtherBookQuery()
    val otherData = GetOtherBookQuery.Data(otherBook = GetOtherBookQuery.OtherBook(__typename =  "Book", title = "Other", isbn = "42"))
    store.writeOperation(otherOperation, otherData)

    val data = store.readOperation(promoOperation, CustomScalarAdapters.Empty)

    assertEquals("Other", data?.promoBook?.title)
  }

  @Test
  fun fallbackIdIsWorking() = runWithMainLoop {
    val store = ApolloStore(MemoryCacheFactory(), CacheResolver())

    val promoOperation = GetPromoLibraryQuery()
    val promoData = GetPromoLibraryQuery.Data(promoLibrary = GetPromoLibraryQuery.PromoLibrary(__typename =  "Library", id = "3", address = "PromoAddress"))
    store.writeOperation(promoOperation, promoData)

    val otherOperation = GetOtherLibraryQuery()
    val otherData = GetOtherLibraryQuery.Data(otherLibrary = GetOtherLibraryQuery.OtherLibrary(__typename =  "Library", id = "3", address = "OtherAddress"))
    store.writeOperation(otherOperation, otherData)

    val data = store.readOperation(promoOperation, CustomScalarAdapters.Empty)

    assertEquals("OtherAddress", data?.promoLibrary?.address)
  }

  @Test
  fun resolveFieldIsWorking() = runWithMainLoop {
    val store = ApolloStore(MemoryCacheFactory(), CacheResolver())

    val promoOperation = GetPromoBookQuery()
    val promoData = GetPromoBookQuery.Data(promoBook = GetPromoBookQuery.PromoBook(__typename =  "Book", title = "Promo", isbn = "42"))
    store.writeOperation(promoOperation, promoData)

    val operation = GetBookQuery(isbn = "42")
    val data = store.readOperation(operation, CustomScalarAdapters.Empty)

    assertEquals("Promo", data?.book?.title)
  }
}