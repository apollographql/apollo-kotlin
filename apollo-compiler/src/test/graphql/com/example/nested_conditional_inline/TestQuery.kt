package com.example.nested_conditional_inline

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
import com.example.nested_conditional_inline.type.Episode
import java.io.IOException
import javax.annotation.Generated
import kotlin.Any
import kotlin.Array
import kotlin.Double
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.jvm.Throws
import kotlin.jvm.Transient

@Generated("Apollo GraphQL")
@Suppress("NAME_SHADOWING", "LocalVariableName")
data class TestQuery(val episode: Input<Episode>) : Query<TestQuery.Data, TestQuery.Data, Operation.Variables> {
    @Transient
    private val variables: Operation.Variables = object : Operation.Variables() {
        override fun valueMap(): Map<String, Any?> = mutableMapOf<String, Any?>().apply {
            if (episode.defined) this["episode"] = episode.value
        }

        override fun marshaller(): InputFieldMarshaller = object : InputFieldMarshaller {
            @Throws(IOException::class)
            override fun marshal(writer: InputFieldWriter) {
                if (episode.defined) writer.writeString("episode", episode.value?.rawValue)
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
    data class AsHuman1(
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

            operator fun invoke(reader: ResponseReader): AsHuman1 {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                val height = reader.readDouble(RESPONSE_FIELDS[2])
                return AsHuman1(
                    __typename = __typename,
                    name = name,
                    height = height
                )
            }
        }
    }

    /**
     * @param name The name of the character
     */
    data class Friend(
        val __typename: String,
        val name: String,
        val asHuman1: AsHuman1?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeString(RESPONSE_FIELDS[1], name)
            it.writeObject(RESPONSE_FIELDS[2], asHuman1?.marshaller())
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forString("name", "name", null, false, null),
                    ResponseField.forInlineFragment("__typename", "__typename", listOf("Human"))
                    )

            operator fun invoke(reader: ResponseReader): Friend {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                val asHuman1 = reader.readConditional(RESPONSE_FIELDS[2]) { conditionalType, reader ->
                    AsHuman1(reader)
                }

                return Friend(
                    __typename = __typename,
                    name = name,
                    asHuman1 = asHuman1
                )
            }
        }
    }

    /**
     * @param name What this human calls themselves
     * @param friends This human's friends, or an empty list if they have none
     */
    data class AsHuman(
        val __typename: String,
        val name: String,
        val friends: List<Friend?>?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeString(RESPONSE_FIELDS[1], name)
            it.writeList(RESPONSE_FIELDS[2], friends) { value, listItemWriter ->
                value?.forEach { value ->
                    listItemWriter.writeObject(value?.marshaller())
                }
            }
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forString("name", "name", null, false, null),
                    ResponseField.forList("friends", "friends", null, true, null)
                    )

