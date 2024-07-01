package com.example

import com.apollographql.apollo3.api.Optional

suspend fun main() {
  val present = Optional.Present("a")
  val absent = Optional.Absent
  val optional = Optional.presentIfNotNull("a")
}
