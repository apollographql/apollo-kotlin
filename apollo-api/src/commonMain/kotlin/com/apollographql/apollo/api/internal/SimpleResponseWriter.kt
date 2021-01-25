package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.CustomScalar
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.ResponseField.Companion.leafType
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.Utils.writeToJson
import com.apollographql.apollo.api.internal.json.use
import okio.Buffer
import okio.IOException

class SimpleResponseWriter(private val customScalarAdapters: CustomScalarAdapters) : ResponseWriter {
  private val data: MutableMap<String, Any?> = LinkedHashMap()

  @Throws(IOException::class)
  fun toJson(indent: String?): String {
    return Buffer().apply {
      JsonWriter.of(this).use { jsonWriter ->
        jsonWriter.indent = indent
        jsonWriter.beginObject()
        jsonWriter.name("data")
        writeToJson(data, jsonWriter)
        jsonWriter.endObject()
      }
    }.readUtf8()
  }

  override fun writeString(field: ResponseField, value: String?) {
    data[field.responseName] = value
  }

  override fun writeInt(field: ResponseField, value: Int?) {
    data[field.responseName] = value
  }

  override fun writeLong(field: ResponseField, value: Long?) {
    data[field.responseName] = value
  }

  override fun writeDouble(field: ResponseField, value: Double?) {
    data[field.responseName] = value
  }

  override fun writeBoolean(field: ResponseField, value: Boolean?) {
    data[field.responseName] = value
  }

  override fun writeCustom(field: ResponseField, value: Any?) {
    if (value == null) {
      data[field.responseName] = null
    } else {
      val typeAdapter = customScalarAdapters.adapterFor<Any>(field.type.leafType())
      val jsonElement = typeAdapter.encode(value)
      data[field.responseName] = jsonElement.toRawValue()
    }
  }

  override fun writeObject(field: ResponseField, block: ((ResponseWriter) -> Unit)?) {
    if (block == null) {
      data[field.responseName] = null
    } else {
      val objectResponseWriter = SimpleResponseWriter(customScalarAdapters)
      block.invoke(objectResponseWriter)
      data[field.responseName] = objectResponseWriter.data
    }
  }

  override fun <T: Any> writeList(
      field: ResponseField,
      values: List<T?>?,
      block: (item: T, listItemWriter: ResponseWriter.ListItemWriter) -> Unit
  ) {
    if (values == null) {
      data[field.responseName] = null
    } else {
      val listItemWriter = CustomListItemWriter(customScalarAdapters)
      values.forEach {
        if (it == null) {
          listItemWriter.writeNull()
        } else {
          block(it, listItemWriter)
        }
      }
      data[field.responseName] = listItemWriter.data
    }
  }

  private class CustomListItemWriter(private val customScalarAdapters: CustomScalarAdapters) : ResponseWriter.ListItemWriter {
    val data = ArrayList<Any?>()

    override fun writeString(value: String) {
      data.add(value)
    }

    override fun writeInt(value: Int) {
      data.add(value)
    }

    override fun writeLong(value: Long) {
      data.add(value)
    }

    override fun writeDouble(value: Double) {
      data.add(value)
    }

    override fun writeBoolean(value: Boolean) {
      data.add(value)
    }

    override fun writeCustom(customScalar: CustomScalar, value: Any) {
      val typeAdapter = customScalarAdapters.adapterFor<Any>(customScalar)
      val jsonElement = typeAdapter.encode(value)
      data.add(jsonElement.toRawValue())
    }

    override fun writeObject(block: ((ResponseWriter) -> Unit)) {
      val objectResponseWriter = SimpleResponseWriter(customScalarAdapters)
      block.invoke(objectResponseWriter)
      data.add(objectResponseWriter.data)
    }

    override fun <T: Any> writeList(items: List<T?>, block: (item: T, listItemWriter: ResponseWriter.ListItemWriter) -> Unit) {
      val listItemWriter = CustomListItemWriter(customScalarAdapters)
      items.forEach {
        if (it == null) {
          listItemWriter.writeNull()
        } else {
          block(it, listItemWriter)
        }
      }
      data.add(listItemWriter.data)
    }

    fun writeNull() {
      data.add(null)
    }
  }
}
