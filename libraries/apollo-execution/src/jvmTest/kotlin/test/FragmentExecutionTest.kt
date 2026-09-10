@file:OptIn(ExperimentalCoroutinesApi::class)

package test

import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.execution.ExecutableSchema
import com.apollographql.apollo.execution.PreparedRequest
import com.apollographql.apollo.execution.RequestType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class FragmentExecutionTest {
  private fun String.toFragmentPreparedRequest(id: String?): PreparedRequest {
    val document = this.toGQLDocument()
    val fragments = document.definitions.filterIsInstance<GQLFragmentDefinition>()

    val fragment = fragments.firstOrNull()
    require(fragment != null) {
      "No fragment found in document: $this"
    }
    return PreparedRequest.Builder()
        .type(RequestType.Fragment)
        .name(fragment.name)
        .rootSelections(fragment.selections)
        .typename(fragment.typeCondition.name)
        .fragments(fragments.drop(1).associateBy { it.name })
        .id(id)
        .build()
  }
  @Test
  fun fragmentOnQueryType() {
    // language=graphql
    val schema = """
      type Query {
        foo: Int
      }
    """.trimIndent()

    val executableSchema = ExecutableSchema.Builder()
      .schema(schema.toGQLDocument())
      .resolver {
        when (it.field.name) {
          "foo" -> 42
          else -> error("Unknown field '${it.field.name}'")
        }
      }
      .build()

    val response = runBlocking {
      executableSchema.execute(
        """
        fragment queryFragment on Query { foo }
      """.toFragmentPreparedRequest(null)
      )
    }

    assertEquals(mapOf("foo" to 42), response.data)
  }

  @Test
  fun fragmentOnOtehrType() {
    // language=graphql
    val schema = """
      type Query {
        user(id: ID!): User
      }
      type User {
        id: ID!
        name: String
      }
    """.trimIndent()

    val executableSchema = ExecutableSchema.Builder()
        .schema(schema.toGQLDocument())
        .resolver {
          when (it.field.name) {
            "user" -> {
              check(it.getArgument<String>("id").getOrThrow() == "42")
              Unit
            }
            "id" -> "42"
            "name" -> "Foobar"
            else -> error("Unknown field '${it.field.name}'")
          }
        }
        .build()

    val response = runBlocking {
      executableSchema.execute(
          """
        fragment userFragment on User { id name }
      """.toFragmentPreparedRequest("42")
      )
    }

    assertEquals(mapOf("id" to "42", "name" to "Foobar"), response.data)
  }

}
