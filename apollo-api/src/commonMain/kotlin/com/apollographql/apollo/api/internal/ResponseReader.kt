package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.CustomScalar

/*
 * ResponseReader is an abstraction for reading GraphQL fields.
 */
interface ResponseReader {

  fun selectField(fields: Array<ResponseField>): Int

  fun readString(field: ResponseField): String?

  fun readInt(field: ResponseField): Int?

  fun readDouble(field: ResponseField): Double?

  fun readBoolean(field: ResponseField): Boolean?

  fun <T : Any> readObject(field: ResponseField, block: (ResponseReader) -> T): T?

  fun <T : Any> readList(field: ResponseField, block: (ListItemReader) -> T): List<T?>?

  fun <T : Any> readCustomScalar(field: ResponseField.CustomScalarField): T?

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

    fun <T : Any> readCustomScalar(customScalar: CustomScalar): T

    fun <T : Any> readObject(block: (ResponseReader) -> T): T

    fun <T : Any> readList(block: (ListItemReader) -> T): List<T?>
  }
}
