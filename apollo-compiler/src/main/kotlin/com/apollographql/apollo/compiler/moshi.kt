package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.InputStream

val MOSHI: Moshi by lazy {
  Moshi.Builder()
      .add(
          PolymorphicJsonAdapterFactory.of(IntrospectionSchema.Type::class.java, "kind")
              .withSubtype(IntrospectionSchema.Type.Scalar::class.java, IntrospectionSchema.Kind.SCALAR.name)
              .withSubtype(IntrospectionSchema.Type.Object::class.java, IntrospectionSchema.Kind.OBJECT.name)
              .withSubtype(IntrospectionSchema.Type.Interface::class.java, IntrospectionSchema.Kind.INTERFACE.name)
              .withSubtype(IntrospectionSchema.Type.Union::class.java, IntrospectionSchema.Kind.UNION.name)
              .withSubtype(IntrospectionSchema.Type.Enum::class.java, IntrospectionSchema.Kind.ENUM.name)
              .withSubtype(IntrospectionSchema.Type.InputObject::class.java, IntrospectionSchema.Kind.INPUT_OBJECT.name)
      )
      .build()
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

inline fun <reified T> T.toJson(): String {
  return getJsonAdapter<T>().toJson(this)
}

inline fun <reified T> T.toJson(file: File) {
  file.outputStream().sink().buffer().use {
    return getJsonAdapter<T>().toJson(it, this)
  }
}
