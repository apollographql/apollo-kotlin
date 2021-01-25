package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.CustomScalar
import com.apollographql.apollo.api.Response

interface ResponseWriter {
  fun writeString(field: ResponseField, value: String?)

  fun writeInt(field: ResponseField, value: Int?)

  fun writeLong(field: ResponseField, value: Long?)

  fun writeDouble(field: ResponseField, value: Double?)

  fun writeBoolean(field: ResponseField, value: Boolean?)

  fun writeCustom(field: ResponseField, value: Any?)

  fun writeObject(field: ResponseField, block: ((ResponseWriter) -> Unit)?)

  fun <T: Any> writeList(field: ResponseField, values: List<T?>?, block: (item: T, listItemWriter: ListItemWriter) -> Unit)

  interface ListWriter<T> {
    fun write(items: List<T>?, listItemWriter: ListItemWriter)
  }

  interface ListItemWriter {
    fun writeString(value: String)

    fun writeInt(value: Int)

    fun writeLong(value: Long)

    fun writeDouble(value: Double)

    fun writeBoolean(value: Boolean)

    fun writeCustom(customScalar: CustomScalar, value: Any)

    fun writeObject(block: ((ResponseWriter) ->Unit))

    fun <T: Any> writeList(items: List<T?>, block: (item: T, listItemWriter: ListItemWriter) -> Unit)
  }
}
