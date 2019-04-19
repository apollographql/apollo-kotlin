package com.example.union_inline_fragments

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseFieldMapper
import com.apollographql.apollo.api.ResponseFieldMarshaller
import com.apollographql.apollo.api.ResponseReader
import com.example.union_inline_fragments.type.CustomType
import com.example.union_inline_fragments.type.Episode
import javax.annotation.Generated
import kotlin.Array
import kotlin.Deprecated
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Generated("Apollo GraphQL")
@Suppress("NAME_SHADOWING", "LocalVariableName")
class TestQuery : Query<TestQuery.Data, TestQuery.Data, Operation.Variables> {
    override fun operationId(): String = OPERATION_ID
    override fun queryDocument(): String = QUERY_DOCUMENT
    override fun wrapData(data: Data): Data = data
    override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES
    override fun name(): OperationName = OPERATION_NAME
    override fun responseFieldMapper(): ResponseFieldMapper<Data> = ResponseFieldMapper {
        Data(it)
    }

    data class Friend(
        val __typename: String,
        /**
         * The movie this character first appears in
         */
        val firstAppearsIn: Episode
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeString(RESPONSE_FIELDS[1], firstAppearsIn.rawValue)
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forEnum("firstAppearsIn", "firstAppearsIn", null, false, null)
                    )

