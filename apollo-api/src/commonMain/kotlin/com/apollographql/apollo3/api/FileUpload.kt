package com.apollographql.apollo.api

import okio.BufferedSink

/**
 * A class that represents a file upload in a multipart upload
 * See https://github.com/jaydenseric/graphql-multipart-request-spec
 *
 * This class is heavily inspired by [okhttp3.RequestBody]
 */
open class FileUpload(val mimetype: String, val filePath: String? = null) {
  /**
   * Returns the number of bytes that will be written to `sink` in a call to [.writeTo],
   * or -1 if that count is unknown.
   */
  open fun contentLength(): Long {
    return -1
  }

  /**
   *  Writes the content of this request to `sink`.
   */
  open fun writeTo(sink: BufferedSink) {
    throw UnsupportedOperationException("ApolloGraphQL: if you're not passing a `filePath` parameter, you must override `FileUpload.writeTo`")
  }

  /**
   * The fileName to send to the server. Might be null
   */
  open fun fileName(): String? {
    throw UnsupportedOperationException("ApolloGraphQL: if you're not passing a `filePath` parameter, you must override `FileUpload.fileName`")
  }

  companion object {

  }
}
