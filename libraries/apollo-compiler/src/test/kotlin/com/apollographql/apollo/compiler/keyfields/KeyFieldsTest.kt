package com.apollographql.apollo.compiler.keyfields

import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.GQLExecutableDefinition
import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.ast.validateAsSchemaAndAddApolloDefinition
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KeyFieldsTest {
  @Test
  fun testExtendInterfaceTypePolicyDirective() {
    val schema = "src/test/kotlin/com/apollographql/apollo/compiler/keyfields/extendsSchema.graphqls"
        .toPath()
        .toGQLDocument()
        .validateAsSchemaAndAddApolloDefinition()
        .getOrThrow()
    assertEquals(setOf("id"), schema.keyFields("Node"))
  }

  @Test
  fun testObjectWithTypePolicyAndInterfaceTypePolicyErrors() {
    "src/test/kotlin/com/apollographql/apollo/compiler/keyfields/objectAndInterfaceTypePolicySchema.graphqls"
        .toPath()
        .toGQLDocument()
        .validateAsSchemaAndAddApolloDefinition()
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
    "src/test/kotlin/com/apollographql/apollo/compiler/keyfields/objectInheritingTwoInterfaces.graphqls"
        .toPath()
        .parseAsGQLDocument()
        .getOrThrow()
        .validateAsSchemaAndAddApolloDefinition()
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
    "src/test/kotlin/com/apollographql/apollo/compiler/keyfields/interfacesWithoutKeyFields.graphqls"
        .toPath()
        .parseAsGQLDocument()
        .getOrThrow()
        .validateAsSchemaAndAddApolloDefinition()
        .issues
        .let { issues ->
          assertTrue(issues.isEmpty())
        }
  }

  @Test
  fun nonexistentKeyField() {
    val definitions = "src/test/kotlin/com/apollographql/apollo/compiler/keyfields/nonexistentKeyField.graphqls"
        .toPath()
        .parseAsGQLDocument()
        .getOrThrow()
        .definitions

    val (_, schemaDefinitions) = definitions.partition { it is GQLExecutableDefinition }

    val issues = GQLDocument(schemaDefinitions, null)
        .validateAsSchemaAndAddApolloDefinition()
        .issues
    check(issues.any { it.message.contains("No such field: 'Animal.id'") })
    check(issues.any { it.message.contains("No such field: 'Node.version'") })
    check(issues.any { it.message.contains("No such field: 'Foo.x'") })
  }
}
