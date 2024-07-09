package test

import com.apollographql.apollo.api.json.BufferedSourceJsonReader
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class BufferedSourceJsonReaderTest {
  @Test
  fun bigdecimal() {
    // Less than 65 zeroes because of https://github.com/square/moshi/issues/1529
    val json = "1000000000000000000000000000000000000000000000000000000000000000"

    val jsonReader = BufferedSourceJsonReader(Buffer().writeUtf8(json))

    val number = jsonReader.nextNumber()

    assertEquals(json, number.value)
  }
}