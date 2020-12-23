package com.apollographql.apollo.api.internal.json

import com.apollographql.apollo.api.CustomTypeValue.*
import com.apollographql.apollo.api.ScalarType
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.internal.InputFieldMarshaller
import com.apollographql.apollo.api.internal.InputFieldWriter
import com.apollographql.apollo.api.internal.Throws
import com.apollographql.apollo.api.internal.json.Utils.writeToJson
import okio.IOException

class InputFieldJsonWriter(
    private val jsonWriter: JsonWriter,
    private val scalarTypeAdapters: ScalarTypeAdapters
) : InputFieldWriter {

  @Throws(IOException::class)
  override fun writeString(fieldName: String, value: String?) {
    if (value == null) {
      jsonWriter.name(fieldName).nullValue()
    } else {
      jsonWriter.name(fieldName).value(value)
    }
  }

  @Throws(IOException::class)
  override fun writeInt(fieldName: String, value: Int?) {
    if (value == null) {
      jsonWriter.name(fieldName).nullValue()
    } else {
      jsonWriter.name(fieldName).value(value)
    }
  }

  @Throws(IOException::class)
  override fun writeLong(fieldName: String, value: Long?) {
    if (value == null) {
      jsonWriter.name(fieldName).nullValue()
    } else {
      jsonWriter.name(fieldName).value(value)
    }
  }

  @Throws(IOException::class)
  override fun writeDouble(fieldName: String, value: Double?) {
    if (value == null) {
      jsonWriter.name(fieldName).nullValue()
    } else {
      jsonWriter.name(fieldName).value(value)
    }
  }

  @Throws(IOException::class)
  override fun writeNumber(fieldName: String, value: Number?) {
    if (value == null) {
      jsonWriter.name(fieldName).nullValue()
    } else {
      jsonWriter.name(fieldName).value(value)
    }
  }

  @Throws(IOException::class)
  override fun writeBoolean(fieldName: String, value: Boolean?) {
    if (value == null) {
      jsonWriter.name(fieldName).nullValue()
    } else {
      jsonWriter.name(fieldName).value(value)
    }
  }

  @Throws(IOException::class)
  override fun writeCustom(fieldName: String, scalarType: ScalarType, value: Any?) {
    if (value == null) {
      jsonWriter.name(fieldName).nullValue()
      return
    }

    val CustomScalarTypeAdapter = scalarTypeAdapters.adapterFor<Any>(scalarType)
    when (val customTypeValue = CustomScalarTypeAdapter.encode(value)) {
      is GraphQLString -> writeString(fieldName, customTypeValue.value)
      is GraphQLBoolean -> writeBoolean(fieldName, customTypeValue.value)
      is GraphQLNumber -> writeNumber(fieldName, customTypeValue.value)
      is GraphQLNull -> writeString(fieldName, null)
      is GraphQLJsonObject -> jsonWriter.name(fieldName).apply { writeToJson(customTypeValue.value, this) }
      is GraphQLJsonList -> jsonWriter.name(fieldName).apply { writeToJson(customTypeValue.value, this) }
    }
  }

  @Throws(IOException::class)
  override fun writeObject(fieldName: String, marshaller: InputFieldMarshaller?) {
    if (marshaller == null) {
      jsonWriter.name(fieldName).nullValue()
    } else {
      jsonWriter.name(fieldName).beginObject()
      marshaller.marshal(this)
      jsonWriter.endObject()
    }
  }

  @Throws(IOException::class)
  override fun writeList(fieldName: String, listWriter: InputFieldWriter.ListWriter?) {
    if (listWriter == null) {
      jsonWriter.name(fieldName).nullValue()
    } else {
      jsonWriter.name(fieldName).beginArray()
      listWriter.write(JsonListItemWriter(jsonWriter, scalarTypeAdapters))
      jsonWriter.endArray()
    }
  }

  @Throws(IOException::class)
  override fun writeMap(fieldName: String, value: Map<String, *>?) {
    if (value == null) {
      jsonWriter.name(fieldName).nullValue()
    } else {
      jsonWriter.name(fieldName)
      writeToJson(value, jsonWriter)
    }
  }

  private class JsonListItemWriter(
      private val jsonWriter: JsonWriter,
      private val scalarTypeAdapters: ScalarTypeAdapters
  ) : InputFieldWriter.ListItemWriter {

    @Throws(IOException::class)
    override fun writeString(value: String?) {
      if (value == null) {
        jsonWriter.nullValue()
      } else {
        jsonWriter.value(value)
      }
    }

    @Throws(IOException::class)
    override fun writeInt(value: Int?) {
      if (value == null) {
        jsonWriter.nullValue()
      } else {
        jsonWriter.value(value)
      }
    }

    @Throws(IOException::class)
    override fun writeLong(value: Long?) {
      if (value == null) {
        jsonWriter.nullValue()
      } else {
        jsonWriter.value(value)
      }
    }

    @Throws(IOException::class)
    override fun writeDouble(value: Double?) {
      if (value == null) {
        jsonWriter.nullValue()
      } else {
        jsonWriter.value(value)
      }
    }

    @Throws(IOException::class)
    override fun writeNumber(value: Number?) {
      if (value == null) {
        jsonWriter.nullValue()
      } else {
        jsonWriter.value(value)
      }
    }

    @Throws(IOException::class)
    override fun writeBoolean(value: Boolean?) {
      if (value == null) {
        jsonWriter.nullValue()
      } else {
        jsonWriter.value(value)
      }
    }

    @Throws(IOException::class)
    override fun writeMap(value: Map<String, *>?) {
      writeToJson(value, jsonWriter)
    }

    @Throws(IOException::class)
    override fun writeCustom(scalarType: ScalarType, value: Any?) {
      if (value == null) {
        jsonWriter.nullValue()
        return
      }

      val CustomScalarTypeAdapter = scalarTypeAdapters.adapterFor<Any>(scalarType)
      when (val customTypeValue = CustomScalarTypeAdapter.encode(value)) {
        is GraphQLString -> writeString(customTypeValue.value)
        is GraphQLBoolean -> writeBoolean(customTypeValue.value)
        is GraphQLNumber -> writeNumber(customTypeValue.value)
        is GraphQLJsonObject -> writeToJson(customTypeValue.value, jsonWriter)
        is GraphQLJsonList -> writeToJson(customTypeValue.value, jsonWriter)
        is GraphQLNull -> writeString(null)
      }
    }

    @Throws(IOException::class)
    override fun writeObject(marshaller: InputFieldMarshaller?) {
      if (marshaller == null) {
        jsonWriter.nullValue()
      } else {
        jsonWriter.beginObject()
        marshaller.marshal(InputFieldJsonWriter(jsonWriter, scalarTypeAdapters))
        jsonWriter.endObject()
      }
    }

    @Throws(IOException::class)
    override fun writeList(listWriter: InputFieldWriter.ListWriter?) {
      if (listWriter == null) {
        jsonWriter.nullValue()
      } else {
        jsonWriter.beginArray()
        listWriter.write(JsonListItemWriter(jsonWriter, scalarTypeAdapters))
        jsonWriter.endArray()
      }
    }
  }
}
