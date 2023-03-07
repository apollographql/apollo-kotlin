package com.apollographql.apollo3.graphql.ast.test.keyfields

import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.checkKeyFields
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.transformation.addRequiredFields
import com.apollographql.apollo3.ast.validateAsSchema
import com.apollographql.apollo3.ast.introspection.toSchema
import com.apollographql.apollo3.ast.introspection.toSchemaGQLDocument
import okio.buffer
import okio.source
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KeyFieldsTest {
  @Test
  fun testAddRequiredFields() {
    val schema = File("src/test/kotlin/com/apollographql/apollo3/graphql/ast/test/keyfields/schema.graphqls").toSchema()

    val definitions = File("src/test/kotlin/com/apollographql/apollo3/graphql/ast/test/keyfields/operations.graphql")
        .source()
        .buffer()
        .parseAsGQLDocument()
        .getOrThrow()
        .definitions

    val fragments = definitions.filterIsInstance<GQLFragmentDefinition>().associateBy { it.name }

    val operation = definitions
        .filterIsInstance<GQLOperationDefinition>()
        .first()

    try {
      checkKeyFields(operation, schema, emptyMap())
      fail("an exception was expected")
    } catch (e: Exception) {
      assert(e.message?.contains("are not queried") == true)
    }

    val operationWithKeyFields = addRequiredFields(operation, "ifFragments", schema, fragments)
    checkKeyFields(operationWithKeyFields, schema, emptyMap())
  }

  @Test
  fun testExtendInterfaceTypePolicyDirective() {
    val schema = File("src/test/kotlin/com/apollographql/apollo3/graphql/ast/test/keyfields/extendsSchema.graphqls").toSchema()
    schema.toGQLDocument().validateAsSchema()
    assertEquals(setOf("id"), schema.keyFields("Node"))
  }

  @Test
  fun testExtendUnionTypePolicyDirective() {
    val schema = File("src/test/kotlin/com/apollographql/apollo3/graphql/ast/test/keyfields/extendsSchema.graphqls").toSchema()
    assertEquals(setOf("x"), schema.keyFields("Foo"))
  }

  @Test
  fun testObjectWithTypePolicyAndInterfaceTypePolicyErrors() {
    File("src/test/kotlin/com/apollographql/apollo3/graphql/ast/test/keyfields/objectAndInterfaceTypePolicySchema.graphqls")
        .toSchemaGQLDocument()
        .validateAsSchema()
        .issues
        .first()
        .let { issue ->
          assertContains(issue.message, "Type 'Foo' cannot have key fields since it implements")
          assertContains(issue.message, "Node")
          assertEquals(13, issue.sourceLocation.line)
        }
  }

  @Test
  fun testObjectInheritingTwoInterfacesWithDifferentKeyFields() {
    File("src/test/kotlin/com/apollographql/apollo3/graphql/ast/test/keyfields/objectInheritingTwoInterfaces.graphqls")
        .toSchemaGQLDocument()
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
          assertEquals(15, issue.sourceLocation.line)
        }
  }

  @Test
  fun testInterfacesWithoutKeyFields() {
    File("src/test/kotlin/com/apollographql/apollo3/graphql/ast/test/keyfields/interfacesWithoutKeyFields.graphqls")
        .toSchemaGQLDocument()
        .validateAsSchema()
        .issues
        .let { issues ->
          assertTrue(issues.isEmpty())
        }
  }
}
