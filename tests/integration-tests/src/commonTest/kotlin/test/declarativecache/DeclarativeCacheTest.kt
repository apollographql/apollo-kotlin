package test.declarativecache

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheResolver
import com.apollographql.apollo3.cache.normalized.api.FieldPolicyCacheResolver
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.testing.runTest
import declarativecache.GetAuthorQuery
import declarativecache.GetBookQuery
import declarativecache.GetBooksQuery
import declarativecache.GetOtherBookQuery
import declarativecache.GetOtherLibraryQuery
import declarativecache.GetPromoAuthorQuery
import declarativecache.GetPromoBookQuery
import declarativecache.GetPromoLibraryQuery
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ApolloExperimental::class)
class DeclarativeCacheTest {

  @Test
  fun typePolicyIsWorking() = runTest {
    val store = ApolloStore(MemoryCacheFactory())

    // Write a book at the "promo" path
    val promoOperation = GetPromoBookQuery()
    val promoData = GetPromoBookQuery.Data(GetPromoBookQuery.PromoBook("Book", "Promo", "42"))
    store.writeOperation(promoOperation, promoData)

    // Overwrite the book title through the "other" path
    val otherOperation = GetOtherBookQuery()
    val otherData = GetOtherBookQuery.Data(GetOtherBookQuery.OtherBook("Book", "42", "Other"))
    store.writeOperation(otherOperation, otherData)

    // Get the "promo" book again, the title must be updated
    val data = store.readOperation(promoOperation, CustomScalarAdapters.Empty)

    assertEquals("Other", data.promoBook?.title)
  }

  @Test
  fun fallbackIdIsWorking() = runTest {
    val store = ApolloStore(MemoryCacheFactory())

    // Write a library at the "promo" path
    val promoOperation = GetPromoLibraryQuery()
    val promoData = GetPromoLibraryQuery.Data(GetPromoLibraryQuery.PromoLibrary("Library", "PromoAddress", "3"))
    store.writeOperation(promoOperation, promoData)

    // Overwrite the library address through the "other" path
    val otherOperation = GetOtherLibraryQuery()
    val otherData = GetOtherLibraryQuery.Data(GetOtherLibraryQuery.OtherLibrary("Library", "3", "OtherAddress"))
    store.writeOperation(otherOperation, otherData)

    // Get the "promo" library again, the address must be updated
    val data = store.readOperation(promoOperation, CustomScalarAdapters.Empty)

    assertEquals("OtherAddress", data.promoLibrary?.address)
  }

  @Test
  fun fieldPolicyIsWorking() = runTest {
    val store = ApolloStore(MemoryCacheFactory())

    val bookQuery1 = GetPromoBookQuery()
    val bookData1 = GetPromoBookQuery.Data(GetPromoBookQuery.PromoBook("Book", "Promo", "42"))
    store.writeOperation(bookQuery1, bookData1)

    val bookQuery2 = GetBookQuery("42")
    val bookData2 = store.readOperation(bookQuery2, CustomScalarAdapters.Empty)

    assertEquals("Promo", bookData2.book?.title)

    val authorQuery1 = GetPromoAuthorQuery()
    val authorData1 = GetPromoAuthorQuery.Data(
        GetPromoAuthorQuery.Author(
            "Author",
            "Pierre",
            "Bordage",
        )
    )

    store.writeOperation(authorQuery1, authorData1)

    val authorQuery2 = GetAuthorQuery("Pierre", "Bordage")
    val authorData2 = store.readOperation(authorQuery2, CustomScalarAdapters.Empty)

    assertEquals("Pierre", authorData2.author?.firstName)
    assertEquals("Bordage", authorData2.author?.lastName)
  }

  @Test
  fun canResolveListProgrammatically() = runTest {
    val cacheResolver = object : CacheResolver {
      override fun resolveField(field: CompiledField, variables: Executable.Variables, parent: Map<String, Any?>, parentId: String): Any? {
        if (field.name == "books") {
          @Suppress("UNCHECKED_CAST")
          val isbns = field.resolveArgument("isbns", variables) as? List<String>
          if (isbns != null) {
            return isbns.map { CacheKey.from(field.type.leafType().name, listOf(it)) }
          }
        }

        return FieldPolicyCacheResolver.resolveField(field, variables, parent, parentId)
      }
    }
    val store = ApolloStore(MemoryCacheFactory(), cacheResolver = cacheResolver)

    val promoOperation = GetPromoBookQuery()
    store.writeOperation(promoOperation, GetPromoBookQuery.Data(GetPromoBookQuery.PromoBook("Book", "Title1", "1")))
    store.writeOperation(promoOperation, GetPromoBookQuery.Data(GetPromoBookQuery.PromoBook("Book", "Title2", "2")))
    store.writeOperation(promoOperation, GetPromoBookQuery.Data(GetPromoBookQuery.PromoBook("Book", "Title3", "3")))
    store.writeOperation(promoOperation, GetPromoBookQuery.Data(GetPromoBookQuery.PromoBook("Book", "Title4", "4")))

    var operation = GetBooksQuery(listOf("4", "1"))
    var data = store.readOperation(operation, CustomScalarAdapters.Empty)

    assertEquals("Title4", data.books.get(0).title)
    assertEquals("Title1", data.books.get(1).title)

    operation = GetBooksQuery(listOf("3"))
    data = store.readOperation(operation, CustomScalarAdapters.Empty)

    assertEquals("Title3", data.books.get(0).title)
  }
}
