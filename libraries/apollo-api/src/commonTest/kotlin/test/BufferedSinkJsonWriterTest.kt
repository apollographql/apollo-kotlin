package test

import com.apollographql.apollo.api.json.BufferedSinkJsonWriter
import com.apollographql.apollo.api.json.JsonNumber
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class BufferedSinkJsonWriterTest {

  @Test
  fun jsonNumberValue() {
    val testSink = Buffer()
    val tested = BufferedSinkJsonWriter(testSink)
    val testStringNumber = "1.23456"

    tested.value(JsonNumber(testStringNumber))
    tested.flush()

    assertEquals(testStringNumber, testSink.readUtf8())
  }
}
