package test

import cache.include.AbsentArgumentWithArgumentDefaultValueQuery
import cache.include.PresentArgumentEmptyQuery
import cache.include.VariableAbsentQuery
import cache.include.VariableDefaultValueEmptyQuery
import cache.include.VariableDefaultValueNullQuery
import cache.include.VariableDefaultValueWithCQuery
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo.cache.normalized.api.normalize
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test to check coercion of various variables/arguments combinations.
 *
 * I am not 100% certain this is all correct but hopefully this is a good base for future
 * improvements
 * See https://github.com/graphql/graphql-spec/pull/793
 */
class CacheArgumentTest {
  @Test
  fun variableDefaultValueEmpty() {
    val operation = VariableDefaultValueEmptyQuery()

    /**
     * One would expect b: 3 here but the default variable is not coerced
     */
    assertEquals("a0({\"b\":{}})", operation.fieldKey(VariableDefaultValueEmptyQuery.Data(a0 = 42)))
  }

  @Test
  fun variableDefaultValueWithC() {
    val operation = VariableDefaultValueWithCQuery()

    /**
     * The default value contains c
     */
    assertEquals("a0({\"b\":{\"c\":4}})", operation.fieldKey(VariableDefaultValueWithCQuery.Data(a0 = 42)))
  }

  @Test
  fun variableDefaultValueNull() {
    val operation = VariableDefaultValueNullQuery()

    /**
     * The default value can be null
     */
    assertEquals("a0({\"b\":null})", operation.fieldKey(VariableDefaultValueNullQuery.Data(a0 = 42)))
  }

  @Test
  fun variableAbsent() {
    val operation = VariableAbsentQuery()

    /**
     * An argument can be absent
     */
    assertEquals("a0", operation.fieldKey(VariableAbsentQuery.Data(a0 = 42)))
  }

  @Test
  fun absentArgumentWithArgumentDefaultValue() {
    val operation = AbsentArgumentWithArgumentDefaultValueQuery()

    /**
     * The argument definition defaultValue is the empty object and is not coerced
     */
    assertEquals("a1", operation.fieldKey(AbsentArgumentWithArgumentDefaultValueQuery.Data(a1 = 42)))
  }

  @Test
  fun presentArgumentEmpty() {
    val operation = PresentArgumentEmptyQuery()

    /**
     * Because here we're passing an argument explicitly, this argument is coerced and the 3 default value gets pulled
     * in even if it was initially not there
     */
    assertEquals("a1({\"b\":{\"c\":3}})", operation.fieldKey(PresentArgumentEmptyQuery.Data(a1 = 42)))
  }
}

private fun <D : Operation.Data> Operation<D>.fieldKey(data: D): String {
  val record = normalize(data, CustomScalarAdapters.Empty, TypePolicyCacheKeyGenerator)

  return record.values.single().keys.single { it.startsWith("a") }
}
