package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.ast.validateAsSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests [Schema.possibleTypes], [Schema.implementedTypes] and [Schema.superTypes], which are all backed by an index of
 * the type hierarchy. Interfaces implementing other interfaces and unions are the interesting cases there.
 */
class TypeHierarchyTest {
  // language=graphql
  private val schema: Schema = """
      type Query {
        node: Node
      }

      interface Node {
        id: ID!
      }

      interface Named implements Node {
        id: ID!
        name: String!
      }

      interface Alone {
        nothing: String
      }

      type Person implements Named & Node {
        id: ID!
        name: String!
      }

      type Company implements Named & Node {
        id: ID!
        name: String!
      }

      type Robot implements Node {
        id: ID!
      }

      union Actor = Person | Company

      union Anything = Person | Company | Robot
  """.trimIndent().toGQLDocument().validateAsSchema().getOrThrow()

  @Test
  fun possibleTypesOfAnInterfaceIncludesTransitiveImplementations() {
    // Person and Company implement Node through Named only
    assertEquals(setOf("Person", "Company", "Robot"), schema.possibleTypes("Node"))
    assertEquals(setOf("Person", "Company"), schema.possibleTypes("Named"))
  }

  @Test
  fun possibleTypesOfAnInterfaceWithoutImplementationsIsEmpty() {
    assertEquals(emptySet(), schema.possibleTypes("Alone"))
  }

  @Test
  fun possibleTypesOfAUnionAreItsMembers() {
    assertEquals(setOf("Person", "Company"), schema.possibleTypes("Actor"))
  }

  @Test
  fun possibleTypesOfAnObjectIsItself() {
    assertEquals(setOf("Person"), schema.possibleTypes("Person"))
  }

  @Test
  fun implementedTypesIncludeTransitiveInterfacesAndUnions() {
    assertEquals(setOf("Node", "Named", "Person", "Actor", "Anything"), schema.implementedTypes("Person"))
    // Robot is not a member of Actor
    assertEquals(setOf("Node", "Robot", "Anything"), schema.implementedTypes("Robot"))
    assertEquals(setOf("Node", "Named"), schema.implementedTypes("Named"))
    assertEquals(setOf("Actor"), schema.implementedTypes("Actor"))
  }

  @Test
  fun subTypesAndSuperTypes() {
    assertTrue(schema.isTypeASubTypeOf("Person", "Node"))
    assertTrue(schema.isTypeASubTypeOf("Person", "Actor"))
    assertFalse(schema.isTypeASubTypeOf("Robot", "Actor"))
    assertTrue(schema.isTypeASuperTypeOf("Node", "Person"))
  }

  @Test
  fun superTypesAreDirectOnly() {
    // Node is implemented through Named and is not a direct super type of Person
    assertEquals(setOf("Named", "Node", "Actor", "Anything"), schema.superTypes(schema.typeDefinition("Person")))
    assertEquals(setOf("Node"), schema.superTypes(schema.typeDefinition("Named")))
    assertEquals(emptySet(), schema.superTypes(schema.typeDefinition("Alone")))
  }

  @Test
  fun rootTypeNamesUseTheSchemaDefinition() {
    assertEquals("Query", schema.rootTypeNameOrNullFor("query"))
    assertNull(schema.rootTypeNameOrNullFor("mutation"))
    assertNull(schema.rootTypeNameOrNullFor("subscription"))
    assertNull(schema.rootTypeNameOrNullFor("somethingElse"))
    assertEquals("Mutation", schema.rootTypeNameFor("mutation"))
  }

  @Test
  fun rootTypeNamesHonourCustomRootTypes() {
    // language=graphql
    val customRoots = """
      schema {
        query: MyQuery
        mutation: MyMutation
      }

      type MyQuery {
        foo: Int
      }

      type MyMutation {
        setFoo(foo: Int): Int
      }
    """.trimIndent().toGQLDocument().validateAsSchema().getOrThrow()

    assertEquals("MyQuery", customRoots.rootTypeNameOrNullFor("query"))
    assertEquals("MyMutation", customRoots.rootTypeNameOrNullFor("mutation"))
    assertNull(customRoots.rootTypeNameOrNullFor("subscription"))
    assertEquals("Subscription", customRoots.rootTypeNameFor("subscription"))
  }
}
