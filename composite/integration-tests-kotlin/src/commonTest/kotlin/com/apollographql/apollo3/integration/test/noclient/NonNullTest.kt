package com.apollographql.apollo3.integration.test.noclient

import com.apollographql.apollo3.api.fromJson
import com.apollographql.apollo3.integration.normalizer.NonNullHeroQuery
import com.apollographql.apollo3.integration.normalizer.NullableHeroQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class NonNullTest {
  private val responseData = """
      {
        "hero": null
      }
    """.trimIndent()

  @Test
  fun failsWithAnnotation() {
    try {
      NonNullHeroQuery().fromJson(responseData)
      fail("An exception was expected")
    } catch (e: Exception) {
      // We might want a more personalized message at some point
      check(e.message?.contains("but was NULL at path hero") == true)
    }
  }

  @Test
  fun succeedsWithoutAnnotation() {
    val data = NullableHeroQuery().fromJson(responseData)
    assertEquals(null, data.hero)
  }
}