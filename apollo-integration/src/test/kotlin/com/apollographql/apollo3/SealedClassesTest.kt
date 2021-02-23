package com.apollographql.apollo3

import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.cache.normalized.internal.RealCacheKeyBuilder
import com.apollographql.apollo3.integration.sealedclasses.SealedClassQuery
import com.apollographql.apollo3.integration.sealedclasses.type.Direction
import com.google.common.truth.Truth
import org.junit.Test


class SealedClassesTest {

  // see https://github.com/apollographql/apollo-android/issues/2775
  @Test
  fun `cache keys are correct for sealed classes`() {

    val operation = SealedClassQuery(Input.Present(Direction.NORTH))
    val variables = operation.variables(ResponseAdapterCache.DEFAULT)
    val cacheKey = RealCacheKeyBuilder().build(operation.responseFields()[0].responseFields[0], variables)
    Truth.assertThat(cacheKey).isEqualTo("path({\"direction\":\"NORTH\"})")
  }
}
