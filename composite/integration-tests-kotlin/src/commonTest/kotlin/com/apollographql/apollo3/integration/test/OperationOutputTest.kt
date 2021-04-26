package com.apollographql.apollo3.integration.test

import com.apollographql.apollo3.integration.httpcache.AllFilmsQuery
import com.apollographql.apollo3.integration.readFile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class OperationOutputTest {
  @Test
  fun operationOutputMatchesTheModels() {
    val operationOutput = readFile("build/generated/operationOutput/apollo/httpcache/operationOutput.json")
    val source = Json.parseToJsonElement(operationOutput).jsonObject.entries.mapNotNull {
      val descriptor = it.value.jsonObject
      if (descriptor.getValue("name").jsonPrimitive.content == "AllFilms") {
        descriptor.getValue("source").jsonPrimitive.content
      } else {
        null
      }
    }.single()

    assertEquals(AllFilmsQuery().document(), source)
  }
}