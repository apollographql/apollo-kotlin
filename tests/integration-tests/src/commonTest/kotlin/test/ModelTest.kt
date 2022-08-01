package test

import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.testing.internal.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ModelTest {
  @Test
  fun equalsAndHashCodeAreCorrect() = runTest {
    val query1 = HeroNameQuery()
    val query2 = HeroNameQuery()
    assertTrue(query1.equals(query2))
    kotlin.test.assertEquals(query1.hashCode(), query2.hashCode())
  }
}
