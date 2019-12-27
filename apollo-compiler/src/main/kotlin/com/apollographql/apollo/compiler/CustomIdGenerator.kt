package com.apollographql.apollo.compiler

interface CustomIdGenerator {
  fun apply(queryString: String): String

  /**
   * The version of the CustomIdGenerator
   *
   * You should change the version every time the implementation of the CustomIdGenerator
   * changes to let gradle and build tools know that they have to re-generate the
   * resulting files.
   */
  val version: String
}
