package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
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
   *
   * When using the compiler outside the Apollo Gradle Plugin context, [version] is not accessed.
   */
  @Deprecated("Load PackageNameGenerator through plugins")
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  val version: String
    get() {
      error("this should only be called from the Apollo Gradle Plugin")
    }


  object Sha256 : OperationIdGenerator {
    override fun apply(operationDocument: String, operationName: String): String {
      return operationDocument.sha256()
    }
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