            operator fun invoke(reader: ResponseReader): Friend {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val firstAppearsIn = Episode.safeValueOf(reader.readString(RESPONSE_FIELDS[1]))
                return Friend(
                    __typename = __typename,
                    firstAppearsIn = firstAppearsIn
                )
            }
        }
    }

    data class AsHuman1(
        val __typename: String,
        /**
         * What this human calls themselves
         */
        val name: String,
        /**
         * The home planet of the human, or null if unknown
         */
        val homePlanet: String?,
        /**
         * This human's friends, or an empty list if they have none
         */
        val friends: List<Friend?>?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeString(RESPONSE_FIELDS[1], name)
            it.writeString(RESPONSE_FIELDS[2], homePlanet)
            it.writeList(RESPONSE_FIELDS[3], friends) { value, listItemWriter ->
                value?.forEach { value ->
                    listItemWriter.writeObject(value?.marshaller())
                }
            }
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forString("name", "name", null, false, null),
                    ResponseField.forString("homePlanet", "homePlanet", null, true, null),
                    ResponseField.forList("friends", "friends", null, true, null)
                    )

            operator fun invoke(reader: ResponseReader): AsHuman1 {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                val homePlanet = reader.readString(RESPONSE_FIELDS[2])
                val friends = reader.readList<Friend>(RESPONSE_FIELDS[3]) {
                    it.readObject<Friend> { reader ->
                        Friend(reader)
                    }

                }
                return AsHuman1(
                    __typename = __typename,
                    name = name,
                    homePlanet = homePlanet,
                    friends = friends
                )
            }
        }
    }

    data class Friend1(
        val __typename: String,
        /**
         * The ID of the character
         */
        val id: String,
        /**
         * Test deprecated field
         */
        @Deprecated(message = "For test purpose only") val deprecated: String
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeCustom(RESPONSE_FIELDS[1] as ResponseField.CustomTypeField, id)
            it.writeString(RESPONSE_FIELDS[2], deprecated)
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forCustomType("id", "id", null, false, CustomType.ID, null),
                    ResponseField.forString("deprecated", "deprecated", null, false, null)
                    )

            operator fun invoke(reader: ResponseReader): Friend1 {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val id = reader.readCustomType<String>(RESPONSE_FIELDS[1] as
                        ResponseField.CustomTypeField)
                val deprecated = reader.readString(RESPONSE_FIELDS[2])
                return Friend1(
                    __typename = __typename,
                    id = id,
                    deprecated = deprecated
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
         * This droid's primary function
         */
        val primaryFunction: String?,
        /**
         * This droid's friends, or an empty list if they have none
         */
        val friends: List<Friend1?>?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeString(RESPONSE_FIELDS[1], name)
            it.writeString(RESPONSE_FIELDS[2], primaryFunction)
            it.writeList(RESPONSE_FIELDS[3], friends) { value, listItemWriter ->
                value?.forEach { value ->
                    listItemWriter.writeObject(value?.marshaller())
                }
            }
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forString("name", "name", null, false, null),
                    ResponseField.forString("primaryFunction", "primaryFunction", null, true, null),
                    ResponseField.forList("friends", "friends", null, true, null)
                    )

            operator fun invoke(reader: ResponseReader): AsDroid {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                val primaryFunction = reader.readString(RESPONSE_FIELDS[2])
                val friends = reader.readList<Friend1>(RESPONSE_FIELDS[3]) {
                    it.readObject<Friend1> { reader ->
                        Friend1(reader)
                    }

                }
                return AsDroid(
                    __typename = __typename,
                    name = name,
                    primaryFunction = primaryFunction,
                    friends = friends
                )
            }
        }
    }

    data class Friend12(
        val __typename: String,
        /**
         * The name of the character
         */
        val name: String,
        val asHuman1: AsHuman1?,
        val asDroid: AsDroid?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeString(RESPONSE_FIELDS[1], name)
            it.writeObject(RESPONSE_FIELDS[2], asHuman1?.marshaller())
            it.writeObject(RESPONSE_FIELDS[3], asDroid?.marshaller())
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forString("name", "name", null, false, null),
                    ResponseField.forInlineFragment("__typename", "__typename", listOf("Human")),
                    ResponseField.forInlineFragment("__typename", "__typename", listOf("Droid"))
                    )

            operator fun invoke(reader: ResponseReader): Friend12 {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                val asHuman1 = reader.readConditional(RESPONSE_FIELDS[2]) { conditionalType,
                        reader ->
                    AsHuman1(reader)
                }

                val asDroid = reader.readConditional(RESPONSE_FIELDS[3]) { conditionalType,
                        reader ->
                    AsDroid(reader)
                }

                return Friend12(
                    __typename = __typename,
                    name = name,
                    asHuman1 = asHuman1,
                    asDroid = asDroid
                )
            }
        }
    }

    data class AsHuman(
        val __typename: String,
        /**
         * The ID of the character
         */
        val id: String,
        /**
         * The name of the character
         */
        val name: String,
        /**
         * The friends of the character, or an empty list if they have none
         */
        val friends: List<Friend12?>?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeCustom(RESPONSE_FIELDS[1] as ResponseField.CustomTypeField, id)
            it.writeString(RESPONSE_FIELDS[2], name)
            it.writeList(RESPONSE_FIELDS[3], friends) { value, listItemWriter ->
                value?.forEach { value ->
                    listItemWriter.writeObject(value?.marshaller())
                }
            }
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forCustomType("id", "id", null, false, CustomType.ID, null),
                    ResponseField.forString("name", "name", null, false, null),
                    ResponseField.forList("friends", "friends", null, true, null)
                    )

            operator fun invoke(reader: ResponseReader): AsHuman {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val id = reader.readCustomType<String>(RESPONSE_FIELDS[1] as
                        ResponseField.CustomTypeField)
                val name = reader.readString(RESPONSE_FIELDS[2])
                val friends = reader.readList<Friend12>(RESPONSE_FIELDS[3]) {
                    it.readObject<Friend12> { reader ->
                        Friend12(reader)
                    }

                }
                return AsHuman(
                    __typename = __typename,
                    id = id,
                    name = name,
                    friends = friends
                )
            }
        }
    }

    data class Friend123(
        val __typename: String,
        /**
         * The movie this character first appears in
         */
        val firstAppearsIn: Episode
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeString(RESPONSE_FIELDS[1], firstAppearsIn.rawValue)
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forEnum("firstAppearsIn", "firstAppearsIn", null, false, null)
                    )

            operator fun invoke(reader: ResponseReader): Friend123 {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val firstAppearsIn = Episode.safeValueOf(reader.readString(RESPONSE_FIELDS[1]))
                return Friend123(
                    __typename = __typename,
                    firstAppearsIn = firstAppearsIn
                )
            }
        }
    }

    data class AsHuman12(
        val __typename: String,
        /**
         * What this human calls themselves
         */
        val name: String,
        /**
         * The home planet of the human, or null if unknown
         */
        val homePlanet: String?,
        /**
         * This human's friends, or an empty list if they have none
         */
        val friends: List<Friend123?>?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeString(RESPONSE_FIELDS[1], name)
            it.writeString(RESPONSE_FIELDS[2], homePlanet)
            it.writeList(RESPONSE_FIELDS[3], friends) { value, listItemWriter ->
                value?.forEach { value ->
                    listItemWriter.writeObject(value?.marshaller())
                }
            }
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forString("name", "name", null, false, null),
                    ResponseField.forString("homePlanet", "homePlanet", null, true, null),
                    ResponseField.forList("friends", "friends", null, true, null)
                    )

            operator fun invoke(reader: ResponseReader): AsHuman12 {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                val homePlanet = reader.readString(RESPONSE_FIELDS[2])
                val friends = reader.readList<Friend123>(RESPONSE_FIELDS[3]) {
                    it.readObject<Friend123> { reader ->
                        Friend123(reader)
                    }

                }
                return AsHuman12(
                    __typename = __typename,
                    name = name,
                    homePlanet = homePlanet,
                    friends = friends
                )
            }
        }
    }

    data class Friend1234(
        val __typename: String,
        /**
         * The ID of the character
         */
        val id: String,
        /**
         * Test deprecated field
         */
        @Deprecated(message = "For test purpose only") val deprecated: String
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeCustom(RESPONSE_FIELDS[1] as ResponseField.CustomTypeField, id)
            it.writeString(RESPONSE_FIELDS[2], deprecated)
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forCustomType("id", "id", null, false, CustomType.ID, null),
                    ResponseField.forString("deprecated", "deprecated", null, false, null)
                    )

            operator fun invoke(reader: ResponseReader): Friend1234 {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val id = reader.readCustomType<String>(RESPONSE_FIELDS[1] as
                        ResponseField.CustomTypeField)
                val deprecated = reader.readString(RESPONSE_FIELDS[2])
                return Friend1234(
                    __typename = __typename,
                    id = id,
                    deprecated = deprecated
                )
            }
        }
    }

    data class AsDroid12(
        val __typename: String,
        /**
         * What others call this droid
         */
        val name: String,
        /**
         * This droid's primary function
         */
        val primaryFunction: String?,
        /**
         * This droid's friends, or an empty list if they have none
         */
        val friends: List<Friend1234?>?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeString(RESPONSE_FIELDS[1], name)
            it.writeString(RESPONSE_FIELDS[2], primaryFunction)
            it.writeList(RESPONSE_FIELDS[3], friends) { value, listItemWriter ->
                value?.forEach { value ->
                    listItemWriter.writeObject(value?.marshaller())
                }
            }
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forString("name", "name", null, false, null),
                    ResponseField.forString("primaryFunction", "primaryFunction", null, true, null),
                    ResponseField.forList("friends", "friends", null, true, null)
                    )

            operator fun invoke(reader: ResponseReader): AsDroid12 {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                val primaryFunction = reader.readString(RESPONSE_FIELDS[2])
                val friends = reader.readList<Friend1234>(RESPONSE_FIELDS[3]) {
                    it.readObject<Friend1234> { reader ->
                        Friend1234(reader)
                    }

                }
                return AsDroid12(
                    __typename = __typename,
                    name = name,
                    primaryFunction = primaryFunction,
                    friends = friends
                )
            }
        }
    }

    data class Friend12345(
        val __typename: String,
        /**
         * The name of the character
         */
        val name: String,
        val asHuman12: AsHuman12?,
        val asDroid12: AsDroid12?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeString(RESPONSE_FIELDS[1], name)
            it.writeObject(RESPONSE_FIELDS[2], asHuman12?.marshaller())
            it.writeObject(RESPONSE_FIELDS[3], asDroid12?.marshaller())
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forString("name", "name", null, false, null),
                    ResponseField.forInlineFragment("__typename", "__typename", listOf("Human")),
                    ResponseField.forInlineFragment("__typename", "__typename", listOf("Droid"))
                    )

            operator fun invoke(reader: ResponseReader): Friend12345 {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                val asHuman12 = reader.readConditional(RESPONSE_FIELDS[2]) { conditionalType,
                        reader ->
                    AsHuman12(reader)
                }

                val asDroid12 = reader.readConditional(RESPONSE_FIELDS[3]) { conditionalType,
                        reader ->
                    AsDroid12(reader)
                }

                return Friend12345(
                    __typename = __typename,
                    name = name,
                    asHuman12 = asHuman12,
                    asDroid12 = asDroid12
                )
            }
        }
    }

    data class AsDroid1(
        val __typename: String,
        /**
         * The ID of the character
         */
        val id: String,
        /**
         * The name of the character
         */
        val name: String,
        /**
         * The friends of the character, or an empty list if they have none
         */
        val friends: List<Friend12345?>?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeCustom(RESPONSE_FIELDS[1] as ResponseField.CustomTypeField, id)
            it.writeString(RESPONSE_FIELDS[2], name)
            it.writeList(RESPONSE_FIELDS[3], friends) { value, listItemWriter ->
                value?.forEach { value ->
                    listItemWriter.writeObject(value?.marshaller())
                }
            }
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forCustomType("id", "id", null, false, CustomType.ID, null),
                    ResponseField.forString("name", "name", null, false, null),
                    ResponseField.forList("friends", "friends", null, true, null)
                    )

            operator fun invoke(reader: ResponseReader): AsDroid1 {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val id = reader.readCustomType<String>(RESPONSE_FIELDS[1] as
                        ResponseField.CustomTypeField)
                val name = reader.readString(RESPONSE_FIELDS[2])
                val friends = reader.readList<Friend12345>(RESPONSE_FIELDS[3]) {
                    it.readObject<Friend12345> { reader ->
                        Friend12345(reader)
                    }

                }
                return AsDroid1(
                    __typename = __typename,
                    id = id,
                    name = name,
                    friends = friends
                )
            }
        }
    }

    data class AsStarship(
        val __typename: String,
        /**
         * The name of the starship
         */
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

            operator fun invoke(reader: ResponseReader): AsStarship {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                return AsStarship(
                    __typename = __typename,
                    name = name
                )
            }
        }
    }

    data class Search(
        val __typename: String,
        val asHuman: AsHuman?,
        val asDroid1: AsDroid1?,
        val asStarship: AsStarship?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeObject(RESPONSE_FIELDS[1], asHuman?.marshaller())
            it.writeObject(RESPONSE_FIELDS[2], asDroid1?.marshaller())
            it.writeObject(RESPONSE_FIELDS[3], asStarship?.marshaller())
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forInlineFragment("__typename", "__typename", listOf("Human")),
                    ResponseField.forInlineFragment("__typename", "__typename", listOf("Droid")),
                    ResponseField.forInlineFragment("__typename", "__typename", listOf("Starship"))
                    )

            operator fun invoke(reader: ResponseReader): Search {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val asHuman = reader.readConditional(RESPONSE_FIELDS[1]) { conditionalType,
                        reader ->
                    AsHuman(reader)
                }

                val asDroid1 = reader.readConditional(RESPONSE_FIELDS[2]) { conditionalType,
                        reader ->
                    AsDroid1(reader)
                }

                val asStarship = reader.readConditional(RESPONSE_FIELDS[3]) { conditionalType,
                        reader ->
                    AsStarship(reader)
                }

                return Search(
                    __typename = __typename,
                    asHuman = asHuman,
                    asDroid1 = asDroid1,
                    asStarship = asStarship
                )
            }
        }
    }

    data class Data(val search: List<Search?>?) : Operation.Data {
        override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeList(RESPONSE_FIELDS[0], search) { value, listItemWriter ->
                value?.forEach { value ->
                    listItemWriter.writeObject(value?.marshaller())
                }
            }
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forList("search", "search", mapOf<String, Any>(
                        "text" to "test"), true, null)
                    )

            operator fun invoke(reader: ResponseReader): Data {
                val search = reader.readList<Search>(RESPONSE_FIELDS[0]) {
                    it.readObject<Search> { reader ->
                        Search(reader)
                    }

                }
                return Data(
                    search = search
                )
            }
        }
    }

    companion object {
        const val OPERATION_ID: String =
                "8f2faf5f45edbcd0f65491d582a5a50f13cad39e3995dd5abe5ddb359d3cf066"

        val QUERY_DOCUMENT: String = """
                |query TestQuery {
                |  search(text: "test") {
                |    __typename
                |    ... on Character {
                |      id
                |      name
                |      friends {
                |        __typename
                |        ... on Character {
                |          name
                |        }
                |        ... on Human {
                |          homePlanet
                |          friends {
                |            __typename
                |            ... on Character {
                |              firstAppearsIn
                |            }
                |          }
                |        }
                |        ... on Droid {
                |          primaryFunction
                |          friends {
                |            __typename
                |            id
                |            deprecated
                |          }
                |        }
                |      }
                |    }
                |    ... on Starship {
                |      name
                |    }
                |  }
                |}
                """.trimMargin()

        val OPERATION_NAME: OperationName = OperationName { "TestQuery" }
    }
}
