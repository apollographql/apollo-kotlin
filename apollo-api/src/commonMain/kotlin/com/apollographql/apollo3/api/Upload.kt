package com.apollographql.apollo3.api

import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8

/**
 * A class that represents a file upload in a multipart upload
 * See https://github.com/jaydenseric/graphql-multipart-request-spec
 *
 * Use this to map your upload custom scalar and the apollo runtime will be able to extract them
 * and send them out of band.
 *
 * customScalarsMapping.set(mapOf(
 *   "Upload" to "com.apollographql.apollo3.api.Upload"
 * ))
 *
 * If you have a JVM File at hand, see also [com.apollographql.apollo3.api.DefaultUpload.Builder.content]
 */
interface Upload {
  val contentType: String

  /**
   * Returns the number of bytes that will be written to `sink` in a call to [.writeTo],
   * or -1 if that count is unknown.
   */
  val contentLength: Long

  /**
   * The fileName to send to the server. Might be null
   */
  val fileName: String?

  /**
   *  Writes the content of this request to `sink`.
   */
  fun writeTo(sink: BufferedSink)

  companion object {
    @Deprecated("This will be removed in v3.0.0.")
    fun fromString(string: String, fileName: String? = null, contentType: String = "text/plain"): Upload {
      return DefaultUpload.Builder()
          .content(string)
          .apply {
            if (fileName != null) {
              fileName(fileName)
            }
          }
          .contentType(contentType)
          .build()
    }

    @Deprecated("This will be removed in v3.0.0.")
    fun fromByteArray(byteArray: ByteArray, fileName: String? = null, contentType: String = "text/plain"): Upload {
      return DefaultUpload.Builder()
          .content(byteArray)
          .apply {
            if (fileName != null) {
              fileName(fileName)
            }
          }
          .contentType(contentType)
          .build()
    }

    @Deprecated("This will be removed in v3.0.0.")
    fun fromSource(source: BufferedSource, contentLength: Long = -1, fileName: String? = null, contentType: String = "text/plain"): Upload {
      return DefaultUpload.Builder()
          .content(source)
          .apply {
            if (fileName != null) {
              fileName(fileName)
            }
          }
          .contentType(contentType)
          .build()
    }
  }
}

