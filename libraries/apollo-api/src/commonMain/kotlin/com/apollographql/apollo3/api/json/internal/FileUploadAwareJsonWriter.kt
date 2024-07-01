package com.apollographql.apollo.api.json.internal

import com.apollographql.apollo.api.Upload
import com.apollographql.apollo.api.json.JsonNumber
import com.apollographql.apollo.api.json.JsonWriter

/**
 * A [JsonWriter] that can wrap a [JsonWriter] and intercept [Upload] writes. This is used to send
 * upload variables out of band in a multipart/form-data HTTP request
 */
internal class FileUploadAwareJsonWriter(private val wrappedWriter: JsonWriter): JsonWriter {
  private val uploads = mutableMapOf<String, Upload>()

  fun collectedUploads(): Map<String, Upload> = uploads

  override fun beginArray() = apply {
    wrappedWriter.beginArray()
  }

  override fun endArray() = apply {
    wrappedWriter.endArray()
  }

  override fun beginObject() = apply {
    wrappedWriter.beginObject()
  }

  override fun endObject() = apply {
    wrappedWriter.endObject()
  }

  override fun name(name: String) = apply {
    wrappedWriter.name(name)
  }

  override fun value(value: String) = apply {
    wrappedWriter.value(value)
  }

  override fun value(value: Boolean) = apply {
    wrappedWriter.value(value)
  }

  override fun value(value: Double) = apply {
    wrappedWriter.value(value)
  }

  override fun value(value: Int) = apply {
    wrappedWriter.value(value)
  }

  override fun value(value: Long) = apply {
    wrappedWriter.value(value)
  }

  override fun value(value: JsonNumber) = apply {
    wrappedWriter.value(value)
  }

  override fun value(value: Upload) = apply {
    uploads[wrappedWriter.path] = value
    wrappedWriter.nullValue()
  }

  override fun nullValue() = apply {
    wrappedWriter.nullValue()
  }

  override val path: String
    get() = wrappedWriter.path

  override fun close() {
    wrappedWriter.close()
  }

  override fun flush() {
    wrappedWriter.flush()
  }
}