package com.apollographql.ijplugin.normalizedcache

import com.apollographql.ijplugin.normalizedcache.NormalizedCache.Companion.RecordKeyComparator
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class NormalizedCacheTest {
  @Test
  fun testRecordKeyComparator(@TestParameter(valuesProvider = ParametersProvider::class) parameter: Parameter) {
    val actual = RecordKeyComparator.compare(
        NormalizedCache.Record(parameter.key1, emptyList(), 0),
        NormalizedCache.Record(parameter.key2, emptyList(), 0),
    ).let {
      if (it < 0) -1 else if (it > 0) 1 else 0
    }
    assertEquals(
        parameter.expectedResult,
        actual
    )
  }

  data class Parameter(
      val key1: String,
      val key2: String,
      val expectedResult: Int,
  )

  class ParametersProvider : TestParameter.TestParameterValuesProvider {
    override fun provideValues(): List<Parameter> = listOf(
        Parameter(key1 = "QUERY_ROOT", key2 = "person", expectedResult = -1),
        Parameter(key1 = "person", key2 = "QUERY_ROOT", expectedResult = 1),
        Parameter(key1 = "person", key2 = "person", expectedResult = 0),
        Parameter(key1 = "person.1", key2 = "person.2", expectedResult = -1),
        Parameter(key1 = "person.2", key2 = "person.1", expectedResult = 1),
        Parameter(key1 = "person.1", key2 = "person.10", expectedResult = -1),
        Parameter(key1 = "person.10", key2 = "person.1", expectedResult = 1),
        Parameter(key1 = "person.2", key2 = "person.10", expectedResult = -1),
        Parameter(key1 = "person.10", key2 = "person.2", expectedResult = 1),
        Parameter(key1 = "person", key2 = "person.2", expectedResult = -1),
        Parameter(key1 = "person.2", key2 = "person", expectedResult = 1),
        Parameter(key1 = "person.10", key2 = "person.2.address", expectedResult = 1),
        Parameter(key1 = "person.1.friend.2", key2 = "person1.friend.3", expectedResult = -1),
        Parameter(key1 = "person.1.friend.3", key2 = "person.1.friend.2", expectedResult = 1),
        Parameter(key1 = "person.9.friend.9", key2 = "person.10.friend.10", expectedResult = -1),
        Parameter(key1 = "person.10.friend.10", key2 = "person.9.friend.9", expectedResult = 1),
        Parameter(key1 = "person.2.friend.10", key2 = "person.10.friend.2", expectedResult = -1),
        Parameter(key1 = "person.2.friend.10", key2 = "person.2.friend.10", expectedResult = 0),
        Parameter(key1 = "person2friend10", key2 = "person10friend2", expectedResult = -1),
        Parameter(key1 = "person02", key2 = "person2", expectedResult = 1),
        Parameter(key1 = "person4", key2 = "person02", expectedResult = 1),
        Parameter(key1 = "person02friend10", key2 = "person2friend2", expectedResult = 1),
    )
  }
}
