package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.ScalarType
import com.apollographql.apollo.api.internal.InputFieldMarshaller
import com.apollographql.apollo.api.internal.InputFieldWriter
import java.io.IOException

class SortedInputFieldMapWriter(
    private val fieldNameComparator: Comparator<String>
) : InputFieldWriter {

  constructor(compare: (String, String) -> Int) : this(Comparator { o1, o2 -> compare(o1, o2) })

  private val buffer = mutableMapOf<String, Any?>().toSortedMap(fieldNameComparator)

  fun map(): Map<String, Any?> = buffer.toMap()

  override fun writeString(fieldName: String, value: String?) = buffer.set(fieldName, value)
  override fun writeInt(fieldName: String, value: Int?) = buffer.set(fieldName, value)
  override fun writeLong(fieldName: String, value: Long?) = buffer.set(fieldName, value)
  override fun writeDouble(fieldName: String, value: Double?) = buffer.set(fieldName, value)
  override fun writeNumber(fieldName: String, value: Number?) = buffer.set(fieldName, value)
  override fun writeBoolean(fieldName: String, value: Boolean?) = buffer.set(fieldName, value)
  override fun writeCustom(fieldName: String, scalarType: ScalarType, value: Any?) = buffer.set(fieldName, value)
  override fun writeMap(fieldName: String, value: Map<String, *>?) = buffer.set(fieldName, value)

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

  private class ListItemWriter(
      val fieldNameComparator: Comparator<String>
  ) : InputFieldWriter.ListItemWriter {

    internal val list = mutableListOf<Any>()

    override fun writeString(value: String?) = list.addIfNotNull(value)
    override fun writeInt(value: Int?) = list.addIfNotNull(value)
    override fun writeLong(value: Long?) = list.addIfNotNull(value)
    override fun writeDouble(value: Double?) = list.addIfNotNull(value)
    override fun writeNumber(value: Number?) = list.addIfNotNull(value)
    override fun writeBoolean(value: Boolean?) = list.addIfNotNull(value)
    override fun writeCustom(scalarType: ScalarType, value: Any?) = list.addIfNotNull(value)
    override fun writeMap(value: Map<String, *>?) = list.addIfNotNull(value)

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


    private fun MutableList<Any>.addIfNotNull(value: Any?) {
      if (value != null) {
        add(value)
      }
    }
  }
}
