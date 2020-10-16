// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.union_fragment

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.ResponseReader
import com.example.union_fragment.fragment.Character_ResponseAdapter
import com.example.union_fragment.fragment.Starship_ResponseAdapter
import kotlin.Array
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
internal object TestQuery_ResponseAdapter : ResponseAdapter<TestQuery.Data> {
  private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
    ResponseField.forList("search", "search", mapOf<String, Any>(
      "text" to "test"), true, null)
  )

  override fun fromResponse(reader: ResponseReader, __typename: String?): TestQuery.Data {
    return reader.run {
      var search: List<TestQuery.Search?>? = null
      while(true) {
        when (selectField(RESPONSE_FIELDS)) {
          0 -> search = readList<TestQuery.Search>(RESPONSE_FIELDS[0]) { reader ->
            reader.readObject<TestQuery.Search> { reader ->
              TestQuery_ResponseAdapter.Search_ResponseAdapter.fromResponse(reader)
            }
          }
          else -> break
        }
      }
      TestQuery.Data(
        search = search
      )
    }
  }

  object CharacterImpl_ResponseAdapter : ResponseAdapter<TestQuery.CharacterImpl> {
    override fun fromResponse(reader: ResponseReader, __typename: String?):
        TestQuery.CharacterImpl {
      return TestQuery.CharacterImpl(Character_ResponseAdapter.fromResponse(reader, __typename))
    }
  }

  object StarshipImpl_ResponseAdapter : ResponseAdapter<TestQuery.StarshipImpl> {
    override fun fromResponse(reader: ResponseReader, __typename: String?): TestQuery.StarshipImpl {
      return TestQuery.StarshipImpl(Starship_ResponseAdapter.fromResponse(reader, __typename))
    }
  }

  object OtherSearch_ResponseAdapter : ResponseAdapter<TestQuery.OtherSearch> {
    private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField.forString("__typename", "__typename", null, false, null)
    )

    override fun fromResponse(reader: ResponseReader, __typename: String?): TestQuery.OtherSearch {
      return reader.run {
        var __typename: String? = __typename
        while(true) {
          when (selectField(RESPONSE_FIELDS)) {
            0 -> __typename = readString(RESPONSE_FIELDS[0])
            else -> break
          }
        }
        TestQuery.OtherSearch(
          __typename = __typename!!
        )
      }
    }
  }

  object Search_ResponseAdapter : ResponseAdapter<TestQuery.Search> {
    private val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField.forString("__typename", "__typename", null, false, null)
    )

    override fun fromResponse(reader: ResponseReader, __typename: String?): TestQuery.Search {
      val typename = __typename ?: reader.readString(RESPONSE_FIELDS[0])
      return when(typename) {
        "Droid" -> TestQuery_ResponseAdapter.CharacterImpl_ResponseAdapter.fromResponse(reader, typename)
        "Human" -> TestQuery_ResponseAdapter.CharacterImpl_ResponseAdapter.fromResponse(reader, typename)
        "Starship" -> TestQuery_ResponseAdapter.StarshipImpl_ResponseAdapter.fromResponse(reader, typename)
        else -> TestQuery_ResponseAdapter.OtherSearch_ResponseAdapter.fromResponse(reader, typename)
      }
    }
  }
}
