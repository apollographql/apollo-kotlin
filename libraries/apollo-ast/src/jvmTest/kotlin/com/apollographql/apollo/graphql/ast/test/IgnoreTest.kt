package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.ParserOptions
import com.apollographql.apollo.ast.SchemaValidationOptions
import com.apollographql.apollo.ast.builtinForeignSchemas
import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.ast.toSchema
import com.apollographql.apollo.ast.validateAsSchema
import kotlin.test.Test
import kotlin.test.assertEquals


class IgnoreTest  {
  @Test
  fun test() {
    val schema = """
      extend schema @link(url: "https://specs.apollo.dev/kotlin_ignore/v0.1/", import: ["@ignore"])
 
      type Query {
        foo: Int!
      }
      
      directive @nonnull @ignore on FIELD
    """.trimIndent().toGQLDocument(options = ParserOptions.Builder().allowDirectivesOnDirectives(true).build())
        .validateAsSchema(
            validationOptions = SchemaValidationOptions.Builder()
            .foreignSchemas(builtinForeignSchemas())
            .build()
        ).getOrThrow()

    assertEquals(null, schema.originalDirectiveName("nonnull"))
  }
}