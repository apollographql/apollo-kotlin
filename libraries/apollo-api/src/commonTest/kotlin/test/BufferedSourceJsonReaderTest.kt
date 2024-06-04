package test

import com.apollographql.apollo3.api.json.BufferedSourceJsonReader
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

  @Test
  fun intmin() {
    val json = "-2147483648"

    val jsonReader = BufferedSourceJsonReader(Buffer().writeUtf8(json))

    val number = jsonReader.nextInt()

    assertEquals(Int.MIN_VALUE, number)
  }

  @Test
  fun intmax() {
    val json = "2147483647"

    val jsonReader = BufferedSourceJsonReader(Buffer().writeUtf8(json))

    val number = jsonReader.nextInt()

    assertEquals(Int.MAX_VALUE, number)
  }

  @Test
  fun longmin() {
    val json = "-9223372036854775808"

    val jsonReader = BufferedSourceJsonReader(Buffer().writeUtf8(json))

    val number = jsonReader.nextLong()

    assertEquals(Long.MIN_VALUE, number)
  }

  @Test
  fun longmax() {
    val json = "9223372036854775807"

    val jsonReader = BufferedSourceJsonReader(Buffer().writeUtf8(json))

    val number = jsonReader.nextLong()

    assertEquals(Long.MAX_VALUE, number)
  }

  @Test
  fun doublemin() {
    val json = "4.9E-324"

    val jsonReader = BufferedSourceJsonReader(Buffer().writeUtf8(json))

    val number = jsonReader.nextDouble()

    assertEquals(Double.MIN_VALUE, number)
  }

  @Test
  fun doublemax() {
    val json = "1.7976931348623157E308"

    val jsonReader = BufferedSourceJsonReader(Buffer().writeUtf8(json))

    val number = jsonReader.nextDouble()

    assertEquals(Double.MAX_VALUE, number)
  }
}