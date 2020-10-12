// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.fragment_used_twice.fragment

import com.apollographql.apollo.api.GraphqlFragment
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo.api.internal.ResponseReader
import com.example.fragment_used_twice.type.CustomType
import kotlin.Any
import kotlin.Array
import kotlin.String
import kotlin.Suppress

/**
 * A character from the Star Wars universe
 */
@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
interface HeroDetails : GraphqlFragment {
  val __typename: String

  /**
   * The name of the character
   */
  val name: String

  /**
   * A character from the Star Wars universe
   */
  data class DefaultImpl(
    override val __typename: String = "Character",
    /**
     * The name of the character
     */
    override val name: String,
    /**
     * The date character was born.
     */
    override val birthDate: Any
  ) : HeroDetails, CharacterDetails {
    override fun marshaller(): ResponseFieldMarshaller {
      return ResponseFieldMarshaller.invoke { writer ->
        writer.writeString(RESPONSE_FIELDS[0], this@DefaultImpl.__typename)
        writer.writeString(RESPONSE_FIELDS[1], this@DefaultImpl.name)
        writer.writeCustom(RESPONSE_FIELDS[2] as ResponseField.CustomTypeField,
            this@DefaultImpl.birthDate)
      }
    }

    companion object {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("__typename", "__typename", null, false, null),
        ResponseField.forString("name", "name", null, false, null),
        ResponseField.forCustomType("birthDate", "birthDate", null, false, CustomType.DATE, null)
      )

      operator fun invoke(reader: ResponseReader, __typename: String? = null): DefaultImpl {
        return reader.run {
          var __typename: String? = __typename
          var name: String? = null
          var birthDate: Any? = null
          while(true) {
            when (selectField(RESPONSE_FIELDS)) {
              0 -> __typename = readString(RESPONSE_FIELDS[0])
              1 -> name = readString(RESPONSE_FIELDS[1])
              2 -> birthDate = readCustomType<Any>(RESPONSE_FIELDS[2] as
                  ResponseField.CustomTypeField)
              else -> break
            }
          }
          DefaultImpl(
            __typename = __typename!!,
            name = name!!,
            birthDate = birthDate!!
          )
        }
      }

      @Suppress("FunctionName")
      fun Mapper(): ResponseFieldMapper<DefaultImpl> = ResponseFieldMapper { invoke(it) }
    }
  }

  companion object {
    val FRAGMENT_DEFINITION: String = """
        |fragment HeroDetails on Character {
        |  __typename
        |  name
        |  ...CharacterDetails
        |}
        """.trimMargin()

    operator fun invoke(reader: ResponseReader, __typename: String? = null): HeroDetails =
        DefaultImpl(reader, __typename)
  }
}
