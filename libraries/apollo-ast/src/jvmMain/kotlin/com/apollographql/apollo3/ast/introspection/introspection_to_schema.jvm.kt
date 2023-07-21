@file:JvmMultifileClass
@file:JvmName("Introspection_to_schemaKt")
package com.apollographql.apollo3.ast.introspection

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.validateAsSchema
import java.io.File

/**
 * Transforms the given file to a [GQLDocument] without doing validation
 */
@ApolloExperimental
fun File.toSchemaGQLDocument(): GQLDocument {
  return if (extension == "json") {
    toIntrospectionSchema().toGQLDocument(filePath = path)
  } else {
    parseAsGQLDocument().getOrThrow()
  }
}

@ApolloExperimental
fun File.toSchema(): Schema = toSchemaGQLDocument().validateAsSchema().getOrThrow()
