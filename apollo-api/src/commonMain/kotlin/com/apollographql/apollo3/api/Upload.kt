package com.apollographql.apollo3.api

import okio.BufferedSink

/**
 * A class that represents a file upload in a multipart upload
 * See https://github.com/jaydenseric/graphql-multipart-request-spec
 *
 * This class is heavily inspired by [okhttp3.RequestBody]
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
