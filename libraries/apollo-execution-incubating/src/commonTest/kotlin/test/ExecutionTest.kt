package test

import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.ast.GQLListType
import com.apollographql.apollo3.ast.GQLNamedType
import com.apollographql.apollo3.ast.GQLNonNullType
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLScalarTypeDefinition
import com.apollographql.apollo3.ast.GQLType
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.execution.ExecutableSchema
import com.apollographql.apollo3.execution.GraphQLRequest
import com.apollographql.apollo3.execution.MainResolver
import com.apollographql.apollo3.execution.ResolveInfo
import com.apollographql.apollo3.execution.Resolver
import kotlin.test.Test

private val randomResolver = object : MainResolver {
    fun GQLType.randomValue(schema: Schema): Any? {
        return when (this) {
            is GQLNonNullType -> type.randomValue(schema)
            is GQLListType -> listOf(type.randomValue(schema))
            is GQLNamedType -> {
                when (schema.typeDefinition(name)) {
                    is GQLObjectTypeDefinition -> mapOf("__typename" to name)
                    is GQLScalarTypeDefinition -> when (name) {
                        "String" -> "Hello"
                        "Int" -> 42
                        "Float" -> 3.0
                        "Boolean" -> true
                        else -> error("No scalar for type '$name'")
                    }
                    else -> error("No random value for type '$name'")
                }
            }
        }
    }

    override fun resolve(resolveInfo: ResolveInfo): Any? {
        val type = resolveInfo.fieldDefinition().type

        return type.randomValue(resolveInfo.schema)
    }

    override fun typename(obj: Any): String? {
        @Suppress("UNCHECKED_CAST")
        return (obj as Map<String, String?>).get("__typename")
    }
}

internal fun String.toGraphQLRequest(): GraphQLRequest = GraphQLRequest.Builder()
    .document(this)
    .build()

class ExecutionTest {

    @Test
    fun simple() {
        // language=graphql
        val schema = """
            type Query {
                foo: String!
            }
        """.trimIndent()

        // language=graphql
        val document = """
            {
                foo
            }
        """.trimIndent()

        val simpleMainResolver = object : MainResolver {
            override fun resolve(resolveInfo: ResolveInfo): Any? {
                if (resolveInfo.parentType != "Query" || resolveInfo.fieldName != "foo") return null
                return Resolver { 42 }
            }

            override fun typename(obj: Any): String? {
                return null
            }
        }

        val response = ExecutableSchema.Builder()
            .schema(schema)
            .resolver(simpleMainResolver)
            .build()
            .execute(document.toGraphQLRequest(), ExecutionContext.Empty)
        println(response.data)
        println(response.errors)
    }

    @Test
    fun argument() {
        val schema = """
            type Query {
                foo(first: Int): String!
            }
        """.trimIndent()

        val document = """
            {
                foo(first = ${'$'}first)
            }
        """.trimIndent()


        val response = ExecutableSchema.Builder()
            .schema(schema)
            .resolver(randomResolver)
            .build()
            .execute(document.toGraphQLRequest(), ExecutionContext.Empty)
        println(response.data)
        println(response.errors)
    }
}