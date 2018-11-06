package com.example.mutation_create_review

import com.apollographql.apollo.api.InputFieldMarshaller
import com.apollographql.apollo.api.InputFieldWriter
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseFieldMapper
import com.apollographql.apollo.api.ResponseFieldMarshaller
import com.apollographql.apollo.api.ResponseReader
import com.example.mutation_create_review.type.CustomType
import com.example.mutation_create_review.type.Episode
import com.example.mutation_create_review.type.ReviewInput
import java.io.IOException
import java.util.Date
import javax.annotation.Generated
import kotlin.Any
import kotlin.Array
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.jvm.Throws
import kotlin.jvm.Transient

@Generated("Apollo GraphQL")
data class CreateReviewForEpisodeMutation(val ep: Episode, val review: ReviewInput) : Mutation<CreateReviewForEpisodeMutation.Data, CreateReviewForEpisodeMutation.Data, Operation.Variables> {
    @Transient
    private val variables: Operation.Variables = object : Operation.Variables() {
        override fun valueMap(): Map<String, Any?> = mutableMapOf<String, Any?>().apply {
            this["ep"] = ep
            this["review"] = review
        }

        override fun marshaller(): InputFieldMarshaller = object : InputFieldMarshaller {
            @Throws(IOException::class)
            override fun marshal(writer: InputFieldWriter) {
                writer.writeString("ep", ep.value.rawValue)
                writer.writeObject("review", review.marshaller())
            }
        }
    }

    override fun operationId(): String = OPERATION_ID
    override fun queryDocument(): String = QUERY_DOCUMENT
    override fun wrapData(data: CreateReviewForEpisodeMutation.Data): CreateReviewForEpisodeMutation.Data = data
    override fun variables(): Operation.Variables = variables
    override fun name(): OperationName = OPERATION_NAME
    override fun responseFieldMapper(): ResponseFieldMapper<CreateReviewForEpisodeMutation.Data> = ResponseFieldMapper {
        CreateReviewForEpisodeMutation.Data(it)
    }

    /**
     * @param name The name of the character
     */
    data class ListOfListOfObject(
        val __typename: String,
        val name: String
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeString(RESPONSE_FIELDS[1], name)
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forString("name", "name", null, false, null)
                    )

