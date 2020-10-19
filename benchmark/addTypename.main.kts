#!/usr/bin/env kotlin
@file:DependsOn("com.squareup.okio:okio:2.9.0")
@file:DependsOn("com.squareup.moshi:moshi:1.11.0")

import com.squareup.moshi.Moshi
import okio.buffer
import okio.sink
import okio.source
import java.io.File

val adapter = Moshi.Builder().build().adapter(Any::class.java)
val map = File("src/androidTest/resources/raw/largesample.json").source().buffer()
    .use {
      adapter.fromJson(it)
    }


fun addTypeName(any: Any?): Any? {
  println(any!!.javaClass.name)
  return when (any) {
    is List<*> -> any.map { addTypeName(it) }
    is Map<*, *> -> {
      val typename = when {
        any.containsKey("_id") -> "User"
        any.containsKey("last") -> "Name"
        any.containsKey("name") && !any.containsKey("is_active")-> "Friend"
        any.containsKey("url") -> "Image"
        else -> "Query"
      }
      mutableMapOf<String, Any>().apply {
        put("__typename", typename)
        putAll(any.mapValues { addTypeName(it.value) } as Map<String, Any>)
      }
    }
    else -> any
  }
}

File("src/androidTest/resources/raw/largesampleWithTypename.json").sink().buffer().use {
  val newMap = addTypeName(map)!! as Map<String, Any>
  adapter.toJson(it, newMap)
}

map.cast<Map<String, Any>>()
fun <T> Any?.cast() = this as T