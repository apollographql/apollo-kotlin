package com.apollographql.apollo.api.internal.json

import com.apollographql.apollo.api.JsonElement.*
import com.apollographql.apollo.api.CustomScalar
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.internal.InputFieldMarshaller
import com.apollographql.apollo.api.internal.InputFieldWriter
import com.apollographql.apollo.api.internal.Throws
import com.apollographql.apollo.api.internal.json.Utils.writeToJson
import okio.IOException

class InputFieldJsonWriter(
    private val jsonWriter: JsonWriter,
    private val customScalarAdapters: CustomScalarAdapters
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
  override fun writeCustom(fieldName: String, customScalar: CustomScalar, value: Any?) {
    if (value == null) {
      jsonWriter.name(fieldName).nullValue()
      return
    }

    val customScalarAdapter = customScalarAdapters.adapterFor<Any>(customScalar)
    when (val jsonElement = customScalarAdapter.encode(value)) {
      is JsonString -> writeString(fieldName, jsonElement.value)
      is JsonBoolean -> writeBoolean(fieldName, jsonElement.value)
      is JsonNumber -> writeNumber(fieldName, jsonElement.value)
      is JsonNull -> writeString(fieldName, null)
      is JsonObject -> jsonWriter.name(fieldName).apply { writeToJson(jsonElement.toRawValue(), this) }
      is JsonList -> jsonWriter.name(fieldName).apply { writeToJson(jsonElement.toRawValue(), this) }
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
      listWriter.write(JsonListItemWriter(jsonWriter, customScalarAdapters))
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
      private val customScalarAdapters: CustomScalarAdapters
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
    override fun writeCustom(customScalar: CustomScalar, value: Any?) {
      if (value == null) {
        jsonWriter.nullValue()
        return
      }

      val customScalarAdapter = customScalarAdapters.adapterFor<Any>(customScalar)
      when (val jsonElement = customScalarAdapter.encode(value)) {
        is JsonString -> writeString(jsonElement.value)
        is JsonBoolean -> writeBoolean(jsonElement.value)
        is JsonNumber -> writeNumber(jsonElement.value)
        is JsonObject -> writeToJson(jsonElement.value, jsonWriter)
        is JsonList -> writeToJson(jsonElement.value, jsonWriter)
        is JsonNull -> writeString(null)
      }
    }

    @Throws(IOException::class)
    override fun writeObject(marshaller: InputFieldMarshaller?) {
      if (marshaller == null) {
        jsonWriter.nullValue()
      } else {
        jsonWriter.beginObject()
        marshaller.marshal(InputFieldJsonWriter(jsonWriter, customScalarAdapters))
        jsonWriter.endObject()
      }
    }

    @Throws(IOException::class)
    override fun writeList(listWriter: InputFieldWriter.ListWriter?) {
      if (listWriter == null) {
        jsonWriter.nullValue()
      } else {
        jsonWriter.beginArray()
        listWriter.write(JsonListItemWriter(jsonWriter, customScalarAdapters))
        jsonWriter.endArray()
      }
    }
  }
}
