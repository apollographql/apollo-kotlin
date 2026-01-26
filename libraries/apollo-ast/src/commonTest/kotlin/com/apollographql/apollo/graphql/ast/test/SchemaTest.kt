@file:OptIn(ApolloExperimental::class)

package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.ast.ForeignSchema
import com.apollographql.apollo.ast.builtinForeignSchemas
import com.apollographql.apollo.ast.internal.SchemaValidationOptions
import com.apollographql.apollo.ast.internal.toSemanticSdl
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
  private val cacheControlSchema =
    ForeignSchema("cache", "v0.1", listOf("directive @cacheControl(maxAge: Int!) on FIELD_DEFINITION".parseAsGQLDocument()
        .getOrThrow().definitions.single()
    )
    )

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
        SchemaValidationOptions.Builder()
            .addKotlinLabsDefinitions(false)
            .addForeignSchema(cacheControlSchema)
            .build()
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
        SchemaValidationOptions.Builder()
            .addKotlinLabsDefinitions(false)
            .addForeignSchema(cacheControlSchema)
            .build()
    ).getOrThrow()

    assertEquals("cacheControl", schema.originalDirectiveName("cacheControl"))
  }

  @Test
  fun multipleImportDirectives() {
    // language=graphql
    val schemaString = """
      extend schema
      @link(url: "https://specs.apollo.dev/cache/v0.1/", import: ["@cacheControl"])
      @link(url: "https://example.com/example/v0.1/", import: ["@example"])
      extend schema
      @link(url: "https://example.com/example2/v0.1/", import: ["@example2"])

      type Query {
        foo: Int @cacheControl(maxAge: 100) @example @example2
      }
    """.trimIndent()

    val schema = schemaString.toGQLDocument().validateAsSchema(
        SchemaValidationOptions.Builder()
            .foreignSchemas(listOf(
                cacheControlSchema,
                ForeignSchema("example", "v0.1",
                    listOf("directive @example on FIELD_DEFINITION".parseAsGQLDocument().getOrThrow().definitions.single())
                ),
                ForeignSchema("example2", "v0.1",
                    listOf("directive @example2 on FIELD_DEFINITION".parseAsGQLDocument().getOrThrow().definitions.single())
                )
            )
            ).build()
    ).getOrThrow()

    assertEquals("cacheControl", schema.originalDirectiveName("cacheControl")
    )
    assertEquals("example", schema.originalDirectiveName("example"))
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
        SchemaValidationOptions.Builder()
            .addForeignSchema(cacheControlSchema)
            .build()
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
        SchemaValidationOptions.Builder()
            .addForeignSchema(cacheControlSchema)
            .build()
    )

    assertEquals(1, result.issues.size)
    assertEquals("Apollo: unknown definition '@unknownDirective'", result.issues.first().message)
  }

  @Test
  fun descriptionsAndArgumentOrderDoesNotMatterForSemanticComparison() {
    // language=graphql
    val document1 = """
      directive @defer(
        "Deferred behaviour is controlled by this argument" 
        if: Boolean! = true, 
        "A unique label that represents the fragment being deferred" 
        label: String
      ) on FRAGMENT_SPREAD|INLINE_FRAGMENT
    """.trimIndent()

    // language=graphql
    val document2 = """
      directive @defer(
        label: String,
        if: Boolean! = true, 
      ) on FRAGMENT_SPREAD|INLINE_FRAGMENT
    """.trimIndent()

    assertEquals(document1.toGQLDocument().toSemanticSdl(), document2.toGQLDocument().toSemanticSdl())
  }

  @Test
  fun purposeDoesNotClash() {
    // language=graphql
    val schemaString = """
      extend schema @link(
        url: "https://specs.apollo.dev/nullability/v0.4",
        import: ["@semanticNonNull", "@semanticNonNullField", "@catch", "CatchTo", "@catchByDefault"]
      )

      # Make sure this doesn't clash with the @link Purpose
      enum Purpose {
        Foo
      }

      type Query {
        purpose: Purpose
      }

      extend schema @catchByDefault(to: THROW)
    """.trimIndent()
    schemaString.toGQLDocument().validateAsSchema(
        SchemaValidationOptions.Builder()
            .foreignSchemas(builtinForeignSchemas())
            .build()
    ).getOrThrow()
  }
}
