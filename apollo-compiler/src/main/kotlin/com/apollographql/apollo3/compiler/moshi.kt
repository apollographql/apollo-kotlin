package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.introspection.IntrospectionSchema
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.InputStream

val MOSHI: Moshi by lazy {
  Moshi.Builder().build()
}

inline fun <reified T> getJsonAdapter() = MOSHI.adapter(T::class.java)

inline fun <reified T> getJsonListAdapter(): JsonAdapter<List<T>> {
  val type = Types.newParameterizedType(List::class.java, T::class.java)
  return MOSHI.adapter(type)
}

inline fun <reified T> String.fromJson(): T {
  return getJsonAdapter<T>().fromJson(this)!!
}

inline fun <reified T> JsonReader.fromJson(): T {
  return getJsonAdapter<T>().fromJson(this)!!
}

inline fun <reified T> InputStream.fromJson(): T {
  use {
    return getJsonAdapter<T>().fromJson(it.source().buffer())!!
  }
}

inline fun <reified T> File.fromJson(): T {
  return inputStream().fromJson()
}

inline fun <reified T> String.fromJsonList(): List<T> {
  return getJsonListAdapter<T>().fromJson(this)!!
}

inline fun <reified T> InputStream.fromJsonList(): List<T> {
  use {
    return getJsonListAdapter<T>().fromJson(it.source().buffer())!!
  }
}

inline fun <reified T> File.fromJsonList(): List<T> {
  return inputStream().fromJsonList<T>()
}

inline fun <reified T> T.toJson(indent: String = ""): String {
  return getJsonAdapter<T>().indent(indent).toJson(this)
}

inline fun <reified T> T.toJson(file: File, indent: String = "") {
  file.outputStream().sink().buffer().use {
    return getJsonAdapter<T>().indent(indent).toJson(it, this)
  }
}
