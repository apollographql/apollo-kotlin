package com.example.arguments_complex

import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.InputFieldMarshaller
import com.apollographql.apollo.api.InputFieldWriter
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseFieldMapper
import com.apollographql.apollo.api.ResponseFieldMarshaller
import com.apollographql.apollo.api.ResponseReader
import com.example.arguments_complex.type.Episode
import java.io.IOException
import javax.annotation.Generated
import kotlin.Any
import kotlin.Array
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.Map
import kotlin.jvm.Throws
import kotlin.jvm.Transient

@Generated("Apollo GraphQL")
@Suppress("NAME_SHADOWING", "LocalVariableName")
data class TestQuery(
    val episode: Input<Episode>,
    val stars: Int,
    val greenValue: Double
) : Query<TestQuery.Data, TestQuery.Data, Operation.Variables> {
    @Transient
    private val variables: Operation.Variables = object : Operation.Variables() {
        override fun valueMap(): Map<String, Any?> = mutableMapOf<String, Any?>().apply {
            if (episode.defined) this["episode"] = episode.value
            this["stars"] = stars
            this["greenValue"] = greenValue
        }

        override fun marshaller(): InputFieldMarshaller = object : InputFieldMarshaller {
            @Throws(IOException::class)
            override fun marshal(writer: InputFieldWriter) {
                if (episode.defined) writer.writeString("episode", episode.value?.rawValue)
                writer.writeInt("stars", stars)
                writer.writeDouble("greenValue", greenValue)
            }
        }
    }

    override fun operationId(): String = OPERATION_ID
    override fun queryDocument(): String = QUERY_DOCUMENT
    override fun wrapData(data: TestQuery.Data): TestQuery.Data = data
    override fun variables(): Operation.Variables = variables
    override fun name(): OperationName = OPERATION_NAME
    override fun responseFieldMapper(): ResponseFieldMapper<TestQuery.Data> = ResponseFieldMapper {
        TestQuery.Data(it)
    }

    /**
     * @param name What this human calls themselves
     * @param height Height in the preferred unit, default is meters
     */
    data class HeroWithReview(
        val __typename: String,
        val name: String,
        val height: Double?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeString(RESPONSE_FIELDS[1], name)
            it.writeDouble(RESPONSE_FIELDS[2], height)
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forString("name", "name", null, false, null),
                    ResponseField.forDouble("height", "height", mapOf<String, Any>(
                        "unit" to "FOOT"), true, null)
                    )

            operator fun invoke(reader: ResponseReader): HeroWithReview {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                val height = reader.readDouble(RESPONSE_FIELDS[2])
                return HeroWithReview(
                    __typename = __typename,
                    name = name,
                    height = height
                )
            }
        }
    }

    data class Data(val heroWithReview: HeroWithReview?) : Operation.Data {
        override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeObject(RESPONSE_FIELDS[0], heroWithReview?.marshaller())
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forObject("heroWithReview", "heroWithReview", mapOf<String, Any>(
                        "episode" to mapOf<String, Any>(
                            "kind" to "Variable",
                            "variableName" to "episode"),
                        "review" to mapOf<String, Any>(
                            "stars" to mapOf<String, Any>(
                                "kind" to "Variable",
                                "variableName" to "stars"),
                            "favoriteColor" to mapOf<String, Any>(
                                "red" to "0.0",
                                "green" to mapOf<String, Any>(
                                    "kind" to "Variable",
                                    "variableName" to "greenValue"),
                                "blue" to "0.0"))), true, null)
                    )

            operator fun invoke(reader: ResponseReader): Data {
                val heroWithReview = reader.readObject<HeroWithReview>(RESPONSE_FIELDS[0]) { reader ->
                    HeroWithReview(reader)
                }

                return Data(
                    heroWithReview = heroWithReview
                )
            }
        }
    }

    companion object {
        val OPERATION_DEFINITION: String = """
                |query TestQuery(${'$'}episode: Episode, ${'$'}stars: Int!, ${'$'}greenValue: Float!) {
                |  heroWithReview(episode: ${'$'}episode, review: {stars: ${'$'}stars, favoriteColor: {red: 0, green: ${'$'}greenValue, blue: 0}}) {
                |    __typename
                |    name
                |    height(unit: FOOT)
                |  }
                |}
                """.trimMargin()

        const val OPERATION_ID: String =
                "4905a0fccc07f97ecd6d660f5a68d4d49ffedc3f4688b76d17288dec1a1fdf93"

        val QUERY_DOCUMENT: String = """
                |query TestQuery(${'$'}episode: Episode, ${'$'}stars: Int!, ${'$'}greenValue: Float!) {
                |  heroWithReview(episode: ${'$'}episode, review: {stars: ${'$'}stars, favoriteColor: {red: 0, green: ${'$'}greenValue, blue: 0}}) {
                |    __typename
                |    name
                |    height(unit: FOOT)
                |  }
                |}
                """.trimMargin()

        val OPERATION_NAME: OperationName = OperationName { "TestQuery" }
    }
}
