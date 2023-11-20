package test

import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.execution.ExecutableSchema
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import kotlin.test.Test

class IntrospectionTest {
  @Test
  fun introspection() {
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
    println(response.data)
    println(response.errors)
  }
}
