package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.ScalarType
import com.apollographql.apollo.api.internal.InputFieldMarshaller
import com.apollographql.apollo.api.internal.InputFieldWriter
import com.apollographql.apollo.api.internal.Utils.__checkNotNull
import java.io.IOException
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.TreeMap

class SortedInputFieldMapWriter(fieldNameComparator: Comparator<String>) : InputFieldWriter {
  private val fieldNameComparator: Comparator<String>
  private val buffer: MutableMap<String, Any?>
  fun map(): Map<String, Any?> {
    return Collections.unmodifiableMap(buffer)
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

  override fun writeCustom(fieldName: String, scalarType: ScalarType, value: Any?) {
    buffer[fieldName] = value
  }

  @Throws(IOException::class)
  override fun writeObject(fieldName: String, marshaller: InputFieldMarshaller?) {
    if (marshaller == null) {
      buffer[fieldName] = null
    } else {
      val nestedWriter = SortedInputFieldMapWriter(fieldNameComparator)
      marshaller.marshal(nestedWriter)
      buffer[fieldName] = nestedWriter.buffer
    }
  }

  @Throws(IOException::class)
  override fun writeList(fieldName: String, listWriter: InputFieldWriter.ListWriter?) {
    if (listWriter == null) {
      buffer[fieldName] = null
    } else {
      val listItemWriter = ListItemWriter(fieldNameComparator)
      listWriter.write(listItemWriter)
      buffer[fieldName] = listItemWriter.list
    }
  }

  override fun writeMap(fieldName: String, value: Map<String, Any?>?) {
    buffer[fieldName] = value
  }

  private open class ListItemWriter internal constructor(val fieldNameComparator: Comparator<String>) : InputFieldWriter.ListItemWriter {
    val list: MutableList<Any?> = ArrayList<Any?>()
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

    override fun writeCustom(scalarType: ScalarType, value: Any?) {
      if (value != null) {
        list.add(value)
      }
    }

    @Throws(IOException::class)
    override fun writeObject(marshaller: InputFieldMarshaller?) {
      if (marshaller != null) {
        val nestedWriter = SortedInputFieldMapWriter(fieldNameComparator)
        marshaller.marshal(nestedWriter)
        list.add(nestedWriter.buffer)
      }
    }

    @Throws(IOException::class)
    override fun writeList(listWriter: InputFieldWriter.ListWriter?) {
      if (listWriter != null) {
        val nestedListItemWriter = ListItemWriter(fieldNameComparator)
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

  init {
    this.fieldNameComparator = __checkNotNull(fieldNameComparator, "fieldNameComparator == null")
    buffer = TreeMap(fieldNameComparator)
  }
}