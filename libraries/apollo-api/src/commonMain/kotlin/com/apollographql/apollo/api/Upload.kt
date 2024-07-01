package com.apollographql.apollo.api

import okio.BufferedSink

/**
 * A class that represents a file upload in a multipart upload
 * See https://github.com/jaydenseric/graphql-multipart-request-spec
 *
 * Use this to map your upload custom scalar and the apollo runtime will be able to extract them
 * and send them out of band.
 *
 * In your build.gradle file:
 * ```
 * mapScalarToUpload(Upload)
 * ```
 *
 * If you have a JVM File at hand, see also [com.apollographql.apollo.api.DefaultUpload.Builder.content]
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
}
