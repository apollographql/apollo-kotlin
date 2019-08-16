package com.apollographql.apollo.api

/*
 * ResponseReader is an abstraction for reading GraphQL fields.
 */
interface ResponseReader {

  fun readString(field: ResponseField): String

  fun readInt(field: ResponseField): Int?

  fun readLong(field: ResponseField): Long?

  fun readDouble(field: ResponseField): Double?

  fun readBoolean(field: ResponseField): Boolean?

  fun <T> readObject(field: ResponseField, objectReader: ObjectReader<T>): T

  fun <T> readList(field: ResponseField, listReader: ListReader<T>): List<T>

  fun <T> readCustomType(field: ResponseField.CustomTypeField): T

  fun <T> readConditional(field: ResponseField, conditionalTypeReader: ConditionalTypeReader<T>): T

  interface ObjectReader<T> {
    fun read(reader: ResponseReader): T
  }

  interface ListReader<T> {
    fun read(reader: ListItemReader): T
  }

  interface ConditionalTypeReader<T> {
    fun read(conditionalType: String, reader: ResponseReader): T
  }

  interface ListItemReader {

    fun readString(): String

    fun readInt(): Int?

    fun readLong(): Long?

    fun readDouble(): Double?

    fun readBoolean(): Boolean?

    fun <T> readCustomType(scalarType: ScalarType): T

    fun <T> readObject(objectReader: ObjectReader<T>): T

    fun <T> readList(listReader: ListReader<T>): List<T>
  }
}
