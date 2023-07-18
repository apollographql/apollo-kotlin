@file:JvmMultifileClass
@file:JvmName("ApolloParser")
package com.apollographql.apollo3.ast

import okio.buffer
import okio.source
import java.io.File

fun File.parseAsGQLDocument(options: ParserOptions = ParserOptions.Default): GQLResult<GQLDocument> {
  return this.source().buffer().parseAsGQLDocument(filePath = path, options = options)
}

