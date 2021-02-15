// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.named_fragment_delegate.fragment.adapter

import com.apollographql.apollo.api.ResponseAdapterCache
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ListResponseAdapter
import com.apollographql.apollo.api.internal.NullableResponseAdapter
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.stringResponseAdapter
import com.example.named_fragment_delegate.fragment.DroidDetailsImpl
import kotlin.Array
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
class DroidDetailsImpl_ResponseAdapter(
  customScalarAdapters: ResponseAdapterCache
) : ResponseAdapter<DroidDetailsImpl.Data> {
  val stringAdapter: ResponseAdapter<String> = stringResponseAdapter

  val nullableStringAdapter: ResponseAdapter<String?> =
      NullableResponseAdapter(stringResponseAdapter)

  val nullableListOfNullableFriendsAdapter: ResponseAdapter<List<DroidDetailsImpl.Data.Friends?>?> =
      NullableResponseAdapter(ListResponseAdapter(NullableResponseAdapter(Friends(customScalarAdapters))))

  override fun fromResponse(reader: JsonReader): DroidDetailsImpl.Data {
    var __typename: String? = null
    var name: String? = null
    var primaryFunction: String? = null
    var friends: List<DroidDetailsImpl.Data.Friends?>? = null
    reader.beginObject()
    while(true) {
      when (reader.selectName(RESPONSE_NAMES)) {
        0 -> __typename = stringAdapter.fromResponse(reader)
        1 -> name = stringAdapter.fromResponse(reader)
        2 -> primaryFunction = nullableStringAdapter.fromResponse(reader)
        3 -> friends = nullableListOfNullableFriendsAdapter.fromResponse(reader)
        else -> break
      }
    }
    reader.endObject()
    return DroidDetailsImpl.Data(
      __typename = __typename!!,
      name = name!!,
      primaryFunction = primaryFunction,
      friends = friends
    )
  }

  override fun toResponse(writer: JsonWriter, value: DroidDetailsImpl.Data) {
    writer.beginObject()
    writer.name("__typename")
    stringAdapter.toResponse(writer, value.__typename)
    writer.name("name")
    stringAdapter.toResponse(writer, value.name)
    writer.name("primaryFunction")
    nullableStringAdapter.toResponse(writer, value.primaryFunction)
    writer.name("friends")
    nullableListOfNullableFriendsAdapter.toResponse(writer, value.friends)
    writer.endObject()
  }

  companion object {
    val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField.Typename,
      ResponseField(
        type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
        fieldName = "name",
      ),
      ResponseField(
        type = ResponseField.Type.Named.Other("String"),
        fieldName = "primaryFunction",
      ),
      ResponseField(
        type = ResponseField.Type.List(ResponseField.Type.Named.Object("Character")),
        fieldName = "friends",
        fieldSets = listOf(
          ResponseField.FieldSet(null, Friends.RESPONSE_FIELDS)
        ),
      )
    )

    val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
  }

  class Friends(
    customScalarAdapters: ResponseAdapterCache
  ) : ResponseAdapter<DroidDetailsImpl.Data.Friends> {
    val stringAdapter: ResponseAdapter<String> = stringResponseAdapter

    override fun fromResponse(reader: JsonReader): DroidDetailsImpl.Data.Friends {
      var name: String? = null
      reader.beginObject()
      while(true) {
        when (reader.selectName(RESPONSE_NAMES)) {
          0 -> name = stringAdapter.fromResponse(reader)
          else -> break
        }
      }
      reader.endObject()
      return DroidDetailsImpl.Data.Friends(
        name = name!!
      )
    }

    override fun toResponse(writer: JsonWriter, value: DroidDetailsImpl.Data.Friends) {
      writer.beginObject()
      writer.name("name")
      stringAdapter.toResponse(writer, value.name)
      writer.endObject()
    }

    companion object {
      val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField(
          type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
          fieldName = "name",
        )
      )

      val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
    }
  }
}
