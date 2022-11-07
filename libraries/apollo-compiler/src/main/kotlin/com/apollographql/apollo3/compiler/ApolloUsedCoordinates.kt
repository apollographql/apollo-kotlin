package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.toUtf8
import com.apollographql.apollo3.compiler.codegen.ResolverInfo
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import okio.buffer
import okio.sink
import okio.source
import java.io.File

private val anyAdapter = Moshi.Builder()
    .build()
    .adapter(Any::class.java)

fun File.toUsedCoordinates(): Set<String> {
  val result = source().buffer().use {
    anyAdapter.fromJson(it)
  }

  check(result is List<*>) {
    "Apollo: cannot read coordinates: $result"
  }

  @Suppress("UNCHECKED_CAST")
  return result.toSet() as Set<String>
}

fun Set<String>.writeTo(file: File) {
  file.sink().buffer().use {
    anyAdapter.toJson(it, this.toList())
  }
}