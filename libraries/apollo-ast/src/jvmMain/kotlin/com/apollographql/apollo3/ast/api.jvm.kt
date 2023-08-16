@file:JvmMultifileClass
@file:JvmName("ApolloParser")
package com.apollographql.apollo3.ast

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.introspection.toGQLDocument
import com.apollographql.apollo3.ast.introspection.toIntrospectionSchema
import okio.buffer
import okio.source
import java.io.File

fun File.parseAsGQLDocument(options: ParserOptions = ParserOptions.Default): GQLResult<GQLDocument> {
  return this.source().buffer().parseAsGQLDocument(filePath = path, options = options)
}

fun File.toGQLDocument(options: ParserOptions = ParserOptions.Default, allowJson: Boolean = false): GQLDocument {
  if (allowJson && extension == "json") {
    return toIntrospectionSchema().toGQLDocument()
  }
  return parseAsGQLDocument(options).getOrThrow()
}


@ApolloExperimental
fun File.toSchema(allowJson: Boolean = false): Schema = toGQLDocument(allowJson = allowJson).validateAsSchema().getOrThrow()
