@file:JvmName("Adapters")
@file:Suppress("DEPRECATION")

package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v4_0_0
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.api.json.readAny
import com.apollographql.apollo3.api.json.writeAny
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

@Deprecated("Use StringScalarAdapter instead", ReplaceWith("StringScalarAdapter"))
@ApolloDeprecatedSince(v4_0_0)
@JvmField
val StringAdapter = object : Adapter<String> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): String {
    return reader.nextString()!!
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: String) {
    writer.value(value)
  }
}

@Deprecated("Use IntScalarAdapter instead", ReplaceWith("IntScalarAdapter"))
@ApolloDeprecatedSince(v4_0_0)
@JvmField
val IntAdapter = object : Adapter<Int> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Int {
    return reader.nextInt()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Int) {
    writer.value(value)
  }
}

@Deprecated("Use DoubleScalarAdapter instead", ReplaceWith("DoubleScalarAdapter"))
@ApolloDeprecatedSince(v4_0_0)
@JvmField
val DoubleAdapter = object : Adapter<Double> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Double {
    return reader.nextDouble()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Double) {
    writer.value(value)
  }
}

@Deprecated("Use FloatScalarAdapter instead", ReplaceWith("FloatScalarAdapter"))
@ApolloDeprecatedSince(v4_0_0)
@JvmField
val FloatAdapter = object : Adapter<Float> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Float {
    return reader.nextDouble().toFloat()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Float) {
    writer.value(value.toDouble())
  }
}

@Deprecated("Use LongScalarAdapter instead", ReplaceWith("LongScalarAdapter"))
@ApolloDeprecatedSince(v4_0_0)
@JvmField
val LongAdapter = object : Adapter<Long> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Long {
    return reader.nextLong()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Long) {
    writer.value(value)
  }
}

@Deprecated("Use BooleanScalarAdapter instead", ReplaceWith("BooleanScalarAdapter"))
@ApolloDeprecatedSince(v4_0_0)
@JvmField
val BooleanAdapter = object : Adapter<Boolean> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Boolean {
    return reader.nextBoolean()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Boolean) {
    writer.value(value)
  }
}

@Deprecated("Use AnyScalarAdapter instead", ReplaceWith("AnyScalarAdapter"))
@ApolloDeprecatedSince(v4_0_0)
@JvmField
val AnyAdapter = object : Adapter<Any> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Any {
    return reader.readAny()!!
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Any) {
    writer.writeAny(value)
  }
}

@Deprecated("Use UploadScalarAdapter instead", ReplaceWith("UploadScalarAdapter"))
@ApolloDeprecatedSince(v4_0_0)
@JvmField
val UploadAdapter = object : Adapter<Upload> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Upload {
    error("File Upload used in output position")
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Upload) {
    writer.value(value)
  }
}

@JvmName("-toJson")
@JvmOverloads
fun <T> Adapter<T>.toJsonString(
    value: T,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    indent: String? = null,
): String = buildJsonString(indent) {
  this@toJsonString.toJson(this, customScalarAdapters, value)
}

@Deprecated("Use ScalarAdapter instead of Adapter")
class AdapterToScalarAdapter<T>(private val wrappedAdapter: Adapter<T>) : ScalarAdapter<T> {
  override fun fromJson(reader: JsonReader): T {
    return wrappedAdapter.fromJson(reader, CustomScalarAdapters.Empty)
  }

  override fun toJson(writer: JsonWriter, value: T) {
    wrappedAdapter.toJson(writer, CustomScalarAdapters.Empty, value)
  }
}
