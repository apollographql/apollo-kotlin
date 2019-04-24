// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.nested_conditional_inline

import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.InputFieldMarshaller
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseFieldMapper
import com.apollographql.apollo.api.ResponseFieldMarshaller
import com.apollographql.apollo.api.ResponseReader
import com.example.nested_conditional_inline.type.Episode
import kotlin.Any
import kotlin.Array
import kotlin.Double
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.jvm.Transient

@Suppress("NAME_SHADOWING", "LocalVariableName", "RemoveExplicitTypeArguments")
data class TestQuery(val episode: Input<Episode>) : Query<TestQuery.Data, TestQuery.Data,
        Operation.Variables> {
    @Transient
    private val variables: Operation.Variables = object : Operation.Variables() {
        override fun valueMap(): Map<String, Any?> = mutableMapOf<String, Any?>().apply {
            if (episode.defined) this["episode"] = episode.value
        }

        override fun marshaller(): InputFieldMarshaller = InputFieldMarshaller { writer ->
            if (episode.defined) writer.writeString("episode", episode.value?.rawValue)
        }
    }

    override fun operationId(): String = OPERATION_ID
    override fun queryDocument(): String = QUERY_DOCUMENT
    override fun wrapData(data: Data): Data = data
    override fun variables(): Operation.Variables = variables
    override fun name(): OperationName = OPERATION_NAME
    override fun responseFieldMapper(): ResponseFieldMapper<Data> = ResponseFieldMapper {
        Data(it)
    }

    interface HeroCharacter {
        fun marshaller(): ResponseFieldMarshaller
    }

    interface FriendCharacter {
        fun marshaller(): ResponseFieldMarshaller
    }

    data class AsHuman1(
        val __typename: String,
        /**
         * What this human calls themselves
         */
        val name: String,
        /**
         * Height in the preferred unit, default is meters
         */
        val height: Double?
    ) : FriendCharacter {
        override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
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

    data class Friend(
        val __typename: String,
        /**
         * The name of the character
         */
        val name: String,
        val inlineFragment: FriendCharacter?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeString(RESPONSE_FIELDS[1], name)
            it.writeObject(RESPONSE_FIELDS[2], inlineFragment?.marshaller())
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
                val inlineFragment = reader.readConditional(RESPONSE_FIELDS[2]) { conditionalType,
                        reader ->
                    when(conditionalType) {
                        in listOf("Human") -> AsHuman1(reader)
                        else -> null
                    }
                }

                return Friend(
                    __typename = __typename,
                    name = name,
                    inlineFragment = inlineFragment
                )
            }
        }
    }

    data class AsHuman(
        val __typename: String,
        /**
         * What this human calls themselves
         */
        val name: String,
        /**
         * This human's friends, or an empty list if they have none
         */
        val friends: List<Friend?>?
    ) : HeroCharacter {
        override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
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

    interface FriendCharacter1 {
        fun marshaller(): ResponseFieldMarshaller
    }

    data class AsHuman2(
        val __typename: String,
        /**
         * What this human calls themselves
         */
        val name: String,
        /**
         * Height in the preferred unit, default is meters
         */
        val height: Double?
    ) : FriendCharacter1 {
        override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
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

            operator fun invoke(reader: ResponseReader): AsHuman2 {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                val height = reader.readDouble(RESPONSE_FIELDS[2])
                return AsHuman2(
                    __typename = __typename,
                    name = name,
                    height = height
                )
            }
        }
    }

    data class Friend1(
        val __typename: String,
        /**
         * The name of the character
         */
        val name: String,
        val inlineFragment: FriendCharacter1?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeString(RESPONSE_FIELDS[1], name)
            it.writeObject(RESPONSE_FIELDS[2], inlineFragment?.marshaller())
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
                val inlineFragment = reader.readConditional(RESPONSE_FIELDS[2]) { conditionalType,
                        reader ->
                    when(conditionalType) {
                        in listOf("Human") -> AsHuman2(reader)
                        else -> null
                    }
                }

                return Friend1(
                    __typename = __typename,
                    name = name,
                    inlineFragment = inlineFragment
                )
            }
        }
    }

    data class AsDroid(
        val __typename: String,
        /**
         * What others call this droid
         */
        val name: String,
        /**
         * This droid's friends, or an empty list if they have none
         */
        val friends: List<Friend1?>?
    ) : HeroCharacter {
        override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
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

    data class Hero(
        val __typename: String,
        /**
         * The name of the character
         */
        val name: String,
        val inlineFragment: HeroCharacter?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeString(RESPONSE_FIELDS[1], name)
            it.writeObject(RESPONSE_FIELDS[2], inlineFragment?.marshaller())
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forString("name", "name", null, false, null),
                    ResponseField.forInlineFragment("__typename", "__typename", listOf("Human",
                            "Droid"))
                    )

            operator fun invoke(reader: ResponseReader): Hero {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                val inlineFragment = reader.readConditional(RESPONSE_FIELDS[2]) { conditionalType,
                        reader ->
                    when(conditionalType) {
                        in listOf("Human") -> AsHuman(reader)
                        in listOf("Droid") -> AsDroid(reader)
                        else -> null
                    }
                }

                return Hero(
                    __typename = __typename,
                    name = name,
                    inlineFragment = inlineFragment
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
        const val OPERATION_ID: String =
                "889b355e84859a8d921df39c9c91993790199dc7c93868ed8a6739ac577579d8"

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
