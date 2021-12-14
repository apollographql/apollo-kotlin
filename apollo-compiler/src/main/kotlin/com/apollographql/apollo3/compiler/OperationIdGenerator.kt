package com.apollographql.apollo3.compiler

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

interface OperationIdGenerator {
  /**
   * computes an id for the given operation
   *
   * @param operationDocument the string representing this operation, including potential fragments,
   * as it is sent to the server
   * @param operationName the operation name
   * @return a string uniquely identifying this operation
   */
  fun apply(
      operationDocument: String,
      operationName: String,
  ): String

  /**
   * The version of the OperationIdGenerator
   *
   * Change the version every time the implementation of the OperationIdGenerator
   * changes to let gradle and build tools know that they have to re-generate the
   * resulting files.
   */
  val version: String

  object Sha256 : OperationIdGenerator {
    override fun apply(operationDocument: String, operationName: String): String {
      return operationDocument.sha256()
    }

    override val version = "sha256-1.0"
  }

  companion object {
    private fun String.sha256(): String {
      val bytes = toByteArray(charset = StandardCharsets.UTF_8)
      val md = MessageDigest.getInstance("SHA-256")
      val digest = md.digest(bytes)
      return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
  }
}
