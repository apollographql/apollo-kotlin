@file:Suppress("UNCHECKED_CAST")

package test

import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.execution.ExecutableSchema
import com.apollographql.apollo.execution.SubscriptionResponse
import com.apollographql.apollo.execution.toGraphQLRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals


class ExecutionTest {

  @Test
  fun simple() = runBlocking {
    val schema = """
            type Query {
                foo: String!
            }
        """.trimIndent()

    val document = """
            {
                foo
            }
        """.trimIndent()

    val response = ExecutableSchema.Builder()
      .schema(schema)
      .resolver { resolveInfo ->
        if (resolveInfo.parentType != "Query" || resolveInfo.fieldName != "foo") return@resolver null
        return@resolver "42"
      }
      .build()
      .execute(document.toGraphQLRequest(), ExecutionContext.Empty)
    assertEquals(mapOf("foo" to "42"), response.data)
    assertEquals(null, response.errors)
  }

  @Test
  fun argument() = runBlocking {
    val schema = """
            type Query {
                foo(first: Int): String!
            }
        """.trimIndent()

    val document = """
            {
                foo(first: 42)
            }
        """.trimIndent()

    val response = ExecutableSchema.Builder()
      .schema(schema)
      .resolver { resolveInfo ->
        resolveInfo.getArgument<Int>("first").getOrNull()?.toString(16)
      }
      .build()
      .execute(document.toGraphQLRequest(), ExecutionContext.Empty)

    assertEquals(mapOf("foo" to "2a"), response.data)
    assertEquals(null, response.errors)
  }

  @Test
  fun subscription() {
    val schema = """
            type Query {
                foo: String
            }
            type Subscription {
                foo: Int
            }
        """.trimIndent()

    val document = """
            subscription {
                foo
            }
        """.trimIndent()

    val executableSchema = ExecutableSchema.Builder()
      .schema(schema)
      .resolver { resolveInfo ->
        val ret: Flow<Int> = when (resolveInfo.parentType) {
          "Subscription" -> flow {
            repeat(5) {
              emit(it)
              delay(1)
            }
          }

          else -> error("never called")
        }

        ret
      }
      .build()

    val response = runBlocking {
      executableSchema.subscribe(document.toGraphQLRequest()).toList()
    }

    assertEquals(
      listOf(0, 1, 2, 3, 4),
      response.filterIsInstance<SubscriptionResponse>().map { it.response.data as Map<String, Any?> }
        .map { it.get("foo") }
    )
  }

  @Test
  fun invalidSubscription() {
    val schema = """
            type Query {
                foo: String
            }
            type Subscription {
                foo: Int
            }
        """.trimIndent()

    val document = """
            subscription {
                # invalid field
                bar
            }
        """.trimIndent()

    val executableSchema = ExecutableSchema.Builder()
      .schema(schema)
      .build()

    val response = runBlocking {
      executableSchema.subscribe(document.toGraphQLRequest()).toList()
    }

    response.filterIsInstance<SubscriptionResponse>().single().response.errors!!.single().apply {
      assertEquals("Can't query `bar` on type `Subscription`", message)
      assertEquals(3, locations.orEmpty().single().line)
      assertEquals(5, locations.orEmpty().single().column)
    }
  }

  @Test
  fun nullBubbles(): Unit = runBlocking {
    val schema = """
            type Query {
                foo: String!
            }
        """.trimIndent()

    val document = """
            {
                foo
            }
        """.trimIndent()

    ExecutableSchema.Builder()
      .schema(schema)
      .resolver {
        null
      }
      .build()
      .execute(document.toGraphQLRequest(), ExecutionContext.Empty)
      .apply {
        assertEquals(null, data)
        assertEquals("A resolver returned null in a non-nullable position", errors.orEmpty().single().message)
        assertEquals(listOf("foo"), errors.orEmpty().single().path)
      }
  }

  @Test
  fun noBubblesPlz(): Unit = runBlocking {
    val schema = """
            type Query {
                foo: String!
            }
            directive @noBubblesPlz on QUERY | MUTATION | SUBSCRIPTION 
        """.trimIndent()

    val document = """
            query GetFoo @noBubblesPlz {
                foo
            }
        """.trimIndent()

    ExecutableSchema.Builder()
      .schema(schema)
      .resolver {
        null
      }
      .build()
      .execute(document.toGraphQLRequest(), ExecutionContext.Empty)
      .apply {
        assertEquals(mapOf("foo" to null), data)
        assertEquals("A resolver returned null in a non-nullable position", errors.orEmpty().single().message)
        assertEquals(listOf("foo"), errors.orEmpty().single().path)
      }
  }
}