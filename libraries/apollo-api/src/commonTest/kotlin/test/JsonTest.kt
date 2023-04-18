package test

import com.apollographql.apollo3.api.AnyApolloAdapter
import com.apollographql.apollo3.api.ApolloAdapter.SerializeDataContext
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
      LongApolloAdapter.serializeData(this, Long.MAX_VALUE, SerializeDataContext(ScalarAdapters.Empty))
    }
    assertEquals("9223372036854775807", json)
  }

  @Test
  fun longAdapterInBufferedObjectWritesNumbers() {
    val mapWriter = MapJsonWriter()
    mapWriter.value(Long.MAX_VALUE)

    val json = buildJsonString {
      AnyApolloAdapter.serializeData(this, mapWriter.root()!!, SerializeDataContext(ScalarAdapters.Empty))
    }
    assertEquals("9223372036854775807", json)
  }
}
