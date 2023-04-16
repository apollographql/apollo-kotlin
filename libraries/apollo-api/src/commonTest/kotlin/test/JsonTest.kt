package test

import com.apollographql.apollo3.api.AnyApolloAdapter
import com.apollographql.apollo3.api.ApolloAdapter.DataSerializeContext
import com.apollographql.apollo3.api.LongApolloAdapter
import com.apollographql.apollo3.api.ScalarAdapters
import com.apollographql.apollo3.api.json.MapJsonWriter
import com.apollographql.apollo3.api.json.buildJsonString
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonTest {
  @Test
  fun longAdapterWritesNumbers() {
    val json = buildJsonString {
      LongApolloAdapter.toJson(this, Long.MAX_VALUE, DataSerializeContext(ScalarAdapters.Empty))
    }
    assertEquals("9223372036854775807", json)
  }

  @Test
  fun longAdapterInBufferedObjectWritesNumbers() {
    val mapWriter = MapJsonWriter()
    mapWriter.value(Long.MAX_VALUE)

    val json = buildJsonString {
      AnyApolloAdapter.toJson(this, mapWriter.root()!!, DataSerializeContext(ScalarAdapters.Empty))
    }
    assertEquals("9223372036854775807", json)
  }
}
