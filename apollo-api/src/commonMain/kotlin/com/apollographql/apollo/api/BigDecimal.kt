package com.apollographql.apollo.api

expect class BigDecimal(value: String?) {
  fun toInt(): Int
  fun toLong(): Long
  fun toDouble(): Double
}
