package test

import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.json.buildJsonString
import com.apollographql.apollo.api.json.writeAny
import com.apollographql.apollo.api.json.writeObject
import com.apollographql.apollo.execution.ExecutableSchema
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IntrospectionTest {
  @Test
  fun introspectionSucceeds() = runBlocking {
    val schema = """
            type Query {
                foo: String!
            }
        """.trimIndent()


    val document =
      FileSystem.SYSTEM.openReadOnly("testFixtures/introspection-query.graphql".toPath()).source().buffer().readUtf8()

    val response = ExecutableSchema.Builder()
        .schema(schema)
        .build()
        .execute(
            document.toGraphQLRequest(),
            ExecutionContext.Empty
        )
    checkExpected(File("testFixtures/introspection-response.json"), buildJsonString(indent = "  ") {
      writeObject {
        name("data")
        writeAny(response.data)
        name("errors")
        writeAny(response.errors)
      }
    })
  }

  @Test
  fun serviceCapabilitiesCanBeDisabled() = runBlocking {
    val schema = """
            type Query {
                foo: String!
            }
        """.trimIndent()

    val executableSchema = ExecutableSchema.Builder()
        .schema(schema)
        .exposeServiceCapabilities(false)
        .build()

    val typeNamesResponse = executableSchema.execute(
        "{ __schema { types { name } } }".toGraphQLRequest(),
        ExecutionContext.Empty
    )
    @Suppress("UNCHECKED_CAST")
    val data = typeNamesResponse.data as Map<String, Any?>
    @Suppress("UNCHECKED_CAST")
    val schemaData = data["__schema"] as Map<String, Any?>
    @Suppress("UNCHECKED_CAST")
    val typeNames = (schemaData["types"] as List<Map<String, Any?>>).map { it["name"] }
    assertEquals(false, typeNames.contains("__Service"))
    assertEquals(false, typeNames.contains("__Capability"))

    val serviceResponse = executableSchema.execute(
        "{ __service { description } }".toGraphQLRequest(),
        ExecutionContext.Empty
    )
    assertNull(serviceResponse.data)
    assert(serviceResponse.errors?.isNotEmpty() == true) {
      "Expected an error when querying `__service` with exposeServiceCapabilities(false)"
    }
  }
}

internal fun checkExpected(expectedFile: File, actual: String) {
  val expected = try {
    expectedFile.readText()
  } catch (e: Exception) {
    null
  }

  if (shouldUpdateTestFixtures()) {
    expectedFile.writeText(actual)
  } else {
    assertEquals(expected, actual)
  }
}

fun shouldUpdateTestFixtures() = System.getenv("updateTestFixtures") != null
