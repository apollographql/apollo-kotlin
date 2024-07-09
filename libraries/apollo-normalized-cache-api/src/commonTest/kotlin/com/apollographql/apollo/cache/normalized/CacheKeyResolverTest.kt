package com.apollographql.apollo.cache.normalized

import com.apollographql.apollo.api.CompiledField
import com.apollographql.apollo.api.CompiledListType
import com.apollographql.apollo.api.Executable
import com.apollographql.apollo.api.ObjectType
import com.apollographql.apollo.cache.normalized.CacheKeyResolverTest.Fixtures.TEST_LIST_FIELD
import com.apollographql.apollo.cache.normalized.CacheKeyResolverTest.Fixtures.TEST_SIMPLE_FIELD
import com.apollographql.apollo.cache.normalized.api.CacheKey
import com.apollographql.apollo.cache.normalized.api.CacheKeyResolver
import com.apollographql.apollo.exception.CacheMissException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail


class CacheKeyResolverTest {

  private lateinit var subject: CacheKeyResolver
  lateinit var onCacheKeyForField: (field: CompiledField, variables: Executable.Variables) -> CacheKey?
  lateinit var onListOfCacheKeysForField: (field: CompiledField, variables: Executable.Variables) -> List<CacheKey?>?

  @BeforeTest
  fun setup() {
    subject = FakeCacheKeyResolver()
    onCacheKeyForField = { _, _ ->
      fail("Unexpected call to cacheKeyForField")
    }
    onListOfCacheKeysForField = { _, _ ->
      fail("Unexpected call to listOfCacheKeysForField")
    }
  }

  @Test
  fun verify_cacheKeyForField_called_for_named_composite_field() {
    val expectedKey = CacheKey("test")
    val fields = mutableListOf<CompiledField>()

    onCacheKeyForField = { field: CompiledField, _: Executable.Variables ->
      fields += field
      expectedKey
    }

    val returned = subject.resolveField(TEST_SIMPLE_FIELD, Executable.Variables(emptyMap()), emptyMap(), "")

    assertEquals(returned, expectedKey)
    assertEquals(fields[0], TEST_SIMPLE_FIELD)
  }

  @Test
  fun listOfCacheKeysForField_called_for_list_field() {
    val expectedKeys = listOf(CacheKey("test"))
    val fields = mutableListOf<CompiledField>()

    onListOfCacheKeysForField = { field: CompiledField, _: Executable.Variables ->
      fields += field
      expectedKeys
    }

    val returned = subject.resolveField(TEST_LIST_FIELD, Executable.Variables(emptyMap()), emptyMap(), "")

    assertEquals(returned, expectedKeys)
    assertEquals(fields[0], TEST_LIST_FIELD)
  }

  @Test
  fun super_called_for_null_return_values() {
    onCacheKeyForField = { _, _ -> null }
    onListOfCacheKeysForField = { _, _ -> null }

    // The best way to ensure that super was called is to check for a cache miss exception from CacheResolver()
    assertFailsWith<CacheMissException> {
      subject.resolveField(TEST_SIMPLE_FIELD, Executable.Variables(emptyMap()), emptyMap(), "")
    }
    assertFailsWith<CacheMissException> {
      subject.resolveField(TEST_LIST_FIELD, Executable.Variables(emptyMap()), emptyMap(), "")
    }
  }

  inner class FakeCacheKeyResolver : CacheKeyResolver() {

    override fun cacheKeyForField(field: CompiledField, variables: Executable.Variables): CacheKey? {
      return onCacheKeyForField(field, variables)
    }

    override fun listOfCacheKeysForField(field: CompiledField, variables: Executable.Variables): List<CacheKey?>? {
      return onListOfCacheKeysForField(field, variables)
    }
  }

  object Fixtures {

    private val TEST_TYPE = ObjectType.Builder(name = "Test").keyFields(listOf("id")).build()

    val TEST_SIMPLE_FIELD = CompiledField.Builder(name = "test", type = TEST_TYPE).build()

    val TEST_LIST_FIELD = CompiledField.Builder(name = "testList", type = CompiledListType(ofType = TEST_TYPE)).build()
  }
}
