@file:Suppress("DEPRECATION")

import com.apollographql.apollo.adapter.DateAdapter
import com.apollographql.apollo.adapter.JavaInstantAdapter
import com.apollographql.apollo.adapter.JavaLocalDateAdapter
import com.apollographql.apollo.adapter.JavaLocalDateTimeAdapter
import com.apollographql.apollo.adapter.JavaLocalTimeAdapter
import com.apollographql.apollo.adapter.JavaOffsetDateTimeAdapter
import com.apollographql.apollo.adapter.KotlinxInstantAdapter
import com.apollographql.apollo.adapter.KotlinxLocalDateAdapter
import com.apollographql.apollo.adapter.KotlinxLocalDateTimeAdapter
import com.apollographql.apollo.adapter.KotlinxLocalTimeAdapter
import com.apollographql.apollo.api.Adapter
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.json.buildJsonString
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

  private fun <T> Adapter<T>.fromJson(string: String): T {
    return fromJson(string.jsonReader(), CustomScalarAdapters.Empty)
  }

  private fun <T> Adapter<T>.toJson(value: T): String {
    return buildJsonString {
      toJson(this, CustomScalarAdapters.Empty, value)
    }.removePrefix("\"")
        .removeSuffix("\"")
  }

  @Test
  fun instant() {
    var instant = JavaInstantAdapter.fromJson("2010-06-01T22:19:44.475Z")
    assertEquals(1275430784475, instant.toEpochMilli())
    assertEquals("2010-06-01T22:19:44.475Z", JavaInstantAdapter.toJson(instant))

    instant = JavaInstantAdapter.fromJson("2010-06-01T23:19:44.475+01:00")
    assertEquals(1275430784475, instant.toEpochMilli())
    // Time zone is lost
    assertEquals("2010-06-01T22:19:44.475Z", JavaInstantAdapter.toJson(instant))
  }

  @Test
  fun offsetDateTime() {
    var offsetDateTime = JavaOffsetDateTimeAdapter.fromJson("2010-06-01T23:19:44.475+01:00")
    assertEquals(1275430784475, offsetDateTime.toInstant().toEpochMilli())
    // Offset is retained
    assertEquals("2010-06-01T23:19:44.475+01:00", JavaOffsetDateTimeAdapter.toJson(offsetDateTime))

    offsetDateTime = JavaOffsetDateTimeAdapter.fromJson("2010-06-01T22:19:44.475Z")
    assertEquals(1275430784475, offsetDateTime.toInstant().toEpochMilli())
    assertEquals("2010-06-01T22:19:44.475Z", JavaOffsetDateTimeAdapter.toJson(offsetDateTime))
  }

  @Test
  fun date() {
    var date = DateAdapter.fromJson("2010-06-01T22:19:44.475Z")
    assertEquals(1275430784475, date.time)
    assertEquals("2010-06-01T22:19:44.475Z", DateAdapter.toJson(date))

    date = DateAdapter.fromJson("2010-06-01T23:19:44.475+01:00")
    assertEquals(1275430784475, date.time)
    // Time zone is lost
    assertEquals("2010-06-01T22:19:44.475Z", DateAdapter.toJson(date))
  }

  @Test
  fun localDateTime() {
    val localDateTime = JavaLocalDateTimeAdapter.fromJson("2010-06-01T22:19:44.475")
    assertEquals(1275430784, localDateTime.toEpochSecond(ZoneOffset.UTC))
    assertEquals("2010-06-01T22:19:44.475", JavaLocalDateTimeAdapter.toJson(localDateTime))
  }

  @Test
  fun localDate() {
    val localDate = JavaLocalDateAdapter.fromJson("2010-06-01")
    assertEquals(1275430784, localDate.atTime(LocalTime.parse("22:19:44.475")).toEpochSecond(ZoneOffset.UTC))
    assertEquals("2010-06-01", JavaLocalDateAdapter.toJson(localDate))
  }

  @Test
  fun localTime() {
    val localTime = JavaLocalTimeAdapter.fromJson("14:35:20")
    assertEquals(14, localTime.hour)
    assertEquals(35, localTime.minute)
    assertEquals(20, localTime.second)
    assertEquals("14:35:20", JavaLocalTimeAdapter.toJson(localTime))
  }

  @Test
  fun kotlinxInstant() {
    var instant = KotlinxInstantAdapter.fromJson("2010-06-01T22:19:44.475Z")
    assertEquals(1275430784475, instant.toEpochMilliseconds())
    assertEquals("2010-06-01T22:19:44.475Z", KotlinxInstantAdapter.toJson(instant))

    instant = KotlinxInstantAdapter.fromJson("2010-06-01T23:19:44.475+01:00")
    assertEquals(1275430784475, instant.toEpochMilliseconds())
    // Time zone is lost
    assertEquals("2010-06-01T22:19:44.475Z", KotlinxInstantAdapter.toJson(instant))
  }

  @Test
  fun kotlinxLocalDateTime() {
    val localDateTime = KotlinxLocalDateTimeAdapter.fromJson("2010-06-01T22:19:44.475")
    assertEquals(1275430784, localDateTime.toInstant(TimeZone.UTC).epochSeconds)
    assertEquals("2010-06-01T22:19:44.475", KotlinxLocalDateTimeAdapter.toJson(localDateTime))
  }

  @Test
  fun kotlinxLocalDate() {
    val localDate = KotlinxLocalDateAdapter.fromJson("2010-06-01")
    assertEquals(1275430784, localDate.atTime(22, 19, 44).toInstant(TimeZone.UTC).epochSeconds)
    assertEquals("2010-06-01", KotlinxLocalDateAdapter.toJson(localDate))
  }

  @Test
  fun kotlinxLocalTime() {
    val localTime = KotlinxLocalTimeAdapter.fromJson("14:35:20")
    assertEquals(14, localTime.hour)
    assertEquals(35, localTime.minute)
    assertEquals(20, localTime.second)
    assertEquals("14:35:20", KotlinxLocalTimeAdapter.toJson(localTime))
  }

}
