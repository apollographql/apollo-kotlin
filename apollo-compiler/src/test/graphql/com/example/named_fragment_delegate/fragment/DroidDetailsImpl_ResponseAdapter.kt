// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.named_fragment_delegate.fragment

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.ResponseReader
import com.apollographql.apollo.api.internal.ResponseWriter
import kotlin.Array
import kotlin.String
import kotlin.collections.List

object DroidDetailsImpl_ResponseAdapter : ResponseAdapter<DroidDetailsImpl> {
  private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
    ResponseField.forString("__typename", "__typename", null, false, null),
    ResponseField.forString("name", "name", null, false, null),
    ResponseField.forString("primaryFunction", "primaryFunction", null, true, null),
    ResponseField.forList("friends", "friends", null, true, null)
  )

  override fun fromResponse(reader: ResponseReader, __typename: String?): DroidDetailsImpl {
    return reader.run {
      var __typename: String? = __typename
      var name: String? = null
      var primaryFunction: String? = null
      var friends: List<DroidDetailsImpl.Friend?>? = null
      while(true) {
        when (selectField(RESPONSE_FIELDS)) {
          0 -> __typename = readString(RESPONSE_FIELDS[0])
          1 -> name = readString(RESPONSE_FIELDS[1])
          2 -> primaryFunction = readString(RESPONSE_FIELDS[2])
          3 -> friends = readList<DroidDetailsImpl.Friend>(RESPONSE_FIELDS[3]) { reader ->
            reader.readObject<DroidDetailsImpl.Friend> { reader ->
              Friend.fromResponse(reader)
            }
          }
          else -> break
        }
      }
      DroidDetailsImpl(
        __typename = __typename!!,
        name = name!!,
        primaryFunction = primaryFunction,
        friends = friends
      )
    }
  }

  override fun toResponse(writer: ResponseWriter, value: DroidDetailsImpl) {
    writer.writeString(RESPONSE_FIELDS[0], value.__typename)
    writer.writeString(RESPONSE_FIELDS[1], value.name)
    writer.writeString(RESPONSE_FIELDS[2], value.primaryFunction)
    writer.writeList(RESPONSE_FIELDS[3], value.friends) { values, listItemWriter ->
      values?.forEach { value ->
        if(value == null) {
          listItemWriter.writeObject(null)
        } else {
          listItemWriter.writeObject { writer ->
            Friend.toResponse(writer, value)
          }
        }
      }
    }
  }

  object Friend : ResponseAdapter<DroidDetailsImpl.Friend> {
    private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField.forString("name", "name", null, false, null)
    )

    override fun fromResponse(reader: ResponseReader, __typename: String?):
        DroidDetailsImpl.Friend {
      return reader.run {
        var name: String? = null
        while(true) {
          when (selectField(RESPONSE_FIELDS)) {
            0 -> name = readString(RESPONSE_FIELDS[0])
            else -> break
          }
        }
        DroidDetailsImpl.Friend(
          name = name!!
        )
      }
    }

    override fun toResponse(writer: ResponseWriter, value: DroidDetailsImpl.Friend) {
      writer.writeString(RESPONSE_FIELDS[0], value.name)
    }
  }
}
