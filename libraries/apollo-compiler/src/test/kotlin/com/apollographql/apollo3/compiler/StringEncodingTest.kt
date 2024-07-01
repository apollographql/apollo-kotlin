package com.apollographql.apollo.compiler

import com.apollographql.apollo.ast.GQLObjectTypeDefinition
import com.apollographql.apollo.ast.GQLStringValue
import com.apollographql.apollo.ast.encodeToGraphQLSingleQuoted
import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.ast.toSchema
import com.google.common.truth.Truth
import org.junit.Test
import kotlin.test.assertEquals

class StringEncodingTest {
  @Test
  fun `single_quotes quotes are escaped`() {
    Truth.assertThat("a \"quote\"".encodeToGraphQLSingleQuoted()).isEqualTo("a \\\"quote\\\"")
  }

  @Test
  fun `single_quotes newlines are escaped`() {
    Truth.assertThat("""
      a
      line""".trimIndent().encodeToGraphQLSingleQuoted()).isEqualTo("a\\nline")
  }

  @Test
  fun `empty triple quotes are detected`() {
    // See https://github.com/apollographql/apollo-kotlin/issues/3172
    val schema = """
      type Query {
        field(param: String = ${"\"\"\"\"\"\""}): String
      }
    """.trimIndent()

    val queryType = schema.toGQLDocument().toSchema().typeDefinition("Query") as GQLObjectTypeDefinition
    val defaultValue = (queryType.fields.first().arguments.first().defaultValue as GQLStringValue).value
    assertEquals("", defaultValue)
  }

  @Test
  fun `string containing to double quotes is a valid default value`() {
    // See https://github.com/apollographql/apollo-kotlin/issues/3172
    val schema = """
      type Query {
        field(param: String = ${"\"\"\"\"\"\""}): String
      }
    """.trimIndent()

    val queryType = schema.toGQLDocument().toSchema().typeDefinition("Query") as GQLObjectTypeDefinition
    val defaultValue = (queryType.fields.first().arguments.first().defaultValue as GQLStringValue).value
    assertEquals("", defaultValue)
  }
}
