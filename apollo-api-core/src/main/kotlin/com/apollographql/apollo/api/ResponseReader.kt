package com.apollographql.apollo.api

/*
 * ResponseReader is an abstraction for reading GraphQL fields.
 */
interface ResponseReader {

  fun readString(field: ResponseField): String?

  fun readInt(field: ResponseField): Int?

  fun readLong(field: ResponseField): Long?

  fun readDouble(field: ResponseField): Double?

  fun readBoolean(field: ResponseField): Boolean?

  fun <T> readObject(field: ResponseField, objectReader: ObjectReader<T>): T?

  @JvmSynthetic
  fun <T> readObject(field: ResponseField, block: (responseReader: ResponseReader) -> T): T? {
    return readObject(field, ObjectReader(block))
  }

  fun <T> readList(field: ResponseField, listReader: ListReader<T>): List<T>?

  @JvmSynthetic
  fun <T> readList(field: ResponseField, block: (responseReader: ListItemReader) -> T): List<T>? {
    return readList(field, ListReader(block))
  }

  fun <T> readCustomType(field: ResponseField.CustomTypeField): T?

  fun <T> readConditional(field: ResponseField, conditionalTypeReader: ConditionalTypeReader<T>): T?

  @JvmSynthetic
  fun <T> readConditional(field: ResponseField, block: (conditionalType: String, reader: ResponseReader) -> T?): T? {
    return readConditional(field, ConditionalTypeReader(block))
  }

  interface ObjectReader<T> {
    fun read(reader: ResponseReader): T

    companion object {
      @JvmSynthetic
      inline operator fun <T> invoke(crossinline block: (responseReader: ResponseReader) -> T) = object : ObjectReader<T> {
        override fun read(reader: ResponseReader): T {
          return block(reader)
        }
      }
    }
  }

  interface ListReader<T> {
    fun read(reader: ListItemReader): T

    companion object {
      @JvmSynthetic
      inline operator fun <T> invoke(crossinline block: (responseReader: ListItemReader) -> T) = object : ListReader<T> {
        override fun read(reader: ListItemReader): T {
          return block(reader)
        }
      }
    }
  }

  interface ConditionalTypeReader<T> {
    fun read(conditionalType: String, reader: ResponseReader): T?

    companion object {
      @JvmSynthetic
      inline operator fun <T> invoke(crossinline block: (conditionalType: String, reader: ResponseReader) -> T?) =
          object : ConditionalTypeReader<T> {
            override fun read(conditionalType: String, reader: ResponseReader): T? {
              return block(conditionalType, reader)
            }
          }
    }
  }

  interface ListItemReader {

    fun readString(): String

    fun readInt(): Int

    fun readLong(): Long

    fun readDouble(): Double

    fun readBoolean(): Boolean

    fun <T> readCustomType(scalarType: ScalarType): T

    fun <T> readObject(objectReader: ObjectReader<T>): T

    @JvmSynthetic
    fun <T> readObject(block: (responseReader: ResponseReader) -> T): T {
      return readObject(object : ObjectReader<T> {
        override fun read(reader: ResponseReader): T {
          return block(reader)
        }
      })
    }

    fun <T> readList(listReader: ListReader<T>): List<T>

    @JvmSynthetic
    fun <T> readList(block: (responseReader: ListItemReader) -> T): List<T> {
      return readList(object : ListReader<T> {
        override fun read(reader: ListItemReader): T {
          return block(reader)
        }
      })
    }
  }
}
