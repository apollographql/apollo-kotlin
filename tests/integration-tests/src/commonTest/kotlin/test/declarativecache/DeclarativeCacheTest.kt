package test.declarativecache

import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.leafType
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.CacheResolver
import com.apollographql.apollo3.cache.normalized.FieldPolicyCacheResolver
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.testing.runTest
import declarativecache.GetBookQuery
import declarativecache.GetBooksQuery
import declarativecache.GetOtherBookQuery
import declarativecache.GetOtherLibraryQuery
import declarativecache.GetPromoBookQuery
import declarativecache.GetPromoLibraryQuery
import kotlin.test.Test
import kotlin.test.assertEquals

class DeclarativeCacheTest {

  @Test
  fun typePolicyIsWorking() = runTest {
    val store = ApolloStore(MemoryCacheFactory())

    // Write a book at the "promo" path
    val promoOperation = GetPromoBookQuery()
    val promoData = GetPromoBookQuery.Data(promoBook = GetPromoBookQuery.PromoBook(__typename =  "Book", title = "Promo", isbn = "42"))
    store.writeOperation(promoOperation, promoData)

    // Overwrite the book title through the "other" path
    val otherOperation = GetOtherBookQuery()
    val otherData = GetOtherBookQuery.Data(otherBook = GetOtherBookQuery.OtherBook(__typename =  "Book", title = "Other", isbn = "42"))
    store.writeOperation(otherOperation, otherData)

    // Get the "promo" book again, the title must be updated
    val data = store.readOperation(promoOperation, CustomScalarAdapters.Empty)

    assertEquals("Other", data?.promoBook?.title)
  }

  @Test
  fun fallbackIdIsWorking() = runTest {
    val store = ApolloStore(MemoryCacheFactory())

    // Write a library at the "promo" path
    val promoOperation = GetPromoLibraryQuery()
    val promoData = GetPromoLibraryQuery.Data(promoLibrary = GetPromoLibraryQuery.PromoLibrary(__typename =  "Library", id = "3", address = "PromoAddress"))
    store.writeOperation(promoOperation, promoData)

    // Overwrite the library address through the "other" path
    val otherOperation = GetOtherLibraryQuery()
    val otherData = GetOtherLibraryQuery.Data(otherLibrary = GetOtherLibraryQuery.OtherLibrary(__typename =  "Library", id = "3", address = "OtherAddress"))
    store.writeOperation(otherOperation, otherData)

    // Get the "promo" library again, the address must be updated
    val data = store.readOperation(promoOperation, CustomScalarAdapters.Empty)

    assertEquals("OtherAddress", data?.promoLibrary?.address)
  }

  @Test
  fun fieldPolicyIsWorking() = runTest {
    val store = ApolloStore(MemoryCacheFactory())

    val promoOperation = GetPromoBookQuery()
    val promoData = GetPromoBookQuery.Data(promoBook = GetPromoBookQuery.PromoBook(__typename =  "Book", title = "Promo", isbn = "42"))
    store.writeOperation(promoOperation, promoData)

    val operation = GetBookQuery(isbn = "42")
    val data = store.readOperation(operation, CustomScalarAdapters.Empty)

    assertEquals("Promo", data?.book?.title)
  }

  @Test
  fun canResolveListProgrammatically() = runTest {
    val cacheResolver = object : CacheResolver {
      override fun resolveField(field: CompiledField, variables: Executable.Variables, parent: Map<String, Any?>, parentId: String): Any? {
        if (field.name == "books") {
          @Suppress("UNCHECKED_CAST")
          val isbns = field.resolveArgument("isbns", variables) as? List<String>
          if (isbns != null) {
            return isbns.map { CacheKey.from(field.type.leafType().name, listOf(it))}
          }
        }

        return FieldPolicyCacheResolver.resolveField(field, variables, parent, parentId)
      }
    }
    val store = ApolloStore(MemoryCacheFactory(), cacheResolver = cacheResolver)

    val promoOperation = GetPromoBookQuery()
    store.writeOperation(promoOperation, GetPromoBookQuery.Data(promoBook = GetPromoBookQuery.PromoBook(__typename =  "Book", title = "Title1", isbn = "1")))
    store.writeOperation(promoOperation, GetPromoBookQuery.Data(promoBook = GetPromoBookQuery.PromoBook(__typename =  "Book", title = "Title2", isbn = "2")))
    store.writeOperation(promoOperation, GetPromoBookQuery.Data(promoBook = GetPromoBookQuery.PromoBook(__typename =  "Book", title = "Title3", isbn = "3")))
    store.writeOperation(promoOperation, GetPromoBookQuery.Data(promoBook = GetPromoBookQuery.PromoBook(__typename =  "Book", title = "Title4", isbn = "4")))

    var operation = GetBooksQuery(isbns = listOf("4", "1"))
    var data = store.readOperation(operation, CustomScalarAdapters.Empty)

    assertEquals("Title4", data?.books?.get(0)?.title)
    assertEquals("Title1", data?.books?.get(1)?.title)

    operation = GetBooksQuery(isbns = listOf("3"))
    data = store.readOperation(operation, CustomScalarAdapters.Empty)

    assertEquals("Title3", data?.books?.get(0)?.title)
  }
}