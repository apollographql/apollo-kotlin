@file:JvmMultifileClass
@file:JvmName("ApolloParser")
package com.apollographql.apollo3.ast

import okio.buffer
import okio.source
import java.io.File

fun File.parseAsGQLDocument(options: ParserOptions = ParserOptions.Default): GQLResult<GQLDocument> {
  return this.source().buffer().parseAsGQLDocument(filePath = path, options = options)
}
fun File.parseAsGQLValue(options: ParserOptions = ParserOptions.Default): GQLResult<GQLValue> {
  return this.source().buffer().parseAsGQLValue(filePath = path, options = options)
}
fun File.parseAsGQLType(options: ParserOptions = ParserOptions.Default): GQLResult<GQLType> {
  return this.source().buffer().parseAsGQLType(filePath = path, options = options)
}
fun File.parseAsGQLSelections(options: ParserOptions = ParserOptions.Default): GQLResult<List<GQLSelection>> {
  return this.source().buffer().parseAsGQLSelections(filePath = path, options = options)
}

