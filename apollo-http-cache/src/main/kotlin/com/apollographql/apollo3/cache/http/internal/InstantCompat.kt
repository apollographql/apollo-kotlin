package com.apollographql.apollo3.cache.http.internal

import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale
import java.util.TimeZone

/**
 * Helper functions to fallback on Java 7 APIs for older Android devices
 */
private fun dateFormat(): SimpleDateFormat {
  return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT).apply {
    timeZone = TimeZone.getTimeZone("UTC")
  }
}

internal fun nowDateMillis(): Long {
  return try {
    Instant.now().toEpochMilli()
  } catch (e: Exception) {
    System.currentTimeMillis()
  }
}

internal fun nowDateString(): String {
  return try {
    Instant.now().toString()
  } catch (e: Exception) {
    return dateFormat().format(System.currentTimeMillis())
  }
}

internal fun parseDateString(dateString: String): Long {
  return try {
    Instant.parse(dateString).toEpochMilli()
  } catch (e: Exception) {
    dateFormat().parse(dateString.replace(Regex("\\.[0-9]*"), "")).time
  }
}