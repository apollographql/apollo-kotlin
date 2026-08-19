package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFieldDefinition
import com.apollographql.apollo.ast.GQLNamedType
import com.apollographql.apollo.ast.GQLObjectTypeDefinition
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.definitionFromScope
import com.apollographql.apollo.ast.rawType
import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.ast.validateAsSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests [definitionFromScope], which is backed by an index of the field definitions of every type of the schema.
 */
class FieldDefinitionTest {
  // language=graphql
  private val schema: Schema = """
      type Query {
        animal: Animal
      }

      interface Animal {
        id: ID!
      }

      type Cat implements Animal {
        id: ID!
        meow: String
      }

      union Pet = Cat
  """.trimIndent().toGQLDocument().validateAsSchema().getOrThrow()

  private fun field(name: String) = GQLField(alias = null, name = name, arguments = emptyList(), directives = emptyList(), selections = emptyList())

  @Test
  fun fieldsOfAnObjectAreResolved() {
    assertEquals("meow", field("meow").definitionFromScope(schema, "Cat")?.name)
    assertEquals("ID", field("id").definitionFromScope(schema, "Cat")?.type?.rawType()?.name)
    assertNull(field("bark").definitionFromScope(schema, "Cat"))
  }

  @Test
  fun metaFieldsAreResolved() {
    // __typename is available everywhere
    assertEquals("String", field("__typename").definitionFromScope(schema, "Cat")?.type?.rawType()?.name)
    assertEquals("String", field("__typename").definitionFromScope(schema, "Animal")?.type?.rawType()?.name)
    assertEquals("String", field("__typename").definitionFromScope(schema, "Pet")?.type?.rawType()?.name)

    // __schema and __type are only available on the query root type
    assertEquals("__Schema", field("__schema").definitionFromScope(schema, "Query")?.type?.rawType()?.name)
    assertEquals("__Type", field("__type").definitionFromScope(schema, "Query")?.type?.rawType()?.name)
    assertNull(field("__schema").definitionFromScope(schema, "Cat"))
    assertNull(field("__type").definitionFromScope(schema, "Cat"))
  }

  @Test
  fun fieldsOfAnInterfaceAreResolved() {
    assertEquals("id", field("id").definitionFromScope(schema, "Animal")?.name)
    // 'meow' is only on Cat
    assertNull(field("meow").definitionFromScope(schema, "Animal"))
  }

  @Test
  fun unionsOnlyHaveTypename() {
    assertNull(field("id").definitionFromScope(schema, "Pet"))
  }

  @Test
  fun typeDefinitionsForeignToTheSchemaAreResolvedToo() {
    val syntheticTypeDefinition = GQLObjectTypeDefinition(
        description = null,
        name = "Cat",
        directives = emptyList(),
        implementsInterfaces = emptyList(),
        fields = listOf(
            GQLFieldDefinition(
                name = "purr",
                type = GQLNamedType(name = "String"),
                arguments = emptyList(),
                directives = emptyList(),
                description = null,
            )
        )
    )

    // Same name as a type of the schema, but a different definition: the definition wins
    assertEquals("purr", field("purr").definitionFromScope(schema, syntheticTypeDefinition)?.name)
    assertNull(field("meow").definitionFromScope(schema, syntheticTypeDefinition))
    assertEquals("__typename", field("__typename").definitionFromScope(schema, syntheticTypeDefinition)?.name)
  }
}
