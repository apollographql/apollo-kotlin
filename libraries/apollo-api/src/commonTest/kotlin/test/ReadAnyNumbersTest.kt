@file:OptIn(ApolloInternal::class)

package test

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.json.JsonNumber
import com.apollographql.apollo.api.json.MapJsonReader
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.json.readAny
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `readAny()` maps JSON numbers to Int, Long, Double or JsonNumber, in that order of preference.
 *
 * This is exercised for both readers because they have different number representations.
 */
class ReadAnyNumbersTest {
  private fun fromSource(literal: String): Any? {
    return Buffer().writeUtf8("""{"v":$literal}""").jsonReader().run {
      beginObject()
      nextName()
      readAny().also {
        endObject()
      }
    }
  }

  private fun fromMap(value: Any?): Any? {
    return MapJsonReader(mapOf("v" to value)).run {
      beginObject()
      nextName()
      readAny().also {
        endObject()
      }
    }
  }

  @Test
  fun integralValuesAreNarrowedToInt() {
    assertEquals(0, fromSource("0"))
    assertEquals(42, fromSource("42"))
    assertEquals(-1, fromSource("-1"))
    assertEquals(Int.MAX_VALUE, fromSource("2147483647"))
    assertEquals(Int.MIN_VALUE, fromSource("-2147483648"))

    assertEquals(0, fromMap(0))
    assertEquals(1, fromMap(1L))
    assertEquals(1, fromMap(JsonNumber("1")))
  }

  @Test
  fun valuesThatDoNotFitAnIntAreLongs() {
    assertEquals(2147483648L, fromSource("2147483648"))
    assertEquals(-2147483649L, fromSource("-2147483649"))
    assertEquals(Long.MAX_VALUE, fromSource("9223372036854775807"))
    assertEquals(Long.MIN_VALUE, fromSource("-9223372036854775808"))

    assertEquals(1099511627776L, fromMap(1099511627776L))
    assertEquals(2147483648L, fromMap(JsonNumber("2147483648")))
  }

  @Test
  fun integralValuedDoublesAreNarrowed() {
    // "1.0" and "1e2" are integral values, they are narrowed like any other integral value
    assertEquals(1, fromSource("1.0"))
    assertEquals(0, fromSource("-0.0"))
    assertEquals(100, fromSource("1E+2"))
    assertEquals(1000000000000L, fromSource("1e12"))

    assertEquals(1, fromMap(1.0))
    assertEquals(100, fromMap(1e2))
    assertEquals(1000000000000L, fromMap(1e12))
  }

  @Test
  fun fractionalValuesAreDoubles() {
    assertEquals(1.5, fromSource("1.5"))
    assertEquals(0.1, fromSource("0.1"))
    assertEquals(0.0025, fromSource("2.5e-3"))
    assertEquals(1e100, fromSource("1e100"))
    assertEquals(1e19, fromSource("1e19"))

    assertEquals(1.5, fromMap(1.5))
    assertEquals(1e100, fromMap(1e100))
    assertEquals(1.5, fromMap(JsonNumber("1.5")))
  }

  @Test
  fun valuesThatDoNotFitADoubleAreJsonNumbers() {
    // JsonNumber doesn't implement equals(), compare the raw values
    assertEquals("1e400", (fromSource("1e400") as JsonNumber).value)
    assertEquals("-1e400", (fromSource("-1e400") as JsonNumber).value)

    // MapJsonReader doesn't check for NaN and infinities
    assertEquals(Double.POSITIVE_INFINITY, fromMap(JsonNumber("1e400")))
    assertEquals(Double.NaN, fromMap(Double.NaN))
    // But it keeps values that are not numbers at all
    assertEquals("not a number", (fromMap(JsonNumber("not a number")) as JsonNumber).value)
  }

  @Test
  fun veryLargeValuesLosePrecision() {
    /**
     * XXX: this is not great but has been the behaviour for a long time.
     * See the comment in `guessNumber`.
     */
    assertEquals(1.2345678901234568E29, fromSource("123456789012345678901234567890"))
    assertEquals(Long.MAX_VALUE, fromSource("9223372036854775808"))
  }
}