            operator fun invoke(reader: ResponseReader): ListOfListOfObject {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                return ListOfListOfObject(
                    __typename = __typename,
                    name = name
                )
            }
        }
    }

    /**
     * @param stars The number of stars this review gave, 1-5
     * @param commentary Comment about the movie
     * @param listOfListOfString for test purpose only
     * @param listOfListOfEnum for test purpose only
     * @param listOfListOfCustom for test purpose only
     * @param listOfListOfObject for test purpose only
     */
    data class CreateReview(
        val __typename: String,
        val stars: Int,
        val commentary: String?,
        val listOfListOfString: List<List<String>>?,
        val listOfListOfEnum: List<List<Episode>>?,
        val listOfListOfCustom: List<List<Date>>?,
        val listOfListOfObject: List<List<ListOfListOfObject>>?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeInt(RESPONSE_FIELDS[1], stars)
            it.writeString(RESPONSE_FIELDS[2], commentary)
            it.writeList(RESPONSE_FIELDS[3], listOfListOfString) { value, listItemWriter ->
                @Suppress("NAME_SHADOWING")
                value?.forEach { value ->
                    listItemWriter.writeList(value) { value, listItemWriter ->
                        value?.forEach { value ->
                            listItemWriter.writeString(value)
                        }
                    }
                }
            }
            it.writeList(RESPONSE_FIELDS[4], listOfListOfEnum) { value, listItemWriter ->
                @Suppress("NAME_SHADOWING")
                value?.forEach { value ->
                    listItemWriter.writeList(value) { value, listItemWriter ->
                        value?.forEach { value ->
                            listItemWriter.writeString(value)
                        }
                    }
                }
            }
            it.writeList(RESPONSE_FIELDS[5], listOfListOfCustom) { value, listItemWriter ->
                @Suppress("NAME_SHADOWING")
                value?.forEach { value ->
                    listItemWriter.writeList(value) { value, listItemWriter ->
                        value?.forEach { value ->
                            listItemWriter.writeCustom(CustomType.DATE, value)
                        }
                    }
                }
            }
            it.writeList(RESPONSE_FIELDS[6], listOfListOfObject) { value, listItemWriter ->
                @Suppress("NAME_SHADOWING")
                value?.forEach { value ->
                    listItemWriter.writeList(value) { value, listItemWriter ->
                        value?.forEach { value ->
                            listItemWriter.writeObject(value?.marshaller())
                        }
                    }
                }
            }
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forInt("stars", "stars", null, false, null),
                    ResponseField.forString("commentary", "commentary", null, true, null),
                    ResponseField.forList("listOfListOfString", "listOfListOfString", null, true, null),
                    ResponseField.forList("listOfListOfEnum", "listOfListOfEnum", null, true, null),
                    ResponseField.forList("listOfListOfCustom", "listOfListOfCustom", null, true, null),
                    ResponseField.forList("listOfListOfObject", "listOfListOfObject", null, true, null)
                    )

            operator fun invoke(reader: ResponseReader): CreateReview {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val stars = reader.readInt(RESPONSE_FIELDS[1])
                val commentary = reader.readString(RESPONSE_FIELDS[2])
                val listOfListOfString = reader.readList<List<String>>(RESPONSE_FIELDS[3]) {
                    it.readList<String> {
                        it.readString()
                    }
                }
                val listOfListOfEnum = reader.readList<List<Episode>>(RESPONSE_FIELDS[4]) {
                    it.readList<Episode> {
                        Episode.safeValueOf(it.readString())
                    }
                }
                val listOfListOfCustom = reader.readList<List<Date>>(RESPONSE_FIELDS[5]) {
                    it.readList<Date> {
                        it.readCustomType<Date>(CustomType.DATE)
                    }
                }
                val listOfListOfObject = reader.readList<List<ListOfListOfObject>>(RESPONSE_FIELDS[6]) {
                    it.readList<ListOfListOfObject> {
                        it.readObject<ListOfListOfObject> {
                            ListOfListOfObject(it)
                        }

                    }
                }
                return CreateReview(
                    __typename = __typename,
                    stars = stars,
                    commentary = commentary,
                    listOfListOfString = listOfListOfString,
                    listOfListOfEnum = listOfListOfEnum,
                    listOfListOfCustom = listOfListOfCustom,
                    listOfListOfObject = listOfListOfObject
                )
            }
        }
    }

    data class Data(val createReview: CreateReview?) : Operation.Data {
        override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeObject(RESPONSE_FIELDS[0], createReview?.marshaller())
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forObject("createReview", "createReview", mapOf<String, Any>(
                        "episode" to mapOf<String, Any>(
                            "kind" to "Variable",
                            "variableName" to "ep"),
                        "review" to mapOf<String, Any>(
                            "kind" to "Variable",
                            "variableName" to "review")), true, null)
                    )

            operator fun invoke(reader: ResponseReader): Data {
                val createReview = reader.readObject<CreateReview>(RESPONSE_FIELDS[0]) {
                    CreateReview(it)
                }

                return Data(
                    createReview = createReview
                )
            }
        }
    }

    companion object {
        val OPERATION_DEFINITION: String = """
                |mutation CreateReviewForEpisode(${'$'}ep: Episode!, ${'$'}review: ReviewInput!) {
                |  createReview(episode: ${'$'}ep, review: ${'$'}review) {
                |    __typename
                |    stars
                |    commentary
                |    listOfListOfString
                |    listOfListOfEnum
                |    listOfListOfCustom
                |    listOfListOfObject {
                |      __typename
                |      name
                |    }
                |  }
                |}
                """.trimMargin()

        const val OPERATION_ID: String =
                "63f677799b4c5cfe580f133b71712c66179ea84077be680e999b0f1d1e66e0a1"

        val QUERY_DOCUMENT: String = """
                |mutation CreateReviewForEpisode(${'$'}ep: Episode!, ${'$'}review: ReviewInput!) {
                |  createReview(episode: ${'$'}ep, review: ${'$'}review) {
                |    __typename
                |    stars
                |    commentary
                |    listOfListOfString
                |    listOfListOfEnum
                |    listOfListOfCustom
                |    listOfListOfObject {
                |      __typename
                |      name
                |    }
                |  }
                |}
                """.trimMargin()

        val OPERATION_NAME: OperationName = OperationName { "CreateReviewForEpisodeMutation" }
    }
}
