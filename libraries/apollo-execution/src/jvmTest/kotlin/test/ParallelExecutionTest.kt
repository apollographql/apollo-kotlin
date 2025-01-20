@file:OptIn(ExperimentalCoroutinesApi::class)

package test

import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.execution.ExecutableSchema
import com.apollographql.apollo.execution.toGraphQLRequest
import kotlinx.coroutines.*
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.times

class ParallelExecutionTest {
  @Test
  fun fieldsAreExecutedConcurrently() {
    // language=graphql
    val schema = """
      type Query {
        field1: Int
        field2: Int
        field3: Int
      }
    """.trimIndent()

    val items = mutableListOf<Int>()
    val executableSchema = ExecutableSchema.Builder()
      .schema(schema.toGQLDocument())
      .resolver {
        val item = when (it.field.name) {
          "field1" -> 1
          "field2" -> 2
          "field3" -> 3
          else -> error("Unknown field '${it.field.name}'")
        }

        // Return the items in opposite order
        delay((3 - item) * 200.milliseconds)
        items.add(item)
        item
      }
      .build()

    val response = runBlocking {
      executableSchema.execute(
        """
        {field1 field2 field3}
      """.trimIndent().toGraphQLRequest()
      )
    }

    assertEquals(mapOf("field1" to 1, "field2" to 2, "field3" to 3), response.data)
    assertEquals(listOf(3, 2, 1), items)
  }

  class Batch {
    val deferred = mutableMapOf<String, CompletableDeferred<String>>()
  }

  class Loader(private val dispatcher: CoroutineDispatcher) {
    private var batch: Batch? = null

    suspend fun load(id: String): String {
      if (batch == null) {
        batch = Batch()
        dispatcher.dispatch(EmptyCoroutineContext) {
          assertEquals(4, batch!!.deferred.size)
          batch!!.deferred.forEach {
            it.value.complete("item-${it.key}")
          }
          batch = null
        }
      }

      val deferred = CompletableDeferred<String>()
      batch!!.deferred.put(id, deferred)

      return deferred.await()
    }
  }

  @Test
  fun subFieldAreStartedFromTheSameStackFrame() {
    val schema = """
      type Query {
        field1: Field
        field2: Field
      }
      type Field {
        subField1: String
        subField2: String
      }
    """.trimIndent()

    var inFirstFrame = true
    var dispatch = true
    val dispatcher = Dispatchers.Default.limitedParallelism(1)
    val loader = Loader(dispatcher)

    val executableSchema = ExecutableSchema.Builder()
      .schema(schema.toGQLDocument())
      .resolver {
        if (inFirstFrame) {
          if (dispatch) {
            dispatch = false
            dispatcher.dispatch(EmptyCoroutineContext) {
              inFirstFrame = false
            }
          }
        }

        check(inFirstFrame)

        if (it.field.name.startsWith("field")) {
          return@resolver it.field.name.substring(5).toInt()
        }

        check(it.field.name.startsWith("subField"))

        loader.load("${it.parentObject}-${it.field.name.substring(8)}")
      }
      .build()

    runBlocking(dispatcher) {
      val response = executableSchema.execute(
        """
        {
            field1 {
                subField1
                subField2            
            }
            field2 {
                subField1
                subField2            
            }
        }
      """.trimIndent().toGraphQLRequest()
      )

      assertEquals(
        mapOf(
          "field1" to mapOf(
            "subField1" to "item-1-1",
            "subField2" to "item-1-2",
          ),
          "field2" to mapOf(
            "subField1" to "item-2-1",
            "subField2" to "item-2-2",
          ),
        ), response.data
      )
    }
  }

  @Test
  fun listItemsAreExecutedInParallel() {
    val schema = """
      type Query {
        items: [Item!]!
      }
      type Item {
        id: ID!
      }
    """.trimIndent()

        val executableSchema = ExecutableSchema.Builder()
      .schema(schema.toGQLDocument())
      .resolver {
        return@resolver when (it.fieldName) {
          "items" -> {
            0.until(100).toList()
          }
          "id" -> {
            delay(500)
            it.parentObject.toString()
          }
          else -> {
            error("Unknown field '${it.fieldName}'")
          }
        }
      }
      .build()

    runBlocking {
      val response = withTimeout(1000) {
        executableSchema.execute("{ items { id } }".toGraphQLRequest())
      }
      @Suppress("UNCHECKED_CAST")
      assertEquals(0.until(100).map { mapOf("id" to it.toString()) }, (response.data as Map<String, Any?>).get("items"))
    }
  }
}