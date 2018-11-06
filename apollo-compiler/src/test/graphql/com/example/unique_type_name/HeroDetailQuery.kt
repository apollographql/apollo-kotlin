package com.example.unique_type_name

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseFieldMapper
import com.apollographql.apollo.api.ResponseFieldMarshaller
import com.apollographql.apollo.api.ResponseReader
import com.example.unique_type_name.fragment.HeroDetails
import com.example.unique_type_name.type.Episode
import javax.annotation.Generated
import kotlin.Array
import kotlin.Double
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Generated("Apollo GraphQL")
class HeroDetailQuery : Query<HeroDetailQuery.Data, HeroDetailQuery.Data, Operation.Variables> {
    override fun operationId(): String = OPERATION_ID
    override fun queryDocument(): String = QUERY_DOCUMENT
    override fun wrapData(data: HeroDetailQuery.Data): HeroDetailQuery.Data = data
    override fun variables(): Operation.Variables = Operation.EMPTY_VARIABLES
    override fun name(): OperationName = OPERATION_NAME
    override fun responseFieldMapper(): ResponseFieldMapper<HeroDetailQuery.Data> = ResponseFieldMapper {
        HeroDetailQuery.Data(it)
    }

    data class Friend1(val __typename: String, val fragments: Fragments) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            fragments.marshaller().marshal(it)
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forString("__typename", "__typename", null, false, null)
                    )

            operator fun invoke(reader: ResponseReader): Friend1 {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val fragments = reader.readConditional(RESPONSE_FIELDS[1]) { conditionalType, reader ->
                    val heroDetails = if (HeroDetails.POSSIBLE_TYPES.contains(conditionalType)) HeroDetails(reader) else null
                    Fragments(
                        heroDetails = heroDetails
                    )
                }

                return Friend1(
                    __typename = __typename,
                    fragments = fragments
                )
            }
        }

        data class Fragments(val heroDetails: HeroDetails?) {
            fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
                heroDetails?.marshaller()?.marshal(it)
            }
        }
    }

    /**
     * @param name The name of the character
     * @param appearsIn The movies this character appears in
     * @param friends The friends of the character, or an empty list if they have none
     */
    data class Friend(
        val __typename: String,
        val name: String,
        val appearsIn: List<Episode>,
        val friends: List<Friend1>?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeString(RESPONSE_FIELDS[1], name)
            it.writeList(RESPONSE_FIELDS[2], appearsIn) { value, listItemWriter ->
                @Suppress("NAME_SHADOWING")
                value?.forEach { value ->
                    listItemWriter.writeString(value)
                }
            }
            it.writeList(RESPONSE_FIELDS[3], friends) { value, listItemWriter ->
                @Suppress("NAME_SHADOWING")
                value?.forEach { value ->
                    listItemWriter.writeObject(value?.marshaller())
                }
            }
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forString("name", "name", null, false, null),
                    ResponseField.forList("appearsIn", "appearsIn", null, false, null),
                    ResponseField.forList("friends", "friends", null, true, null)
                    )

            operator fun invoke(reader: ResponseReader): Friend {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                val appearsIn = reader.readList<Episode>(RESPONSE_FIELDS[2]) {
                    Episode.safeValueOf(it.readString())
                }
                val friends = reader.readList<Friend1>(RESPONSE_FIELDS[3]) {
                    it.readObject<Friend1> {
                        Friend1(it)
                    }

                }
                return Friend(
                    __typename = __typename,
                    name = name,
                    appearsIn = appearsIn,
                    friends = friends
                )
            }
        }
    }

    /**
     * @param name What this human calls themselves
     * @param friends This human's friends, or an empty list if they have none
     * @param height Height in the preferred unit, default is meters
     */
    data class AsHuman(
        val __typename: String,
        val name: String,
        val friends: List<Friend>?,
        val height: Double?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeString(RESPONSE_FIELDS[1], name)
            it.writeList(RESPONSE_FIELDS[2], friends) { value, listItemWriter ->
                @Suppress("NAME_SHADOWING")
                value?.forEach { value ->
                    listItemWriter.writeObject(value?.marshaller())
                }
            }
            it.writeDouble(RESPONSE_FIELDS[3], height)
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forString("name", "name", null, false, null),
                    ResponseField.forList("friends", "friends", null, true, null),
                    ResponseField.forDouble("height", "height", null, true, null)
                    )

            operator fun invoke(reader: ResponseReader): AsHuman {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                val friends = reader.readList<Friend>(RESPONSE_FIELDS[2]) {
                    it.readObject<Friend> {
                        Friend(it)
                    }

                }
                val height = reader.readDouble(RESPONSE_FIELDS[3])
                return AsHuman(
                    __typename = __typename,
                    name = name,
                    friends = friends,
                    height = height
                )
            }
        }
    }

    /**
     * @param name The name of the character
     */
    data class Friend12(
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

            operator fun invoke(reader: ResponseReader): Friend12 {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                return Friend12(
                    __typename = __typename,
                    name = name
                )
            }
        }
    }

    /**
     * @param name The name of the character
     * @param friends The friends of the character, or an empty list if they have none
     */
    data class HeroDetailQuery(
        val __typename: String,
        val name: String,
        val friends: List<Friend12>?,
        val asHuman: AsHuman?
    ) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            it.writeString(RESPONSE_FIELDS[1], name)
            it.writeList(RESPONSE_FIELDS[2], friends) { value, listItemWriter ->
                @Suppress("NAME_SHADOWING")
                value?.forEach { value ->
                    listItemWriter.writeObject(value?.marshaller())
                }
            }
            it.writeObject(RESPONSE_FIELDS[3], asHuman?.marshaller())
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forString("name", "name", null, false, null),
                    ResponseField.forList("friends", "friends", null, true, null),
                    ResponseField.forInlineFragment("__typename", "__typename", listOf("Human"))
                    )

            operator fun invoke(reader: ResponseReader): HeroDetailQuery {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val name = reader.readString(RESPONSE_FIELDS[1])
                val friends = reader.readList<Friend12>(RESPONSE_FIELDS[2]) {
                    it.readObject<Friend12> {
                        Friend12(it)
                    }

                }
                val asHuman = reader.readConditional(RESPONSE_FIELDS[3]) { conditionalType, reader ->
                    AsHuman(reader)
                }

                return HeroDetailQuery(
                    __typename = __typename,
                    name = name,
                    friends = friends,
                    asHuman = asHuman
                )
            }
        }
    }

    data class Data(val heroDetailQuery: HeroDetailQuery?) : Operation.Data {
        override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeObject(RESPONSE_FIELDS[0], heroDetailQuery?.marshaller())
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forObject("heroDetailQuery", "heroDetailQuery", null, true, null)
                    )

            operator fun invoke(reader: ResponseReader): Data {
                val heroDetailQuery = reader.readObject<HeroDetailQuery>(RESPONSE_FIELDS[0]) {
                    HeroDetailQuery(it)
                }

                return Data(
                    heroDetailQuery = heroDetailQuery
                )
            }
        }
    }

    companion object {
        val OPERATION_DEFINITION: String = """
                |query HeroDetailQuery {
                |  heroDetailQuery {
                |    __typename
                |    name
                |    friends {
                |      __typename
                |      name
                |    }
                |    ... on Human {
                |      height
                |      friends {
                |        __typename
                |        appearsIn
                |        friends {
                |          __typename
                |          ...HeroDetails
                |        }
                |      }
                |    }
                |  }
                |}
                """.trimMargin()

        const val OPERATION_ID: String =
                "92a6f8d924f3a9768021c6685ce6648cc180aa1066a76cf0d7ed7240e22c67bd"

        val QUERY_DOCUMENT: String = """
                |query HeroDetailQuery {
                |  heroDetailQuery {
                |    __typename
                |    name
                |    friends {
                |      __typename
                |      name
                |    }
                |    ... on Human {
                |      height
                |      friends {
                |        __typename
                |        appearsIn
                |        friends {
                |          __typename
                |          ...HeroDetails
                |        }
                |      }
                |    }
                |  }
                |}
                |fragment HeroDetails on Character {
                |  __typename
                |  name
                |  friendsConnection {
                |    __typename
                |    totalCount
                |    edges {
                |      __typename
                |      node {
                |        __typename
                |        name
                |      }
                |    }
                |  }
                |}
                """.trimMargin()

        val OPERATION_NAME: OperationName = OperationName { "HeroDetailQuery" }
    }
}
