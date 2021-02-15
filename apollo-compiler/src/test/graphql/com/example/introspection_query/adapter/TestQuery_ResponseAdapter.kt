// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.introspection_query.adapter

import com.apollographql.apollo.api.ResponseAdapterCache
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ListResponseAdapter
import com.apollographql.apollo.api.internal.NullableResponseAdapter
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.stringResponseAdapter
import com.example.introspection_query.TestQuery
import kotlin.Array
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

@Suppress("NAME_SHADOWING", "UNUSED_ANONYMOUS_PARAMETER", "LocalVariableName",
    "RemoveExplicitTypeArguments", "NestedLambdaShadowedImplicitParameter", "PropertyName",
    "RemoveRedundantQualifierName")
class TestQuery_ResponseAdapter(
  customScalarAdapters: ResponseAdapterCache
) : ResponseAdapter<TestQuery.Data> {
  val __SchemaAdapter: ResponseAdapter<TestQuery.Data.__Schema> = __Schema(customScalarAdapters)

  val __TypeAdapter: ResponseAdapter<TestQuery.Data.__Type> = __Type(customScalarAdapters)

  override fun fromResponse(reader: JsonReader): TestQuery.Data {
    var __schema: TestQuery.Data.__Schema? = null
    var __type: TestQuery.Data.__Type? = null
    reader.beginObject()
    while(true) {
      when (reader.selectName(RESPONSE_NAMES)) {
        0 -> __schema = __SchemaAdapter.fromResponse(reader)
        1 -> __type = __TypeAdapter.fromResponse(reader)
        else -> break
      }
    }
    reader.endObject()
    return TestQuery.Data(
      __schema = __schema!!,
      __type = __type!!
    )
  }

  override fun toResponse(writer: JsonWriter, value: TestQuery.Data) {
    writer.beginObject()
    writer.name("__schema")
    __SchemaAdapter.toResponse(writer, value.__schema)
    writer.name("__type")
    __TypeAdapter.toResponse(writer, value.__type)
    writer.endObject()
  }

  companion object {
    val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
      ResponseField(
        type = ResponseField.Type.NotNull(ResponseField.Type.Named.Object("__Schema")),
        fieldName = "__schema",
        fieldSets = listOf(
          ResponseField.FieldSet(null, __Schema.RESPONSE_FIELDS)
        ),
      ),
      ResponseField(
        type = ResponseField.Type.NotNull(ResponseField.Type.Named.Object("__Type")),
        fieldName = "__type",
        arguments = mapOf<String, Any?>(
          "name" to "Vehicle"),
        fieldSets = listOf(
          ResponseField.FieldSet(null, __Type.RESPONSE_FIELDS)
        ),
      )
    )

    val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
  }

  class __Schema(
    customScalarAdapters: ResponseAdapterCache
  ) : ResponseAdapter<TestQuery.Data.__Schema> {
    val queryTypeAdapter: ResponseAdapter<TestQuery.Data.__Schema.QueryType> =
        QueryType(customScalarAdapters)

    val listOfTypesAdapter: ResponseAdapter<List<TestQuery.Data.__Schema.Types>> =
        ListResponseAdapter(Types(customScalarAdapters))

    override fun fromResponse(reader: JsonReader): TestQuery.Data.__Schema {
      var queryType: TestQuery.Data.__Schema.QueryType? = null
      var types: List<TestQuery.Data.__Schema.Types>? = null
      reader.beginObject()
      while(true) {
        when (reader.selectName(RESPONSE_NAMES)) {
          0 -> queryType = queryTypeAdapter.fromResponse(reader)
          1 -> types = listOfTypesAdapter.fromResponse(reader)
          else -> break
        }
      }
      reader.endObject()
      return TestQuery.Data.__Schema(
        queryType = queryType!!,
        types = types!!
      )
    }

    override fun toResponse(writer: JsonWriter, value: TestQuery.Data.__Schema) {
      writer.beginObject()
      writer.name("queryType")
      queryTypeAdapter.toResponse(writer, value.queryType)
      writer.name("types")
      listOfTypesAdapter.toResponse(writer, value.types)
      writer.endObject()
    }

    companion object {
      val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField(
          type = ResponseField.Type.NotNull(ResponseField.Type.Named.Object("__Type")),
          fieldName = "queryType",
          fieldSets = listOf(
            ResponseField.FieldSet(null, QueryType.RESPONSE_FIELDS)
          ),
        ),
        ResponseField(
          type =
              ResponseField.Type.NotNull(ResponseField.Type.List(ResponseField.Type.NotNull(ResponseField.Type.Named.Object("__Type")))),
          fieldName = "types",
          fieldSets = listOf(
            ResponseField.FieldSet(null, Types.RESPONSE_FIELDS)
          ),
        )
      )

      val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
    }

    class QueryType(
      customScalarAdapters: ResponseAdapterCache
    ) : ResponseAdapter<TestQuery.Data.__Schema.QueryType> {
      val nullableStringAdapter: ResponseAdapter<String?> =
          NullableResponseAdapter(stringResponseAdapter)

      override fun fromResponse(reader: JsonReader): TestQuery.Data.__Schema.QueryType {
        var name: String? = null
        reader.beginObject()
        while(true) {
          when (reader.selectName(RESPONSE_NAMES)) {
            0 -> name = nullableStringAdapter.fromResponse(reader)
            else -> break
          }
        }
        reader.endObject()
        return TestQuery.Data.__Schema.QueryType(
          name = name
        )
      }

      override fun toResponse(writer: JsonWriter, value: TestQuery.Data.__Schema.QueryType) {
        writer.beginObject()
        writer.name("name")
        nullableStringAdapter.toResponse(writer, value.name)
        writer.endObject()
      }

      companion object {
        val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField(
            type = ResponseField.Type.Named.Other("String"),
            fieldName = "name",
          )
        )

        val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
      }
    }

    class Types(
      customScalarAdapters: ResponseAdapterCache
    ) : ResponseAdapter<TestQuery.Data.__Schema.Types> {
      val nullableStringAdapter: ResponseAdapter<String?> =
          NullableResponseAdapter(stringResponseAdapter)

      override fun fromResponse(reader: JsonReader): TestQuery.Data.__Schema.Types {
        var name: String? = null
        reader.beginObject()
        while(true) {
          when (reader.selectName(RESPONSE_NAMES)) {
            0 -> name = nullableStringAdapter.fromResponse(reader)
            else -> break
          }
        }
        reader.endObject()
        return TestQuery.Data.__Schema.Types(
          name = name
        )
      }

      override fun toResponse(writer: JsonWriter, value: TestQuery.Data.__Schema.Types) {
        writer.beginObject()
        writer.name("name")
        nullableStringAdapter.toResponse(writer, value.name)
        writer.endObject()
      }

      companion object {
        val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
          ResponseField(
            type = ResponseField.Type.Named.Other("String"),
            fieldName = "name",
          )
        )

        val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
      }
    }
  }

  class __Type(
    customScalarAdapters: ResponseAdapterCache
  ) : ResponseAdapter<TestQuery.Data.__Type> {
    val nullableStringAdapter: ResponseAdapter<String?> =
        NullableResponseAdapter(stringResponseAdapter)

    override fun fromResponse(reader: JsonReader): TestQuery.Data.__Type {
      var name: String? = null
      reader.beginObject()
      while(true) {
        when (reader.selectName(RESPONSE_NAMES)) {
          0 -> name = nullableStringAdapter.fromResponse(reader)
          else -> break
        }
      }
      reader.endObject()
      return TestQuery.Data.__Type(
        name = name
      )
    }

    override fun toResponse(writer: JsonWriter, value: TestQuery.Data.__Type) {
      writer.beginObject()
      writer.name("name")
      nullableStringAdapter.toResponse(writer, value.name)
      writer.endObject()
    }

    companion object {
      val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
        ResponseField(
          type = ResponseField.Type.Named.Other("String"),
          fieldName = "name",
        )
      )

      val RESPONSE_NAMES: List<String> = RESPONSE_FIELDS.map { it.responseName }
    }
  }
}
