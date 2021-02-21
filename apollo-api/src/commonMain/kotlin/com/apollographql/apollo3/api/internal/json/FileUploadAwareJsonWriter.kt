package com.apollographql.apollo3.api.internal.json

import com.apollographql.apollo3.api.FileUpload

class FileUploadAwareJsonWriter(private val wrappedWriter: BufferedSinkJsonWriter): JsonWriter {
  private val uploads = mutableMapOf<String, FileUpload>()

  init {
    wrappedWriter.serializeNulls = true
  }
  fun collectedUploads(): Map<String, FileUpload> = uploads

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

  override fun value(value: FileUpload) = apply {
    uploads[wrappedWriter.path] = value
    wrappedWriter.nullValue()
  }

  override fun nullValue() = apply {
    wrappedWriter.nullValue()
  }

  override fun close() {
    wrappedWriter.nullValue()
  }

  override fun flush() {
    wrappedWriter.flush()
  }
}