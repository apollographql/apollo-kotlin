package com.apollographql.apollo.compiler

interface CustomIdGenerator {
  fun apply(
      /**
       * The minified query string source with fragments
       */
      queryString: String,

      /**
       * The path of the query file
       */
      queryFilepath: String
  ): String

  /**
   * The version of the CustomIdGenerator
   *
   * You should change the version every time the implementation of the CustomIdGenerator
   * changes to let gradle and build tools know that they have to re-generate the
   * resulting files.
   */
  val version: String
}
