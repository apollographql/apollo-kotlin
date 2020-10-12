package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.BigDecimal
import com.apollographql.apollo.api.CustomTypeValue
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ScalarType
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.toNumber

class SimpleResponseReader private constructor(
    private val recordSet: Map<String, Any?>,
    private val variableValues: Map<String, Any?>,
    private val scalarTypeAdapters: ScalarTypeAdapters
) : ResponseReader {
  private val responseRecordSetIterator = recordSet.iterator()

  constructor(
      recordSet: Map<String, Any?>,
      variables: Operation.Variables,
      scalarTypeAdapters: ScalarTypeAdapters
  ) : this(recordSet, variables.valueMap(), scalarTypeAdapters)

  override fun selectField(fields: Array<ResponseField>): Int {
    while (true)
      if (responseRecordSetIterator.hasNext()) {
        val (nextFieldName, _) = responseRecordSetIterator.next()
        val fieldIndex = fields.indexOfFirst { field -> field.responseName == nextFieldName }
        if (fieldIndex != -1) {
          return fieldIndex
        }
      } else {
        return -1
      }
  }

  override fun readString(field: ResponseField): String? {
    return valueFor<String>(field)
  }

  override fun readInt(field: ResponseField): Int? {
    return valueFor<BigDecimal>(field)?.toNumber()?.toInt()
  }

  override fun readDouble(field: ResponseField): Double? {
    return valueFor<BigDecimal>(field)?.toNumber()?.toDouble()
  }

  override fun readBoolean(field: ResponseField): Boolean? {
    return valueFor<Boolean>(field)
  }

  override fun <T : Any> readObject(field: ResponseField, objectReader: ResponseReader.ObjectReader<T>): T? {
    return valueFor<Map<String, Any>>(field)?.let {
      objectReader.read(SimpleResponseReader(it, variableValues, scalarTypeAdapters))
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> readList(field: ResponseField, listReader: ResponseReader.ListReader<T>): List<T?>? {
    return valueFor<List<*>>(field)?.map { value ->
      value?.let { listReader.read(ListItemReader(field, it)) }
    }
  }

  override fun <T : Any> readCustomType(field: ResponseField.CustomTypeField): T? {
    return valueFor<Any>(field)?.let {
      val typeAdapter = scalarTypeAdapters.adapterFor<T>(field.scalarType)
      typeAdapter.decode(CustomTypeValue.fromRawValue(it))
    }
  }

  private inner class ListItemReader constructor(
      private val field: ResponseField,
      private val value: Any
  ) : ResponseReader.ListItemReader {

    override fun readString(): String {
      return value as String
    }

    override fun readInt(): Int {
      return (value as BigDecimal).toNumber().toInt()
    }

    override fun readDouble(): Double {
      return (value as BigDecimal).toNumber().toDouble()
    }

    override fun readBoolean(): Boolean {
      return value as Boolean
    }

    override fun <T : Any> readCustomType(scalarType: ScalarType): T {
      val typeAdapter = scalarTypeAdapters.adapterFor<T>(scalarType)
      return typeAdapter.decode(CustomTypeValue.fromRawValue(value))
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> readObject(objectReader: ResponseReader.ObjectReader<T>): T {
      val value = this.value as Map<String, Any>
      return objectReader.read(SimpleResponseReader(value, variableValues, scalarTypeAdapters))
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> readList(listReader: ResponseReader.ListReader<T>): List<T?> {
      val values = value as List<*>

      return values.map { value ->
        value?.let { listReader.read(ListItemReader(field, it)) }
      }
    }
  }

  private inline fun <reified T> valueFor(field: ResponseField): T? {
    return when (val value = recordSet[field.responseName]) {
      null -> {
        if (field.optional) null else throw NullPointerException(
            "Couldn't read `${field.responseName}` response value, expected non null value but was `null`"
        )
      }
      is T -> value
      else -> throw ClassCastException(
          "The value for \"${field.responseName}\" expected to be of type \"${T::class.simpleName}\" but was \"${value::class.simpleName}\""
      )
    }
  }
}
