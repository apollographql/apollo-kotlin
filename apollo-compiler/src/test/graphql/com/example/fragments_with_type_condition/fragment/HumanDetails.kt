package com.example.fragments_with_type_condition.fragment

import com.apollographql.apollo.api.GraphqlFragment
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseFieldMarshaller
import com.apollographql.apollo.api.ResponseReader
import javax.annotation.Generated
import kotlin.Array
import kotlin.Double
import kotlin.String
import kotlin.Suppress

/**
 * @param name What this human calls themselves
 * @param height Height in the preferred unit, default is meters
 */
@Generated("Apollo GraphQL")
@Suppress("NAME_SHADOWING", "LocalVariableName")
data class HumanDetails(
    val __typename: String,
    val name: String,
    val height: Double?
) : GraphqlFragment {
    override fun marshaller(): ResponseFieldMarshaller = ResponseFieldMarshaller {
        it.writeString(RESPONSE_FIELDS[0], __typename)
        it.writeString(RESPONSE_FIELDS[1], name)
        it.writeDouble(RESPONSE_FIELDS[2], height)
    }

    companion object {
        private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                ResponseField.forString("__typename", "__typename", null, false, null),
                ResponseField.forString("name", "name", null, false, null),
                ResponseField.forDouble("height", "height", null, true, null)
                )

        val FRAGMENT_DEFINITION: String = """
                |fragment HumanDetails on Human {
                |  __typename
                |  name
                |  height
                |}
                """.trimMargin()

        val POSSIBLE_TYPES: Array<String> = arrayOf("Human")

        operator fun invoke(reader: ResponseReader): HumanDetails {
            val __typename = reader.readString(RESPONSE_FIELDS[0])
            val name = reader.readString(RESPONSE_FIELDS[1])
            val height = reader.readDouble(RESPONSE_FIELDS[2])
            return HumanDetails(
                __typename = __typename,
                name = name,
                height = height
            )
        }
    }
}
