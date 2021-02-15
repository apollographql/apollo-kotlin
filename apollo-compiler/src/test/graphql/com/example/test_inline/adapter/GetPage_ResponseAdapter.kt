// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.test_inline.adapter

import com.apollographql.apollo.api.ResponseAdapterCache
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ListResponseAdapter
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.stringResponseAdapter
import com.example.test_inline.GetPage
import kotlin.Array
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
class GetPage_ResponseAdapter(
  customScalarAdapters: ResponseAdapterCache
) : ResponseAdapter<GetPage.Data> {
  val collectionAdapter: ResponseAdapter<GetPage.Data.Collection> = Collection(customScalarAdapters)

  override fun fromResponse(reader: JsonReader): GetPage.Data {
    var collection: GetPage.Data.Collection? = null
    reader.beginObject()
    while(true) {
      when (reader.selectName(RESPONSE_NAMES)) {
        0 -> collection = collectionAdapter.fromResponse(reader)
        else -> break
      }
    }
    reader.endObject()
    return GetPage.Data(
      collection = collection!!
    )
  }

  override fun toResponse(writer: JsonWriter, value: GetPage.Data) {
    writer.beginObject()
    writer.name("collection")
    collectionAdapter.toResponse(writer, value.collection)
    writer.endObject()
  }

  companion object {
    val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField(
        type = ResponseField.Type.NotNull(ResponseField.Type.Named.Object("Collection")),
        fieldName = "collection",
        fieldSets = listOf(
          ResponseField.FieldSet("ParticularCollection",
              Collection.ParticularCollectionCollection.RESPONSE_FIELDS),
          ResponseField.FieldSet(null, Collection.OtherCollection.RESPONSE_FIELDS),
        ),
      )
    )

    val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
  }

  class Collection(
    customScalarAdapters: ResponseAdapterCache
  ) : ResponseAdapter<GetPage.Data.Collection> {
    val ParticularCollectionCollectionAdapter: ParticularCollectionCollection =
        com.example.test_inline.adapter.GetPage_ResponseAdapter.Collection.ParticularCollectionCollection(customScalarAdapters)

    val OtherCollectionAdapter: OtherCollection =
        com.example.test_inline.adapter.GetPage_ResponseAdapter.Collection.OtherCollection(customScalarAdapters)

    override fun fromResponse(reader: JsonReader): GetPage.Data.Collection {
      reader.beginObject()
      check(reader.nextName() == "__typename")
      val typename = reader.nextString()

      return when(typename) {
        "ParticularCollection" -> ParticularCollectionCollectionAdapter.fromResponse(reader, typename)
        else -> OtherCollectionAdapter.fromResponse(reader, typename)
      }
      .also { reader.endObject() }
    }

    override fun toResponse(writer: JsonWriter, value: GetPage.Data.Collection) {
      when(value) {
        is GetPage.Data.Collection.ParticularCollectionCollection -> ParticularCollectionCollectionAdapter.toResponse(writer, value)
        is GetPage.Data.Collection.OtherCollection -> OtherCollectionAdapter.toResponse(writer, value)
      }
    }

    class ParticularCollectionCollection(
      customScalarAdapters: ResponseAdapterCache
    ) {
      val stringAdapter: ResponseAdapter<String> = stringResponseAdapter

      val listOfItemsAdapter:
          ResponseAdapter<List<GetPage.Data.Collection.ParticularCollectionCollection.Items>> =
          ListResponseAdapter(Items(customScalarAdapters))

      fun fromResponse(reader: JsonReader, __typename: String?):
          GetPage.Data.Collection.ParticularCollectionCollection {
        var __typename: String? = __typename
        var items: List<GetPage.Data.Collection.ParticularCollectionCollection.Items>? = null
        while(true) {
          when (reader.selectName(RESPONSE_NAMES)) {
            0 -> __typename = stringAdapter.fromResponse(reader)
            1 -> items = listOfItemsAdapter.fromResponse(reader)
            else -> break
          }
        }
        return GetPage.Data.Collection.ParticularCollectionCollection(
          __typename = __typename!!,
          items = items!!
        )
      }

      fun toResponse(writer: JsonWriter,
          value: GetPage.Data.Collection.ParticularCollectionCollection) {
        writer.beginObject()
        writer.name("__typename")
        stringAdapter.toResponse(writer, value.__typename)
        writer.name("items")
        listOfItemsAdapter.toResponse(writer, value.items)
        writer.endObject()
      }

      companion object {
        val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.Typename,
          ResponseField(
            type =
                ResponseField.Type.NotNull(ResponseField.Type.List(ResponseField.Type.NotNull(ResponseField.Type.Named.Object("Item")))),
            fieldName = "items",
            fieldSets = listOf(
              ResponseField.FieldSet("ParticularItem", Items.ParticularItemItems.RESPONSE_FIELDS),
              ResponseField.FieldSet(null, Items.OtherItems.RESPONSE_FIELDS),
            ),
          )
        )

        val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
      }

      class Items(
        customScalarAdapters: ResponseAdapterCache
      ) : ResponseAdapter<GetPage.Data.Collection.ParticularCollectionCollection.Items> {
        val ParticularItemItemsAdapter: ParticularItemItems =
            com.example.test_inline.adapter.GetPage_ResponseAdapter.Collection.ParticularCollectionCollection.Items.ParticularItemItems(customScalarAdapters)

        val OtherItemsAdapter: OtherItems =
            com.example.test_inline.adapter.GetPage_ResponseAdapter.Collection.ParticularCollectionCollection.Items.OtherItems(customScalarAdapters)

        override fun fromResponse(reader: JsonReader):
            GetPage.Data.Collection.ParticularCollectionCollection.Items {
          reader.beginObject()
          check(reader.nextName() == "__typename")
          val typename = reader.nextString()

          return when(typename) {
            "ParticularItem" -> ParticularItemItemsAdapter.fromResponse(reader, typename)
            else -> OtherItemsAdapter.fromResponse(reader, typename)
          }
          .also { reader.endObject() }
        }

        override fun toResponse(writer: JsonWriter,
            value: GetPage.Data.Collection.ParticularCollectionCollection.Items) {
          when(value) {
            is GetPage.Data.Collection.ParticularCollectionCollection.Items.ParticularItemItems -> ParticularItemItemsAdapter.toResponse(writer, value)
            is GetPage.Data.Collection.ParticularCollectionCollection.Items.OtherItems -> OtherItemsAdapter.toResponse(writer, value)
          }
        }

        class ParticularItemItems(
          customScalarAdapters: ResponseAdapterCache
        ) {
          val stringAdapter: ResponseAdapter<String> = stringResponseAdapter

          fun fromResponse(reader: JsonReader, __typename: String?):
              GetPage.Data.Collection.ParticularCollectionCollection.Items.ParticularItemItems {
            var title: String? = null
            var __typename: String? = __typename
            var image: String? = null
            while(true) {
              when (reader.selectName(RESPONSE_NAMES)) {
                0 -> title = stringAdapter.fromResponse(reader)
                1 -> __typename = stringAdapter.fromResponse(reader)
                2 -> image = stringAdapter.fromResponse(reader)
                else -> break
              }
            }
            return GetPage.Data.Collection.ParticularCollectionCollection.Items.ParticularItemItems(
              title = title!!,
              __typename = __typename!!,
              image = image!!
            )
          }

          fun toResponse(writer: JsonWriter,
              value: GetPage.Data.Collection.ParticularCollectionCollection.Items.ParticularItemItems) {
            writer.beginObject()
            writer.name("title")
            stringAdapter.toResponse(writer, value.title)
            writer.name("__typename")
            stringAdapter.toResponse(writer, value.__typename)
            writer.name("image")
            stringAdapter.toResponse(writer, value.image)
            writer.endObject()
          }

          companion object {
            val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
              ResponseField(
                type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
                fieldName = "title",
              ),
              ResponseField.Typename,
              ResponseField(
                type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
                fieldName = "image",
              )
            )

            val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
          }
        }

        class OtherItems(
          customScalarAdapters: ResponseAdapterCache
        ) {
          val stringAdapter: ResponseAdapter<String> = stringResponseAdapter

          fun fromResponse(reader: JsonReader, __typename: String?):
              GetPage.Data.Collection.ParticularCollectionCollection.Items.OtherItems {
            var title: String? = null
            var __typename: String? = __typename
            while(true) {
              when (reader.selectName(RESPONSE_NAMES)) {
                0 -> title = stringAdapter.fromResponse(reader)
                1 -> __typename = stringAdapter.fromResponse(reader)
                else -> break
              }
            }
            return GetPage.Data.Collection.ParticularCollectionCollection.Items.OtherItems(
              title = title!!,
              __typename = __typename!!
            )
          }

          fun toResponse(writer: JsonWriter,
              value: GetPage.Data.Collection.ParticularCollectionCollection.Items.OtherItems) {
            writer.beginObject()
            writer.name("title")
            stringAdapter.toResponse(writer, value.title)
            writer.name("__typename")
            stringAdapter.toResponse(writer, value.__typename)
            writer.endObject()
          }

          companion object {
            val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
              ResponseField(
                type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
                fieldName = "title",
              ),
              ResponseField.Typename
            )

            val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
          }
        }
      }
    }

    class OtherCollection(
      customScalarAdapters: ResponseAdapterCache
    ) {
      val stringAdapter: ResponseAdapter<String> = stringResponseAdapter

      val listOfItemsAdapter: ResponseAdapter<List<GetPage.Data.Collection.OtherCollection.Items>> =
          ListResponseAdapter(Items(customScalarAdapters))

      fun fromResponse(reader: JsonReader, __typename: String?):
          GetPage.Data.Collection.OtherCollection {
        var __typename: String? = __typename
        var items: List<GetPage.Data.Collection.OtherCollection.Items>? = null
        while(true) {
          when (reader.selectName(RESPONSE_NAMES)) {
            0 -> __typename = stringAdapter.fromResponse(reader)
            1 -> items = listOfItemsAdapter.fromResponse(reader)
            else -> break
          }
        }
        return GetPage.Data.Collection.OtherCollection(
          __typename = __typename!!,
          items = items!!
        )
      }

      fun toResponse(writer: JsonWriter, value: GetPage.Data.Collection.OtherCollection) {
        writer.beginObject()
        writer.name("__typename")
        stringAdapter.toResponse(writer, value.__typename)
        writer.name("items")
        listOfItemsAdapter.toResponse(writer, value.items)
        writer.endObject()
      }

      companion object {
        val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField.Typename,
          ResponseField(
            type =
                ResponseField.Type.NotNull(ResponseField.Type.List(ResponseField.Type.NotNull(ResponseField.Type.Named.Object("Item")))),
            fieldName = "items",
            fieldSets = listOf(
              ResponseField.FieldSet(null, Items.RESPONSE_FIELDS)
            ),
          )
        )

        val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
      }

      class Items(
        customScalarAdapters: ResponseAdapterCache
      ) : ResponseAdapter<GetPage.Data.Collection.OtherCollection.Items> {
        val stringAdapter: ResponseAdapter<String> = stringResponseAdapter

        override fun fromResponse(reader: JsonReader):
            GetPage.Data.Collection.OtherCollection.Items {
          var title: String? = null
          reader.beginObject()
          while(true) {
            when (reader.selectName(RESPONSE_NAMES)) {
              0 -> title = stringAdapter.fromResponse(reader)
              else -> break
            }
          }
          reader.endObject()
          return GetPage.Data.Collection.OtherCollection.Items(
            title = title!!
          )
        }

        override fun toResponse(writer: JsonWriter,
            value: GetPage.Data.Collection.OtherCollection.Items) {
          writer.beginObject()
          writer.name("title")
          stringAdapter.toResponse(writer, value.title)
          writer.endObject()
        }

        companion object {
          val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
            ResponseField(
              type = ResponseField.Type.NotNull(ResponseField.Type.Named.Other("String")),
              fieldName = "title",
            )
          )

          val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
        }
      }
    }
  }
}
