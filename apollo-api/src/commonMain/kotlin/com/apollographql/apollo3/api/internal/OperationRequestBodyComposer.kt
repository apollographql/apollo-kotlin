package com.apollographql.apollo3.api.internal

import com.apollographql.apollo3.api.AnyResponseAdapter
import com.apollographql.apollo3.api.Upload
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.internal.json.FileUploadAwareJsonWriter
import com.apollographql.apollo3.api.json.use
import com.benasher44.uuid.uuid4
import okio.Buffer
import okio.BufferedSink
import okio.ByteString
import kotlin.jvm.JvmStatic

/**
 * [OperationRequestBodyComposer] is a helper class to create a body from an operation. The body will include serialized
 * variables and possibly be multi-part if variables contain uploads.
 */
object OperationRequestBodyComposer {
  interface Body {
    val operations: ByteString
    val contentType: String
    val contentLength: Long

    fun writeTo(bufferedSink: BufferedSink)
  }

  /**
   * @param operation the instance of the [Operation] to create a body for.
   * @param autoPersistQueries write the APQs extension if true
   * @param withQueryDocument if false, skip writing the query document. This can be used with APQs to make network requests smaller
   * @param responseAdapterCache a [ResponseAdapterCache] containing the custom scalar [ResponseAdapter] to use to serialize variables
   *
   * @return a [Body] to be sent over HTTP. It will either be of "application/json" type or "multipart/form-data" if variables contain
   * [Upload]
   */
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
          name("sha256Hash").value(operation.id())
          endObject()
          endObject()
        }
        if (!autoPersistQueries || withQueryDocument) {
          name("query").value(operation.document())
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
        override val contentLength = operationByteString.size.toLong()

        override fun writeTo(bufferedSink: BufferedSink) {
          bufferedSink.write(operationByteString)
        }
      }
    } else {
      return object : Body {
        private val boundary = uuid4().toString()

        override val operations = operationByteString
        override val contentType = "multipart/form-data; boundary=$boundary"
        // XXX: support non-chunked multipart
        override val contentLength = -1L

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

    BufferedSinkJsonWriter(buffer).use {
      AnyResponseAdapter.toResponse(it, ResponseAdapterCache.DEFAULT, entries.mapIndexed { index, entry ->
        index.toString() to listOf(entry.key)
      }.toMap())
    }

    return buffer
  }
}
