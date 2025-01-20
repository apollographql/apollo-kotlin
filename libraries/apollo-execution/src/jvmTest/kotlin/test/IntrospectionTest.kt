package test

import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.execution.ExecutableSchema
import com.apollographql.apollo.execution.toGraphQLRequest
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import kotlin.test.Test
import kotlin.test.assertNull

class IntrospectionTest {
  @Test
  fun introspection() = runBlocking {
    val schema = """
            type Query {
                foo: String!
            }
        """.trimIndent()


    val document =
        FileSystem.SYSTEM.openReadOnly("testFixtures/introspection.graphql".toPath()).source().buffer().readUtf8()

    val response = ExecutableSchema.Builder()
        .schema(schema)
        .build()
        .execute(
            document.toGraphQLRequest(),
            ExecutionContext.Empty
        )
    assertNull(response.errors)
  }
}
