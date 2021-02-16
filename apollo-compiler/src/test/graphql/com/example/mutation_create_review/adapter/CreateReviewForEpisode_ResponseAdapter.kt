// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.mutation_create_review.adapter

import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.internal.ListResponseAdapter
import com.apollographql.apollo3.api.internal.NullableResponseAdapter
import com.apollographql.apollo3.api.internal.ResponseAdapter
import com.apollographql.apollo3.api.internal.intResponseAdapter
import com.apollographql.apollo3.api.internal.json.JsonReader
import com.apollographql.apollo3.api.internal.json.JsonWriter
import com.apollographql.apollo3.api.internal.stringResponseAdapter
import com.example.mutation_create_review.CreateReviewForEpisode
import com.example.mutation_create_review.type.CustomScalars
import com.example.mutation_create_review.type.Episode
import com.example.mutation_create_review.type.Episode_ResponseAdapter
import java.util.Date
import kotlin.Array
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
internal class CreateReviewForEpisode_ResponseAdapter(
  customScalarAdapters: ResponseAdapterCache
) : ResponseAdapter<CreateReviewForEpisode.Data> {
  val nullableCreateReviewAdapter: ResponseAdapter<CreateReviewForEpisode.Data.CreateReview?> =
      NullableResponseAdapter(CreateReview(customScalarAdapters))

  override fun fromResponse(reader: JsonReader): CreateReviewForEpisode.Data {
    var createReview: CreateReviewForEpisode.Data.CreateReview? = null
    reader.beginObject()
    while(true) {
      when (reader.selectName(RESPONSE_NAMES)) {
        0 -> createReview = nullableCreateReviewAdapter.fromResponse(reader)
        else -> break
      }
    }
    reader.endObject()
    return CreateReviewForEpisode.Data(
      createReview = createReview
    )
  }

  override fun toResponse(writer: JsonWriter, value: CreateReviewForEpisode.Data) {
    writer.beginObject()
    writer.name("createReview")
    nullableCreateReviewAdapter.toResponse(writer, value.createReview)
    writer.endObject()
  }

  companion object {
    val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField(
        type = ResponseField.Type.Named.Object("Review"),
        fieldName = "createReview",
        arguments = mapOf<String, Any?>(
          "episode" to mapOf<String, Any?>(
            "kind" to "Variable",
            "variableName" to "ep"),
          "review" to mapOf<String, Any?>(
            "kind" to "Variable",
            "variableName" to "review")),
        fieldSets = listOf(
          ResponseField.FieldSet(null, CreateReview.RESPONSE_FIELDS)
        ),
      )
    )

    val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
  }

  class CreateReview(
    customScalarAdapters: ResponseAdapterCache
  ) : ResponseAdapter<CreateReviewForEpisode.Data.CreateReview> {
    val intAdapter: ResponseAdapter<Int> = intResponseAdapter

    val nullableStringAdapter: ResponseAdapter<String?> =
        NullableResponseAdapter(stringResponseAdapter)

    val nullableListOfListOfStringAdapter: ResponseAdapter<List<List<String>>?> =
        NullableResponseAdapter(ListResponseAdapter(ListResponseAdapter(stringResponseAdapter)))

    val nullableListOfListOfEpisodeAdapter: ResponseAdapter<List<List<Episode>>?> =
        NullableResponseAdapter(ListResponseAdapter(ListResponseAdapter(Episode_ResponseAdapter)))

    val nullableListOfListOfDateAdapter: ResponseAdapter<List<List<Date>>?> =
        NullableResponseAdapter(ListResponseAdapter(ListResponseAdapter(customScalarAdapters.responseAdapterFor<Date>(CustomScalars.Date))))

    val nullableListOfListOfListOfListOfObjectAdapter:
        ResponseAdapter<List<List<CreateReviewForEpisode.Data.CreateReview.ListOfListOfObject>>?> =
        NullableResponseAdapter(ListResponseAdapter(ListResponseAdapter(ListOfListOfObject(customScalarAdapters))))

    override fun fromResponse(reader: JsonReader): CreateReviewForEpisode.Data.CreateReview {
      var stars: Int? = null
      var commentary: String? = null
      var listOfListOfString: List<List<String>>? = null
      var listOfListOfEnum: List<List<Episode>>? = null
      var listOfListOfCustom: List<List<Date>>? = null
      var listOfListOfObject: List<List<CreateReviewForEpisode.Data.CreateReview.ListOfListOfObject>>? = null
      reader.beginObject()
      while(true) {
        when (reader.selectName(RESPONSE_NAMES)) {
          0 -> stars = intAdapter.fromResponse(reader)
          1 -> commentary = nullableStringAdapter.fromResponse(reader)
          2 -> listOfListOfString = nullableListOfListOfStringAdapter.fromResponse(reader)
          3 -> listOfListOfEnum = nullableListOfListOfEpisodeAdapter.fromResponse(reader)
          4 -> listOfListOfCustom = nullableListOfListOfDateAdapter.fromResponse(reader)
          5 -> listOfListOfObject = nullableListOfListOfListOfListOfObjectAdapter.fromResponse(reader)
          else -> break
        }
      }
      reader.endObject()
      return CreateReviewForEpisode.Data.CreateReview(
        stars = stars!!,
        commentary = commentary,
        listOfListOfString = listOfListOfString,
        listOfListOfEnum = listOfListOfEnum,
        listOfListOfCustom = listOfListOfCustom,
        listOfListOfObject = listOfListOfObject
      )
    }

    override fun toResponse(writer: JsonWriter, value: CreateReviewForEpisode.Data.CreateReview) {
      writer.beginObject()
      writer.name("stars")
      intAdapter.toResponse(writer, value.stars)
      writer.name("commentary")
      nullableStringAdapter.toResponse(writer, value.commentary)
      writer.name("listOfListOfString")
      nullableListOfListOfStringAdapter.toResponse(writer, value.listOfListOfString)
      writer.name("listOfListOfEnum")
      nullableListOfListOfEpisodeAdapter.toResponse(writer, value.listOfListOfEnum)
      writer.name("listOfListOfCustom")
      nullableListOfListOfDateAdapter.toResponse(writer, value.listOfListOfCustom)
      writer.name("listOfListOfObject")
      nullableListOfListOfListOfListOfObjectAdapter.toResponse(writer, value.listOfListOfObject)
      writer.endObject()
    }

    companion object {
      val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField(
          type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("Int")),
          fieldName = "stars",
        ),
        ResponseField(
          type = ResponseField.Type.Named.Other("String"),
          fieldName = "commentary",
        ),
        ResponseField(
          type =
              ResponseField.Type.List(ResponseField.Type.NotNull(ResponseField.Type.List(ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String"))))),
          fieldName = "listOfListOfString",
        ),
        ResponseField(
          type =
              ResponseField.Type.List(ResponseField.Type.NotNull(ResponseField.Type.List(ResponseField.Type.NotNull(ResponseField.Type.Named.Other("Episode"))))),
          fieldName = "listOfListOfEnum",
        ),
        ResponseField(
          type =
              ResponseField.Type.List(ResponseField.Type.NotNull(ResponseField.Type.List(ResponseField.Type.NotNull(ResponseField.Type.Named.Other("Date"))))),
          fieldName = "listOfListOfCustom",
        ),
        ResponseField(
          type =
              ResponseField.Type.List(ResponseField.Type.NotNull(ResponseField.Type.List(ResponseField.Type.NotNull(ResponseField.Type.Named.Object("Character"))))),
          fieldName = "listOfListOfObject",
          fieldSets = listOf(
            ResponseField.FieldSet(null, ListOfListOfObject.RESPONSE_FIELDS)
          ),
        )
      )

      val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
    }

    class ListOfListOfObject(
      customScalarAdapters: ResponseAdapterCache
    ) : ResponseAdapter<CreateReviewForEpisode.Data.CreateReview.ListOfListOfObject> {
      val stringAdapter: ResponseAdapter<String> = stringResponseAdapter

      override fun fromResponse(reader: JsonReader):
          CreateReviewForEpisode.Data.CreateReview.ListOfListOfObject {
        var name: String? = null
        reader.beginObject()
        while(true) {
          when (reader.selectName(RESPONSE_NAMES)) {
            0 -> name = stringAdapter.fromResponse(reader)
            else -> break
          }
        }
        reader.endObject()
        return CreateReviewForEpisode.Data.CreateReview.ListOfListOfObject(
          name = name!!
        )
      }

      override fun toResponse(writer: JsonWriter,
          value: CreateReviewForEpisode.Data.CreateReview.ListOfListOfObject) {
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
}
