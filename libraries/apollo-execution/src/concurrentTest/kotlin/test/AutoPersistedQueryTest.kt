package test

import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.execution.ExecutableSchema
import com.apollographql.apollo.execution.GraphQLRequest
import com.apollographql.apollo.execution.InMemoryPersistedDocumentCache
import kotlinx.coroutines.runBlocking
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutoPersistedQueryTest {
    private val schema = """
            type Query {
                foo: String!
            }
        """.trimIndent()

    private val document = """
            query GetFoo {
              __typename
            }
        """.trimIndent()

    @Test
    fun queryIsPersisted() = runBlocking {
        val executableSchema = ExecutableSchema.Builder()
            .schema(schema)
            .persistedDocumentCache(InMemoryPersistedDocumentCache())
            .build()

        val response1 = executableSchema.execute(
            GraphQLRequest.Builder()
                .extensions(mapOf("persistedQuery" to mapOf("sha256Hash" to document.sha256())))
                .build(),
            ExecutionContext.Empty
        )

        assertEquals(1, response1.errors?.size)
        assertEquals("PersistedQueryNotFound", response1.errors!!.first().message)

        val response2 = executableSchema.execute(
            GraphQLRequest.Builder()
                .document(document)
                .extensions(mapOf("persistedQuery" to mapOf("sha256Hash" to document.sha256())))
                .build(),
            ExecutionContext.Empty
        )

        assertTrue(response2.errors.orEmpty().isEmpty())
    }

    @Test
    fun errorIsReceivedIfNoCache() = runBlocking {
        val executableSchema = ExecutableSchema.Builder()
            .schema(schema)
            .build()

        val response = executableSchema.execute(
            GraphQLRequest.Builder()
                .extensions(mapOf("persistedQuery" to mapOf("sha256Hash" to document.sha256())))
                .build(),
            ExecutionContext.Empty
        )

        assertEquals(1, response.errors?.size)
        assertEquals("PersistedQueryNotSupported", response.errors!!.first().message)

    }

    private fun String.sha256(): String {
        return Buffer().apply { writeUtf8(this@sha256) }.sha256().hex()
    }
}