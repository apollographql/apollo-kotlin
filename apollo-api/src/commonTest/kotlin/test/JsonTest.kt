package test

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.AnyAdapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.LongAdapter
import com.apollographql.apollo3.api.json.MapJsonWriter
import com.apollographql.apollo3.api.json.buildJsonString
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonTest {
  @Test
  fun longAdapterWritesNumbers() {
    @OptIn(ApolloInternal::class)
    val json = buildJsonString {
      LongAdapter.toJson(this, CustomScalarAdapters.Empty, Long.MAX_VALUE)
    }
    assertEquals("9223372036854775807", json)
  }

  @Test
  fun longAdapterInBufferedObjectWritesNumbers() {
    val mapWriter = MapJsonWriter()
    mapWriter.value(Long.MAX_VALUE)

    @OptIn(ApolloInternal::class)
    val json = buildJsonString {
      AnyAdapter.toJson(this, CustomScalarAdapters.Empty, mapWriter.root()!!)
    }
    assertEquals("9223372036854775807", json)
  }
}