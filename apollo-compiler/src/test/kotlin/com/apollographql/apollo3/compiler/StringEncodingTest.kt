package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLStringValue
import com.apollographql.apollo3.ast.GQLTypeDefinition
import com.apollographql.apollo3.ast.encodeToGraphQLSingleQuoted
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.toSchema
import com.apollographql.apollo3.ast.validateAsSchema
import com.google.common.truth.Truth
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ApolloExperimental::class)
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
    // See https://github.com/apollographql/apollo-android/issues/3172
    val schema = """
      type Query {
        field(param: String = ${"\"\"\"\"\"\""}): String
      }
    """.trimIndent()

    val queryType = schema.buffer().toSchema().typeDefinition("Query") as GQLObjectTypeDefinition
    val defaultValue = (queryType.fields.first().arguments.first().defaultValue as GQLStringValue).value
    assertEquals("", defaultValue)
  }

  @Test
  fun `string containing to double quotes is a valid default value`() {
    // See https://github.com/apollographql/apollo-android/issues/3172
    val schema = """
      type Query {
        field(param: String = ${"\"\"\"\"\"\""}): String
      }
    """.trimIndent()

    val queryType = schema.buffer().toSchema().typeDefinition("Query") as GQLObjectTypeDefinition
    val defaultValue = (queryType.fields.first().arguments.first().defaultValue as GQLStringValue).value
    assertEquals("", defaultValue)
  }
}
