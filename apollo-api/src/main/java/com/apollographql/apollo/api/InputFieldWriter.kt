package com.apollographql.apollo.api

import java.io.IOException

interface InputFieldWriter {
  @Throws(IOException::class)
  fun writeString(fieldName: String, value: String?)

  @Throws(IOException::class)
  fun writeInt(fieldName: String, value: Int?)

  @Throws(IOException::class)
  fun writeLong(fieldName: String, value: Long?)

  @Throws(IOException::class)
  fun writeDouble(fieldName: String, value: Double?)

  @Throws(IOException::class)
  fun writeNumber(fieldName: String, value: Number?)

  @Throws(IOException::class)
  fun writeBoolean(fieldName: String, value: Boolean?)

  @Throws(IOException::class)
  fun writeCustom(fieldName: String, scalarType: ScalarType, value: Any?)

  @Throws(IOException::class)
  fun writeObject(fieldName: String, marshaller: InputFieldMarshaller?)

  @Throws(IOException::class)
  fun writeList(fieldName: String, listWriter: ListWriter?)

  @Throws(IOException::class)
  fun writeMap(fieldName: String, value: Map<String, @JvmSuppressWildcards Any>?)

  interface ListWriter {
    @Throws(IOException::class)
    fun write(listItemWriter: ListItemWriter)
  }

  interface ListItemWriter {
    @Throws(IOException::class)
    fun writeString(value: String?)

    @Throws(IOException::class)
    fun writeInt(value: Int?)

    @Throws(IOException::class)
    fun writeLong(value: Long?)

    @Throws(IOException::class)
    fun writeDouble(value: Double?)

    @Throws(IOException::class)
    fun writeNumber(value: Number?)

    @Throws(IOException::class)
    fun writeBoolean(value: Boolean?)

    @Throws(IOException::class)
    fun writeCustom(scalarType: ScalarType, value: Any?)

    @Throws(IOException::class)
    fun writeObject(marshaller: InputFieldMarshaller?)

    @Throws(IOException::class)
    fun writeList(listWriter: ListWriter?)

    @Throws(IOException::class)
    fun writeMap(value: Map<String, @JvmSuppressWildcards Any>?)
  }
}
