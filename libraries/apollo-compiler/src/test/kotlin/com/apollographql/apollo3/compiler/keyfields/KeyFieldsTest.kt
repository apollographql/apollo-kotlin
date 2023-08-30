package com.apollographql.apollo3.compiler.keyfields

import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.GQLExecutableDefinition
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.SourceAwareException
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.toGQLDocument
import com.apollographql.apollo3.ast.toSchema
import com.apollographql.apollo3.ast.validateAsSchema
import com.apollographql.apollo3.compiler.addRequiredFields
import com.apollographql.apollo3.compiler.checkKeyFields
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class KeyFieldsTest {
  @Test
  fun testAddRequiredFields() {
    val schema = "src/test/kotlin/com/apollographql/apollo3/compiler/keyfields/schema.graphqls"
        .toPath()
        .toGQLDocument()
        .toSchema()

    val definitions = "src/test/kotlin/com/apollographql/apollo3/compiler/keyfields/operations.graphql".toPath()
        .toGQLDocument()
        .definitions

    val fragments = definitions.filterIsInstance<GQLFragmentDefinition>().associateBy { it.name }

    val operation = definitions
        .filterIsInstance<GQLOperationDefinition>()
        .first()

    try {
      checkKeyFields(operation, schema, emptyMap())
      fail("an exception was expected")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("are not queried") == true)
    }

    val operationWithKeyFields = addRequiredFields(operation, "ifFragments", schema, fragments)
    checkKeyFields(operationWithKeyFields, schema, emptyMap())
  }

  @Test
  fun testExtendInterfaceTypePolicyDirective() {
    val schema = "src/test/kotlin/com/apollographql/apollo3/compiler/keyfields/extendsSchema.graphqls"
        .toPath()
        .toGQLDocument()
        .toSchema()
    assertEquals(setOf("id"), schema.keyFields("Node"))
  }

  @Test
  fun testExtendUnionTypePolicyDirective() {
    val schema = "src/test/kotlin/com/apollographql/apollo3/compiler/keyfields/extendsSchema.graphqls"
        .toPath()
        .toGQLDocument()
        .toSchema()
    assertEquals(setOf("x"), schema.keyFields("Foo"))
  }

  @Test
  fun testObjectWithTypePolicyAndInterfaceTypePolicyErrors() {
    "src/test/kotlin/com/apollographql/apollo3/compiler/keyfields/objectAndInterfaceTypePolicySchema.graphqls"
        .toPath()
        .toGQLDocument()
        .validateAsSchema()
        .issues
        .first()
        .let { issue ->
          assertContains(issue.message, "Type 'Foo' cannot have key fields since it implements")
          assertContains(issue.message, "Node")
          assertEquals(13, issue.sourceLocation?.line)
        }
  }

  @Test
  fun testObjectInheritingTwoInterfacesWithDifferentKeyFields() {
    "src/test/kotlin/com/apollographql/apollo3/compiler/keyfields/objectInheritingTwoInterfaces.graphqls"
        .toPath()
        .parseAsGQLDocument()
        .getOrThrow()
        .validateAsSchema()
        .issues
        .first()
        .let { issue ->
          assertEquals(
              """
          Apollo: Type 'Book' cannot inherit different keys from different interfaces:
          Node: [id]
          Product: [upc]
        """.trimIndent(),
              issue.message
          )
          assertEquals(15, issue.sourceLocation?.line)
        }
  }

  @Test
  fun testInterfacesWithoutKeyFields() {
    "src/test/kotlin/com/apollographql/apollo3/compiler/keyfields/interfacesWithoutKeyFields.graphqls"
        .toPath()
        .parseAsGQLDocument()
        .getOrThrow()
        .validateAsSchema()
        .issues
        .let { issues ->
          assertTrue(issues.isEmpty())
        }
  }

  @Test
  fun nonexistentKeyField() {
    val definitions = "src/test/kotlin/com/apollographql/apollo3/compiler/keyfields/nonexistentKeyField.graphqls"
        .toPath()
        .parseAsGQLDocument()
        .getOrThrow()
        .definitions

    val (operationDefinitions, schemaDefinitions) = definitions.partition { it is GQLExecutableDefinition }

    try {
      val schema = GQLDocument(schemaDefinitions, null).validateAsSchema().getOrThrow()
      var operation = (operationDefinitions.single() as GQLOperationDefinition)
      operation = addRequiredFields(operation, "always", schema, emptyMap())
      checkKeyFields(operation, schema, emptyMap())
      fail("An exception was expected")
    } catch (e: SourceAwareException) {
      check(e.message!!.contains("Field 'id' is not a valid key field for type 'Animal'"))
      return
    }
  }
}
