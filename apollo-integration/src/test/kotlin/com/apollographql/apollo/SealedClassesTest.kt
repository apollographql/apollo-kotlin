package com.apollographql.apollo

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.cache.normalized.internal.RealCacheKeyBuilder
import com.apollographql.apollo.integration.sealedclasses.type.Direction
import com.google.common.truth.Truth
import org.junit.Test


class SealedClassesTest {

  // see https://github.com/apollographql/apollo-android/issues/2775
  @Test
  fun `cache keys are correct for sealed classes`() {

    val arguments = mapOf("direction" to
        mapOf(
            "kind" to "Variable",
            "variableName" to "direction"
        )
    )

    val field = ResponseField(
        ResponseField.Type.Named.Other("String"),
        "path",
        "path",
        arguments,
        emptyList(),
        emptyMap())


    val variables = object : Operation.Variables() {
      override fun valueMap() = mapOf("direction" to Direction.NORTH)
    }
    val cacheKey = RealCacheKeyBuilder().build(field, variables)
    Truth.assertThat(cacheKey).isEqualTo("path({\"direction\":\"NORTH\"})")
  }
}
