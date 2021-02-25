package com.apollographql.apollo3.api.internal

import com.apollographql.apollo3.api.Upload
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.internal.json.FileUploadAwareJsonWriter
import com.apollographql.apollo3.api.internal.json.use
import com.benasher44.uuid.uuid4
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString
import okio.Source
import kotlin.jvm.JvmStatic

object OperationRequestBodyComposer {
  interface Body {
    val operations: ByteString
    val contentType: String

    fun writeTo(bufferedSink: BufferedSink)
  }

  @JvmStatic
  fun compose(
      operation: Operation<*>,
      autoPersistQueries: Boolean,
      withQueryDocument: Boolean,
      responseAdapterCache: ResponseAdapterCache
  ): Body {
    val buffer = Buffer()
    val jsonWriter = FileUploadAwareJsonWriter(BufferedSinkJsonWriter(buffer))

    jsonWriter.use { writer ->
      with(writer) {
        beginObject()
        name("operationName").value(operation.name())
        name("variables")
        operation.serializeVariables(this, responseAdapterCache)
        if (autoPersistQueries) {
          name("extensions")
          beginObject()
          name("persistedQuery")
          beginObject()
          name("version").value(1)
          name("sha256Hash").value(operation.operationId())
          endObject()
          endObject()
        }
        if (!autoPersistQueries || withQueryDocument) {
          name("query").value(operation.queryDocument())
        }
        endObject()
      }
    }

    buffer.flush()
    val operationByteString = buffer.readByteString()

    val uploads = jsonWriter.collectedUploads()
    if (uploads.isEmpty()) {
      return object : Body {
        override val operations = operationByteString
        override val contentType = "application/json"

        override fun writeTo(bufferedSink: BufferedSink) {
          bufferedSink.write(operationByteString)
        }
      }
    } else {
      return object : Body {
        private val boundary = uuid4().toString()

        override val operations = operationByteString
        override val contentType = "multipart/form-data; boundary=$boundary"

        override fun writeTo(bufferedSink: BufferedSink) {
          bufferedSink.writeUtf8("--$boundary\r\n")
          bufferedSink.writeUtf8("Content-Disposition: form-data; name=\"operations\"\r\n")
          bufferedSink.writeUtf8("Content-Type: application/json\r\n")
          bufferedSink.writeUtf8("Content-Length: ${operationByteString.size}\r\n")
          bufferedSink.writeUtf8("\r\n")
          bufferedSink.write(operationByteString)

          val uploadsMapBuffer = uploads.toMapBuffer()
          bufferedSink.writeUtf8("\r\n--$boundary\r\n")
          bufferedSink.writeUtf8("Content-Disposition: form-data; name=\"map\"\r\n")
          bufferedSink.writeUtf8("Content-Type: application/json\r\n")
          bufferedSink.writeUtf8("Content-Length: ${uploadsMapBuffer.size}\r\n")
          bufferedSink.writeUtf8("\r\n")
          bufferedSink.writeAll(uploadsMapBuffer)

          uploads.values.forEachIndexed { index, upload ->
            bufferedSink.writeUtf8("\r\n--$boundary\r\n")
            bufferedSink.writeUtf8("Content-Disposition: form-data; name=\"$index\"")
            if (upload.fileName != null) {
              bufferedSink.writeUtf8("; filename=\"${upload.fileName}\"")
            }
            bufferedSink.writeUtf8("\r\n")
            bufferedSink.writeUtf8("Content-Type: ${upload.contentType}\r\n")
            val contentLength = upload.contentLength
            if (contentLength != -1L) {
              bufferedSink.writeUtf8("Content-Length: $contentLength\r\n")
            }
            bufferedSink.writeUtf8("\r\n")
            upload.writeTo(bufferedSink)
          }
          bufferedSink.writeUtf8("\r\n--$boundary--\r\n")
        }
      }
    }
  }
  private fun Map<String, Upload>.toMapBuffer(): Buffer {
    val buffer = Buffer()

    val writer = BufferedSinkJsonWriter(buffer)
    AnyResponseAdapter.toResponse(writer, entries.mapIndexed { index, entry ->
      index.toString() to listOf(entry.key)
    }.toMap())
    writer.flush()

    return buffer
  }
}
