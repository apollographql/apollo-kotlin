package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.ForeignSchema
import com.apollographql.apollo.ast.internal.SchemaValidationOptions
import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.ast.toSchema
import com.apollographql.apollo.ast.validateAsSchema
import kotlin.test.Test
import kotlin.test.assertEquals

class SchemaTest {
  @Test
  fun schemaMayContainBuiltinDirectives() {
    // language=graphql
    val schemaString = """
      "Directs the executor to include this field or fragment only when the `if` argument is true"
      directive @include(
          "Included when true."
          if: Boolean!
      ) on FIELD | FRAGMENT_SPREAD | INLINE_FRAGMENT
      
      type Query {
        foo: Int
      }
    """.trimIndent()

    schemaString.toGQLDocument().toSchema()
  }

  /**
   * A trimmed version of a cache foreign schema.
   */
  private val cacheControlSchema = ForeignSchema("cache", "v0.1", listOf("directive @cacheControl(maxAge: Int!) on FIELD_DEFINITION".parseAsGQLDocument().getOrThrow().definitions.single()))

  @Test
  fun linkDirective() {
    // language=graphql
    val schemaString = """
      extend schema @link(url: "https://specs.apollo.dev/cache/v0.1/")
      
      type Query {
        foo: Int @cache__cacheControl(maxAge: 100)
      }
    """.trimIndent()

    schemaString.toGQLDocument().validateAsSchema(
        SchemaValidationOptions(
            addKotlinLabsDefinitions = false,
            foreignSchemas = listOf(cacheControlSchema)
        )
    ).getOrThrow()
  }

  @Test
  fun importDirective() {
    // language=graphql
    val schemaString = """
      extend schema @link(url: "https://specs.apollo.dev/cache/v0.1/", import: ["@cacheControl"])
      
      type Query {
        foo: Int @cacheControl(maxAge: 100)
      }
    """.trimIndent()

    val schema = schemaString.toGQLDocument().validateAsSchema(
        SchemaValidationOptions(
            addKotlinLabsDefinitions = false,
            foreignSchemas = listOf(cacheControlSchema)
        )
    ).getOrThrow()

    assertEquals("cacheControl", schema.originalDirectiveName("cacheControl"))
  }

  @Test
  fun unknownSchemaFails() {
    // language=graphql
    val schemaString = """
      extend schema @link(url: "https://specs.apollo.dev/unknown/v0.1/")
      
      type Query {
        foo: Int 
      }
    """.trimIndent()

    val result = schemaString.toGQLDocument().validateAsSchema(
        SchemaValidationOptions(
            addKotlinLabsDefinitions = false,
            foreignSchemas = listOf(cacheControlSchema)
        )
    )

    assertEquals(1, result.issues.size)
    assertEquals("Apollo: unknown foreign schema 'unknown/v0.1'", result.issues.first().message)
  }

  @Test
  fun unknownImportFails() {
    // language=graphql
    val schemaString = """
      extend schema @link(url: "https://specs.apollo.dev/cache/v0.1/", import: ["@unknownDirective"])
      
      type Query {
        foo: Int 
      }
    """.trimIndent()

    val result = schemaString.toGQLDocument().validateAsSchema(
        SchemaValidationOptions(
            addKotlinLabsDefinitions = false,
            foreignSchemas = listOf(cacheControlSchema)
        )
    )

    assertEquals(1, result.issues.size)
    assertEquals("Apollo: unknown definition '@unknownDirective'", result.issues.first().message)
  }
}
