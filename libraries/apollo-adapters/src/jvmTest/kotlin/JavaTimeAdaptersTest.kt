import com.apollographql.apollo3.adapter.DateScalarAdapter
import com.apollographql.apollo3.adapter.JavaInstantScalarAdapter
import com.apollographql.apollo3.adapter.JavaLocalDateScalarAdapter
import com.apollographql.apollo3.adapter.JavaLocalDateTimeScalarAdapter
import com.apollographql.apollo3.adapter.JavaLocalTimeScalarAdapter
import com.apollographql.apollo3.adapter.JavaOffsetDateTimeScalarAdapter
import com.apollographql.apollo3.adapter.KotlinxInstantScalarAdapter
import com.apollographql.apollo3.adapter.KotlinxLocalDateScalarAdapter
import com.apollographql.apollo3.adapter.KotlinxLocalDateTimeScalarAdapter
import com.apollographql.apollo3.adapter.KotlinxLocalTimeScalarAdapter
import com.apollographql.apollo3.api.ScalarAdapter
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.json.buildJsonString
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import okio.Buffer
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.test.assertEquals

class JavaTimeAdaptersTest {
  private fun String.jsonReader() = BufferedSourceJsonReader(Buffer().writeUtf8("\"${this}\""))

  private fun <T> ScalarAdapter<T>.fromJson(string: String): T {
    return fromJson(string.jsonReader())
  }

  private fun <T> ScalarAdapter<T>.toJson(value: T): String {
    return buildJsonString {
      toJson(this, value)
    }.removePrefix("\"")
        .removeSuffix("\"")
  }

  @Test
  fun instant() {
    var instant = JavaInstantScalarAdapter.fromJson("2010-06-01T22:19:44.475Z")
    assertEquals(1275430784475, instant.toEpochMilli())
    assertEquals("2010-06-01T22:19:44.475Z", JavaInstantScalarAdapter.toJson(instant))

    instant = JavaInstantScalarAdapter.fromJson("2010-06-01T23:19:44.475+01:00")
    assertEquals(1275430784475, instant.toEpochMilli())
    // Time zone is lost
    assertEquals("2010-06-01T22:19:44.475Z", JavaInstantScalarAdapter.toJson(instant))
  }

  @Test
  fun offsetDateTime() {
    var offsetDateTime = JavaOffsetDateTimeScalarAdapter.fromJson("2010-06-01T23:19:44.475+01:00")
    assertEquals(1275430784475, offsetDateTime.toInstant().toEpochMilli())
    // Offset is retained
    assertEquals("2010-06-01T23:19:44.475+01:00", JavaOffsetDateTimeScalarAdapter.toJson(offsetDateTime))

    offsetDateTime = JavaOffsetDateTimeScalarAdapter.fromJson("2010-06-01T22:19:44.475Z")
    assertEquals(1275430784475, offsetDateTime.toInstant().toEpochMilli())
    assertEquals("2010-06-01T22:19:44.475Z", JavaOffsetDateTimeScalarAdapter.toJson(offsetDateTime))
  }

  @Test
  fun date() {
    var date = DateScalarAdapter.fromJson("2010-06-01T22:19:44.475Z")
    assertEquals(1275430784475, date.time)
    assertEquals("2010-06-01T22:19:44.475Z", DateScalarAdapter.toJson(date))

    date = DateScalarAdapter.fromJson("2010-06-01T23:19:44.475+01:00")
    assertEquals(1275430784475, date.time)
    // Time zone is lost
    assertEquals("2010-06-01T22:19:44.475Z", DateScalarAdapter.toJson(date))
  }

  @Test
  fun localDateTime() {
    val localDateTime = JavaLocalDateTimeScalarAdapter.fromJson("2010-06-01T22:19:44.475")
    assertEquals(1275430784, localDateTime.toEpochSecond(ZoneOffset.UTC))
    assertEquals("2010-06-01T22:19:44.475", JavaLocalDateTimeScalarAdapter.toJson(localDateTime))
  }

  @Test
  fun localDate() {
    val localDate = JavaLocalDateScalarAdapter.fromJson("2010-06-01")
    assertEquals(1275430784, localDate.atTime(LocalTime.parse("22:19:44.475")).toEpochSecond(ZoneOffset.UTC))
    assertEquals("2010-06-01", JavaLocalDateScalarAdapter.toJson(localDate))
  }

  @Test
  fun localTime() {
    val localTime = JavaLocalTimeScalarAdapter.fromJson("14:35:20")
    assertEquals(14, localTime.hour)
    assertEquals(35, localTime.minute)
    assertEquals(20, localTime.second)
    assertEquals("14:35:20", JavaLocalTimeScalarAdapter.toJson(localTime))
  }

  @Test
  fun kotlinxInstant() {
    var instant = KotlinxInstantScalarAdapter.fromJson("2010-06-01T22:19:44.475Z")
    assertEquals(1275430784475, instant.toEpochMilliseconds())
    assertEquals("2010-06-01T22:19:44.475Z", KotlinxInstantScalarAdapter.toJson(instant))

    instant = KotlinxInstantScalarAdapter.fromJson("2010-06-01T23:19:44.475+01:00")
    assertEquals(1275430784475, instant.toEpochMilliseconds())
    // Time zone is lost
    assertEquals("2010-06-01T22:19:44.475Z", KotlinxInstantScalarAdapter.toJson(instant))
  }

  @Test
  fun kotlinxLocalDateTime() {
    val localDateTime = KotlinxLocalDateTimeScalarAdapter.fromJson("2010-06-01T22:19:44.475")
    assertEquals(1275430784, localDateTime.toInstant(TimeZone.UTC).epochSeconds)
    assertEquals("2010-06-01T22:19:44.475", KotlinxLocalDateTimeScalarAdapter.toJson(localDateTime))
  }

  @Test
  fun kotlinxLocalDate() {
    val localDate = KotlinxLocalDateScalarAdapter.fromJson("2010-06-01")
    assertEquals(1275430784, localDate.atTime(22, 19, 44).toInstant(TimeZone.UTC).epochSeconds)
    assertEquals("2010-06-01", KotlinxLocalDateScalarAdapter.toJson(localDate))
  }

  @Test
  fun kotlinxLocalTime() {
    val localTime = KotlinxLocalTimeScalarAdapter.fromJson("14:35:20")
    assertEquals(14, localTime.hour)
    assertEquals(35, localTime.minute)
    assertEquals(20, localTime.second)
    assertEquals("14:35:20", KotlinxLocalTimeScalarAdapter.toJson(localTime))
  }

}
