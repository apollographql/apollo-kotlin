// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.deprecation

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.ResponseReader
import com.apollographql.apollo.api.internal.ResponseWriter
import kotlin.Array
import kotlin.Boolean
import kotlin.String
import kotlin.Suppress

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
object TestQuery_ResponseAdapter : ResponseAdapter<TestQuery.Data> {
  private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
    ResponseField.forObject("hero", "hero", mapOf<String, Any>(
      "episode" to mapOf<String, Any>(
        "kind" to "Variable",
        "variableName" to "episode")), true, null)
  )

  override fun fromResponse(reader: ResponseReader, __typename: String?): TestQuery.Data {
    return Data.fromResponse(reader, __typename)
  }

  override fun toResponse(writer: ResponseWriter, value: TestQuery.Data) {
    Data.toResponse(writer, value)
  }

  object Data : ResponseAdapter<TestQuery.Data> {
    private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField.forObject("hero", "hero", mapOf<String, Any>(
        "episode" to mapOf<String, Any>(
          "kind" to "Variable",
          "variableName" to "episode")), true, null)
    )

    override fun fromResponse(reader: ResponseReader, __typename: String?): TestQuery.Data {
      return reader.run {
        var hero: TestQuery.Data.Hero? = null
        while(true) {
          when (selectField(RESPONSE_FIELDS)) {
            0 -> hero = readObject<TestQuery.Data.Hero>(RESPONSE_FIELDS[0]) { reader ->
              Hero.fromResponse(reader)
            }
            else -> break
          }
        }
        TestQuery.Data(
          hero = hero
        )
      }
    }

    override fun toResponse(writer: ResponseWriter, value: TestQuery.Data) {
      if(value.hero == null) {
        writer.writeObject(RESPONSE_FIELDS[0], null)
      } else {
        writer.writeObject(RESPONSE_FIELDS[0]) { writer ->
          Hero.toResponse(writer, value.hero)
        }
      }
    }

    object Hero : ResponseAdapter<TestQuery.Data.Hero> {
      private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField.forString("name", "name", null, false, null),
        ResponseField.forString("deprecated", "deprecated", null, false, null),
        ResponseField.forBoolean("deprecatedBool", "deprecatedBool", null, false, null)
      )

      override fun fromResponse(reader: ResponseReader, __typename: String?): TestQuery.Data.Hero {
        return reader.run {
          var name: String? = null
          var deprecated: String? = null
          var deprecatedBool: Boolean? = null
          while(true) {
            when (selectField(RESPONSE_FIELDS)) {
              0 -> name = readString(RESPONSE_FIELDS[0])
              1 -> deprecated = readString(RESPONSE_FIELDS[1])
              2 -> deprecatedBool = readBoolean(RESPONSE_FIELDS[2])
              else -> break
            }
          }
          TestQuery.Data.Hero(
            name = name!!,
            deprecated = deprecated!!,
            deprecatedBool = deprecatedBool!!
          )
        }
      }

      override fun toResponse(writer: ResponseWriter, value: TestQuery.Data.Hero) {
        writer.writeString(RESPONSE_FIELDS[0], value.name)
        writer.writeString(RESPONSE_FIELDS[1], value.deprecated)
        writer.writeBoolean(RESPONSE_FIELDS[2], value.deprecatedBool)
      }
    }
  }
}
