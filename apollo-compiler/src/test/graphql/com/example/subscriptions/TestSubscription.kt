package com.example.subscriptions

import com.apollographql.apollo.api.InputFieldMarshaller
import com.apollographql.apollo.api.InputFieldWriter
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseFieldMapper
import com.apollographql.apollo.api.ResponseFieldMarshaller
import com.apollographql.apollo.api.ResponseReader
import com.apollographql.apollo.api.Subscription
import java.io.IOException
import javax.annotation.Generated
import kotlin.Any
import kotlin.Array
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.Map
import kotlin.jvm.Throws
import kotlin.jvm.Transient

@Generated("Apollo GraphQL")
@Suppress("NAME_SHADOWING", "LocalVariableName")
data class TestSubscription(val repo: String) : Subscription<TestSubscription.Data, TestSubscription.Data, Operation.Variables> {
    @Transient
    private val variables: Operation.Variables = object : Operation.Variables() {
        override fun valueMap(): Map<String, Any?> = mutableMapOf<String, Any?>().apply {
            this["repo"] = repo
        }

        override fun marshaller(): InputFieldMarshaller = object : InputFieldMarshaller {
            @Throws(IOException::class)
            override fun marshal(writer: InputFieldWriter) {
                writer.writeString("repo", repo)
            }
        }
    }

    override fun operationId(): String = OPERATION_ID
    override fun queryDocument(): String = QUERY_DOCUMENT
    override fun wrapData(data: TestSubscription.Data): TestSubscription.Data = data
    override fun variables(): Operation.Variables = variables
    override fun name(): OperationName = OPERATION_NAME
    override fun responseFieldMapper(): ResponseFieldMapper<TestSubscription.Data> = ResponseFieldMapper {
        TestSubscription.Data(it)
    }

    /**
     * @param id The SQL ID of this entry
     * @param content The text of the comment
     */
    data class CommentAdded(
        val __typename: String,
        val id: Int,
        val content: String
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeInt(RESPONSE_FIELDS[1], id)
            it.writeString(RESPONSE_FIELDS[2], content)
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forInt("id", "id", null, false, null),
                    ResponseField.forString("content", "content", null, false, null)
                    )

            operator fun invoke(reader: ResponseReader): CommentAdded {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val id = reader.readInt(RESPONSE_FIELDS[1])
                val content = reader.readString(RESPONSE_FIELDS[2])
                return CommentAdded(
                    __typename = __typename,
                    id = id,
                    content = content
                )
            }
        }
    }

    /**
     * @param commentAdded Subscription fires on every comment added
     */
    data class Data(
        val commentAdded: CommentAdded?
    ) : Operation.Data {
        override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeObject(RESPONSE_FIELDS[0], commentAdded?.marshaller())
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forObject("commentAdded", "commentAdded", mapOf<String, Any>(
                        "repoFullName" to mapOf<String, Any>(
                            "kind" to "Variable",
                            "variableName" to "repo")), true, null)
                    )

            operator fun invoke(reader: ResponseReader): Data {
                val commentAdded = reader.readObject<CommentAdded>(RESPONSE_FIELDS[0]) { reader ->
                    CommentAdded(reader)
                }

                return Data(
                    commentAdded = commentAdded
                )
            }
        }
    }

    companion object {
        val OPERATION_DEFINITION: String = """
                |subscription TestSubscription(${'$'}repo: String!) {
                |  commentAdded(repoFullName: ${'$'}repo) {
                |    __typename
                |    id
                |    content
                |  }
                |}
                """.trimMargin()

        const val OPERATION_ID: String =
                "8f1972cf9af58c4659da0ae72d02b97faf5fa6e6b794070d2cbcb034e2881fb8"

        val QUERY_DOCUMENT: String = """
                |subscription TestSubscription(${'$'}repo: String!) {
                |  commentAdded(repoFullName: ${'$'}repo) {
                |    __typename
                |    id
                |    content
                |  }
                |}
                """.trimMargin()

        val OPERATION_NAME: OperationName = OperationName { "TestSubscription" }
    }
}
