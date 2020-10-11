package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ScalarType

/*
 * ResponseReader is an abstraction for reading GraphQL fields.
 */
interface ResponseReader {

  fun readString(field: ResponseField): String?

  fun readInt(field: ResponseField): Int?

  fun readDouble(field: ResponseField): Double?

  fun readBoolean(field: ResponseField): Boolean?

  fun <T : Any> readObject(field: ResponseField, objectReader: ObjectReader<T>): T?

  fun <T : Any> readObject(field: ResponseField, block: (ResponseReader) -> T): T? {
    return readObject(field, object : ObjectReader<T> {
      override fun read(reader: ResponseReader): T {
        return block(reader)
      }
    })
  }

  fun <T : Any> readList(field: ResponseField, listReader: ListReader<T>): List<T?>?

  fun <T : Any> readList(field: ResponseField, block: (ListItemReader) -> T): List<T?>? {
    return readList(field, object : ListReader<T> {
      override fun read(reader: ListItemReader): T {
        return block(reader)
      }
    })
  }

  fun <T : Any> readCustomType(field: ResponseField.CustomTypeField): T?

  interface ObjectReader<T : Any> {
    fun read(reader: ResponseReader): T
  }

  interface ListReader<T : Any> {
    fun read(reader: ListItemReader): T
  }

  interface ListItemReader {

    fun readString(): String

    fun readInt(): Int

    fun readDouble(): Double

    fun readBoolean(): Boolean

    fun <T : Any> readCustomType(scalarType: ScalarType): T

    fun <T : Any> readObject(objectReader: ObjectReader<T>): T

    fun <T : Any> readObject(block: (ResponseReader) -> T): T {
      return readObject(object : ObjectReader<T> {
        override fun read(reader: ResponseReader): T {
          return block(reader)
        }
      })
    }

    fun <T : Any> readList(listReader: ListReader<T>): List<T?>

    fun <T : Any> readList(block: (ListItemReader) -> T): List<T?> {
      return readList(object : ListReader<T> {
        override fun read(reader: ListItemReader): T {
          return block(reader)
        }
      })
    }
  }
}
