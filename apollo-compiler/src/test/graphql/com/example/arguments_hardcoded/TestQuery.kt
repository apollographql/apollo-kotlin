package com.example.arguments_hardcoded

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseFieldMapper
import com.apollographql.apollo.api.ResponseFieldMarshaller
import com.apollographql.apollo.api.ResponseReader
import javax.annotation.Generated
import kotlin.Array
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Generated("Apollo GraphQL")
@Suppress("NAME_SHADOWING", "LocalVariableName")
class TestQuery : Query<TestQuery.Data, TestQuery.Data, Operation.Variables> {
    override fun operationId(): String = OPERATION_ID
    override fun queryDocument(): String = QUERY_DOCUMENT
    override fun wrapData(data: TestQuery.Data): TestQuery.Data = data
    override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES
    override fun name(): OperationName = OPERATION_NAME
    override fun responseFieldMapper(): ResponseFieldMapper<TestQuery.Data> = ResponseFieldMapper {
        TestQuery.Data(it)
    }

    /**
     * @param stars The number of stars this review gave, 1-5
     * @param commentary Comment about the movie
     */
    data class Review(
        val __typename: String,
        val stars: Int,
        val commentary: String?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeInt(RESPONSE_FIELDS[1], stars)
            it.writeString(RESPONSE_FIELDS[2], commentary)
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forInt("stars", "stars", null, false, null),
                    ResponseField.forString("commentary", "commentary", null, true, null)
                    )

            operator fun invoke(reader: ResponseReader): Review {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val stars = reader.readInt(RESPONSE_FIELDS[1])
                val commentary = reader.readString(RESPONSE_FIELDS[2])
                return Review(
                    __typename = __typename,
                    stars = stars,
                    commentary = commentary
                )
            }
        }
    }

    data class Data(val reviews: List<Review?>?) : Operation.Data {
        override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeList(RESPONSE_FIELDS[0], reviews) { value, listItemWriter ->
                value?.forEach { value ->
                    listItemWriter.writeObject(value?.marshaller())
                }
            }
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forList("reviews", "reviews", mapOf<String, Any>(
                        "episode" to "JEDI",
                        "starsInt" to "10",
                        "starsFloat" to "9.9"), true, null)
                    )

            operator fun invoke(reader: ResponseReader): Data {
                val reviews = reader.readList<Review>(RESPONSE_FIELDS[0]) {
                    it.readObject<Review> { reader ->
                        Review(reader)
                    }

                }
                return Data(
                    reviews = reviews
                )
            }
        }
    }

    companion object {
        val OPERATION_DEFINITION: String = """
                |query TestQuery {
                |  reviews(episode: JEDI, starsInt: 10, starsFloat: 9.9) {
                |    __typename
                |    stars
                |    commentary
                |  }
                |}
                """.trimMargin()

        const val OPERATION_ID: String =
                "a6746506c2fb20405972e7e76920eec4aa2e5e02dc429cfbd1585a4f1787b0d9"

        val QUERY_DOCUMENT: String = """
                |query TestQuery {
                |  reviews(episode: JEDI, starsInt: 10, starsFloat: 9.9) {
                |    __typename
                |    stars
                |    commentary
                |  }
                |}
                """.trimMargin()

        val OPERATION_NAME: OperationName = OperationName { "TestQuery" }
    }
}
