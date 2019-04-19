package com.example.simple_fragment

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseFieldMapper
import com.apollographql.apollo.api.ResponseFieldMarshaller
import com.apollographql.apollo.api.ResponseReader
import com.example.simple_fragment.fragment.HeroDetails
import javax.annotation.Generated
import kotlin.Array
import kotlin.String
import kotlin.Suppress

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

    data class Hero(val __typename: String, val fragments: Fragments) {
        fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeString(RESPONSE_FIELDS[0], __typename)
            fragments.marshaller().marshal(it)
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forString("__typename", "__typename", null, false, null),
                    ResponseField.forString("__typename", "__typename", null, false, null)
                    )

            operator fun invoke(reader: ResponseReader): Hero {
                val __typename = reader.readString(RESPONSE_FIELDS[0])
                val fragments = reader.readConditional(RESPONSE_FIELDS[1]) { conditionalType,
                        reader ->
                    val heroDetails = if (HeroDetails.POSSIBLE_TYPES.contains(conditionalType))
                            HeroDetails(reader) else null
                    Fragments(
                        heroDetails = heroDetails!!
                    )
                }

                return Hero(
                    __typename = __typename,
                    fragments = fragments
                )
            }
        }

        data class Fragments(val heroDetails: HeroDetails) {
            fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
                heroDetails.marshaller().marshal(it)
            }
        }
    }

    data class Data(val hero: Hero?) : Operation.Data {
        override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
            it.writeObject(RESPONSE_FIELDS[0], hero?.marshaller())
        }

        companion object {
            private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                    ResponseField.forObject("hero", "hero", null, true, null)
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
                "fb34e1f33db46408de360dadc7679f93aa0bd4259eacdff7a4c9c8032c89d60b"

        val QUERY_DOCUMENT: String = """
                |query TestQuery {
                |  hero {
                |    __typename
                |    ...HeroDetails
                |  }
                |}
                |fragment HeroDetails on Character {
                |  __typename
                |  name
                |}
                """.trimMargin()

        val OPERATION_NAME: OperationName = OperationName { "TestQuery" }
    }
}
