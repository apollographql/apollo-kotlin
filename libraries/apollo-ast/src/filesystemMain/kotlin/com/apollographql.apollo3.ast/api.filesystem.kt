@file:JvmMultifileClass
@file:JvmName("ApolloParser")

package com.apollographql.apollo.ast

import com.apollographql.apollo.ast.introspection.toGQLDocument
import com.apollographql.apollo.ast.introspection.toIntrospectionSchema
import okio.Path
import okio.buffer
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

fun Path.parseAsGQLDocument(options: ParserOptions = ParserOptions.Default): GQLResult<GQLDocument> {
  return HOST_FILESYSTEM.source(this).buffer().parseAsGQLDocument(filePath = toString(), options = options)
}

fun Path.toGQLDocument(options: ParserOptions = ParserOptions.Default, allowJson: Boolean = false): GQLDocument {
  return if (allowJson && name.endsWith(".json")) {
    toIntrospectionSchema().toGQLDocument()
  } else {
    parseAsGQLDocument(options).getOrThrow()
  }
}