package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.CustomScalar
import com.apollographql.apollo.api.internal.InputFieldMarshaller
import com.apollographql.apollo.api.internal.InputFieldWriter
import com.apollographql.apollo.api.internal.Throws
import okio.IOException

class SortedInputFieldMapWriter() : InputFieldWriter {
  private val buffer = mutableMapOf<String, Any?>()

  fun map(): Map<String, Any?> {
    return buffer.toList().sortedBy { it.first }.toMap()
  }

  override fun writeString(fieldName: String, value: String?) {
    buffer[fieldName] = value
  }

  override fun writeInt(fieldName: String, value: Int?) {
    buffer[fieldName] = value
  }

  override fun writeLong(fieldName: String, value: Long?) {
    buffer[fieldName] = value
  }

  override fun writeDouble(fieldName: String, value: Double?) {
    buffer[fieldName] = value
  }

  override fun writeNumber(fieldName: String, value: Number?) {
    buffer[fieldName] = value
  }

  override fun writeBoolean(fieldName: String, value: Boolean?) {
    buffer[fieldName] = value
  }

  override fun writeCustom(fieldName: String, customScalar: CustomScalar, value: Any?) {
    buffer[fieldName] = value
  }

  @Throws(IOException::class)
  override fun writeObject(fieldName: String, marshaller: InputFieldMarshaller?) {
    if (marshaller == null) {
      buffer[fieldName] = null
    } else {
      val nestedWriter = SortedInputFieldMapWriter()
      marshaller.marshal(nestedWriter)
      buffer[fieldName] = nestedWriter.map()
    }
  }

  @Throws(IOException::class)
  override fun writeList(fieldName: String, listWriter: InputFieldWriter.ListWriter?) {
    if (listWriter == null) {
      buffer[fieldName] = null
    } else {
      val listItemWriter = ListItemWriter()
      listWriter.write(listItemWriter)
      buffer[fieldName] = listItemWriter.list
    }
  }

  override fun writeMap(fieldName: String, value: Map<String, Any?>?) {
    buffer[fieldName] = value
  }

  private open class ListItemWriter internal constructor() : InputFieldWriter.ListItemWriter {
    val list = ArrayList<Any?>()
    override fun writeString(value: String?) {
      if (value != null) {
        list.add(value)
      }
    }

    override fun writeInt(value: Int?) {
      if (value != null) {
        list.add(value)
      }
    }

    override fun writeLong(value: Long?) {
      if (value != null) {
        list.add(value)
      }
    }

    override fun writeDouble(value: Double?) {
      if (value != null) {
        list.add(value)
      }
    }

    override fun writeNumber(value: Number?) {
      if (value != null) {
        list.add(value)
      }
    }

    override fun writeBoolean(value: Boolean?) {
      if (value != null) {
        list.add(value)
      }
    }

    override fun writeCustom(customScalar: CustomScalar, value: Any?) {
      if (value != null) {
        list.add(value)
      }
    }

    @Throws(IOException::class)
    override fun writeObject(marshaller: InputFieldMarshaller?) {
      if (marshaller != null) {
        val nestedWriter = SortedInputFieldMapWriter()
        marshaller.marshal(nestedWriter)
        list.add(nestedWriter.map())
      }
    }

    @Throws(IOException::class)
    override fun writeList(listWriter: InputFieldWriter.ListWriter?) {
      if (listWriter != null) {
        val nestedListItemWriter = ListItemWriter()
        listWriter.write(nestedListItemWriter)
        list.add(nestedListItemWriter.list)
      }
    }

    override fun writeMap(value: Map<String, Any?>?) {
      if (value != null) {
        list.add(value)
      }
    }

  }
}