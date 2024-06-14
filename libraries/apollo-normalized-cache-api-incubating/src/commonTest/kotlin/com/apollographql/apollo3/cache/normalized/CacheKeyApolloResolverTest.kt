package com.apollographql.apollo3.cache.normalized

import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CompiledListType
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.ObjectType
import com.apollographql.apollo3.cache.normalized.CacheKeyApolloResolverTest.Fixtures.TEST_LIST_FIELD
import com.apollographql.apollo3.cache.normalized.CacheKeyApolloResolverTest.Fixtures.TEST_SIMPLE_FIELD
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheKeyApolloResolver
import com.apollographql.apollo3.cache.normalized.api.DefaultFieldKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.ResolverContext
import com.apollographql.apollo3.exception.CacheMissException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail


class CacheKeyApolloResolverTest {

  private lateinit var subject: CacheKeyApolloResolver
  lateinit var onCacheKeyForField: (context: ResolverContext) -> CacheKey?
  lateinit var onListOfCacheKeysForField: (context: ResolverContext) -> List<CacheKey?>?

  @BeforeTest
  fun setup() {
    subject = FakeCacheKeyApolloResolver()
    onCacheKeyForField = { _ ->
      fail("Unexpected call to cacheKeyForField")
    }
    onListOfCacheKeysForField = { _ ->
      fail("Unexpected call to listOfCacheKeysForField")
    }
  }

  private fun resolverContext(field: CompiledField) =
    ResolverContext(field, Executable.Variables(emptyMap()), emptyMap(), "", "", CacheHeaders(emptyMap()), DefaultFieldKeyGenerator)

  @Test
  fun verify_cacheKeyForField_called_for_named_composite_field() {
    val expectedKey = CacheKey("test")
    val fields = mutableListOf<CompiledField>()

    onCacheKeyForField = { context: ResolverContext ->
      fields += context.field
      expectedKey
    }

    val returned = subject.resolveField(resolverContext(TEST_SIMPLE_FIELD))

    assertEquals(returned, expectedKey)
    assertEquals(fields[0], TEST_SIMPLE_FIELD)
  }

  @Test
  fun listOfCacheKeysForField_called_for_list_field() {
    val expectedKeys = listOf(CacheKey("test"))
    val fields = mutableListOf<CompiledField>()

    onListOfCacheKeysForField = { context: ResolverContext ->
      fields += context.field
      expectedKeys
    }

    val returned = subject.resolveField(resolverContext(TEST_LIST_FIELD))

    assertEquals(returned, expectedKeys)
    assertEquals(fields[0], TEST_LIST_FIELD)
  }

  @Test
  fun super_called_for_null_return_values() {
    onCacheKeyForField = { _ -> null }
    onListOfCacheKeysForField = { _ -> null }

    // The best way to ensure that super was called is to check for a cache miss exception from CacheResolver()
    assertFailsWith<CacheMissException> {
      subject.resolveField(resolverContext(TEST_SIMPLE_FIELD))
    }
    assertFailsWith<CacheMissException> {
      subject.resolveField(resolverContext(TEST_LIST_FIELD))
    }
  }

  inner class FakeCacheKeyApolloResolver : CacheKeyApolloResolver() {

    override fun cacheKeyForField(context: ResolverContext): CacheKey? {
      return onCacheKeyForField(context)
    }

    override fun listOfCacheKeysForField(context: ResolverContext): List<CacheKey?>? {
      return onListOfCacheKeysForField(context)
    }
  }

  object Fixtures {

    private val TEST_TYPE = ObjectType.Builder(name = "Test").keyFields(keyFields = listOf("id")).build()

    val TEST_SIMPLE_FIELD = CompiledField.Builder(name = "test", type = TEST_TYPE).build()

    val TEST_LIST_FIELD = CompiledField.Builder(name = "testList", type = CompiledListType(ofType = TEST_TYPE)).build()
  }
}
