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
