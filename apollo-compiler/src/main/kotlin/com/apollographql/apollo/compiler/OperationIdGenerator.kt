package com.apollographql.apollo.compiler

interface OperationIdGenerator {
  fun apply(
      /**
       * The GraphQL document
       */
      operationDocument: String,

      /**
       * The path to the GraphQL file
       */
      operationFilepath: String
  ): String

  /**
   * The version of the OperationIdGenerator
   *
   * You should change the version every time the implementation of the OperationIdGenerator
   * changes to let gradle and build tools know that they have to re-generate the
   * resulting files.
   */
  val version: String

  class Sha256: OperationIdGenerator {
    override fun apply(operationDocument: String, operationFilepath: String): String {
      return operationDocument.sha256()
    }

    override val version = "1.0"
  }
}