            operator fun invoke(reader: ResponseReader): AsHuman {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                val friends = reader.readList<Friend>(RESPONSE_FIELDS[2]) {
                    it.readObject<Friend> { reader ->
                        Friend(reader)
                    }

                }
                return AsHuman(
                    __typename = __typename,
                    name = name,
                    friends = friends
                )
            }
        }
    }

    /**
     * @param name What this human calls themselves
     * @param height Height in the preferred unit, default is meters
     */
    data class AsHuman12(
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
                        "unit" to "METER"), true, null)
                    )

            operator fun invoke(reader: ResponseReader): AsHuman12 {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                val height = reader.readDouble(RESPONSE_FIELDS[2])
                return AsHuman12(
                    __typename = __typename,
                    name = name,
                    height = height
                )
            }
        }
    }

    /**
     * @param name The name of the character
     */
    data class Friend1(
        val __typename: String,
        val name: String,
        val asHuman12: AsHuman12?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeString(RESPONSE_FIELDS[1], name)
            it.writeObject(RESPONSE_FIELDS[2], asHuman12?.marshaller())
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forString("name", "name", null, false, null),
                    ResponseField.forInlineFragment("__typename", "__typename", listOf("Human"))
                    )

            operator fun invoke(reader: ResponseReader): Friend1 {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                val asHuman12 = reader.readConditional(RESPONSE_FIELDS[2]) { conditionalType, reader ->
                    AsHuman12(reader)
                }

                return Friend1(
                    __typename = __typename,
                    name = name,
                    asHuman12 = asHuman12
                )
            }
        }
    }

    /**
     * @param name What others call this droid
     * @param friends This droid's friends, or an empty list if they have none
     */
    data class AsDroid(
        val __typename: String,
        val name: String,
        val friends: List<Friend1?>?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeString(RESPONSE_FIELDS[1], name)
            it.writeList(RESPONSE_FIELDS[2], friends) { value, listItemWriter ->
                value?.forEach { value ->
                    listItemWriter.writeObject(value?.marshaller())
                }
            }
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forString("name", "name", null, false, null),
                    ResponseField.forList("friends", "friends", null, true, null)
                    )

            operator fun invoke(reader: ResponseReader): AsDroid {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                val friends = reader.readList<Friend1>(RESPONSE_FIELDS[2]) {
                    it.readObject<Friend1> { reader ->
                        Friend1(reader)
                    }

                }
                return AsDroid(
                    __typename = __typename,
                    name = name,
                    friends = friends
                )
            }
        }
    }

    /**
     * @param name The name of the character
     */
    data class Hero(
        val __typename: String,
        val name: String,
        val asHuman: AsHuman?,
        val asDroid: AsDroid?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeString(RESPONSE_FIELDS[1], name)
            it.writeObject(RESPONSE_FIELDS[2], asHuman?.marshaller())
            it.writeObject(RESPONSE_FIELDS[3], asDroid?.marshaller())
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forString("name", "name", null, false, null),
                    ResponseField.forInlineFragment("__typename", "__typename", listOf("Human")),
                    ResponseField.forInlineFragment("__typename", "__typename", listOf("Droid"))
                    )

            operator fun invoke(reader: ResponseReader): Hero {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                val asHuman = reader.readConditional(RESPONSE_FIELDS[2]) { conditionalType, reader ->
                    AsHuman(reader)
                }

                val asDroid = reader.readConditional(RESPONSE_FIELDS[3]) { conditionalType, reader ->
                    AsDroid(reader)
                }

                return Hero(
                    __typename = __typename,
                    name = name,
                    asHuman = asHuman,
                    asDroid = asDroid
                )
            }
        }
    }

    data class Data(val hero: Hero?) : Operation.Data {
        override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeObject(RESPONSE_FIELDS[0], hero?.marshaller())
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forObject("hero", "hero", mapOf<String, Any>(
                        "episode" to mapOf<String, Any>(
                            "kind" to "Variable",
                            "variableName" to "episode")), true, null)
                    )

            operator fun invoke(reader: ResponseReader): Data {
                val hero = reader.readObject<Hero>(RESPONSE_FIELDS[0]) { reader ->
                    Hero(reader)
                }

                return Data(
                    hero = hero
                )
            }
        }
    }

    companion object {
        val OPERATION_DEFINITION: String = """
                |query TestQuery(${'$'}episode: Episode) {
                |  hero(episode: ${'$'}episode) {
                |    __typename
                |    name
                |    ... on Human {
                |      friends {
                |        __typename
                |        name
                |        ... on Human {
                |          height(unit: FOOT)
                |        }
                |      }
                |    }
                |    ... on Droid {
                |      friends {
                |        __typename
                |        name
                |        ... on Human {
                |          height(unit: METER)
                |        }
                |      }
                |    }
                |  }
                |}
                """.trimMargin()

        const val OPERATION_ID: String =
                "071e064b3415e8b92bed3befa46bf04501c7194cde77ede0ebf50429624796cc"

        val QUERY_DOCUMENT: String = """
                |query TestQuery(${'$'}episode: Episode) {
                |  hero(episode: ${'$'}episode) {
                |    __typename
                |    name
                |    ... on Human {
                |      friends {
                |        __typename
                |        name
                |        ... on Human {
                |          height(unit: FOOT)
                |        }
                |      }
                |    }
                |    ... on Droid {
                |      friends {
                |        __typename
                |        name
                |        ... on Human {
                |          height(unit: METER)
                |        }
                |      }
                |    }
                |  }
                |}
                """.trimMargin()

        val OPERATION_NAME: OperationName = OperationName { "TestQuery" }
    }
}
