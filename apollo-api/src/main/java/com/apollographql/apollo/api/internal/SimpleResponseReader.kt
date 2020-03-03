package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.*
import java.math.BigDecimal

class SimpleResponseReader private constructor(
    private val recordSet: Map<String, Any?>,
    private val variableValues: Map<String, Any?>,
    private val scalarTypeAdapters: ScalarTypeAdapters
) : ResponseReader {

  constructor(
      recordSet: Map<String, Any>,
      variables: Operation.Variables,
      scalarTypeAdapters: ScalarTypeAdapters
  ) : this(recordSet, variables.valueMap(), scalarTypeAdapters)

  override fun readString(field: ResponseField): String? {
    if (shouldSkip(field)) {
      return null
    }

    val value = valueFor<String>(recordSet, field)
    return checkValue(field, value)
  }

  override fun readInt(field: ResponseField): Int? {
    if (shouldSkip(field)) {
      return null
    }

    val value = valueFor<BigDecimal>(recordSet, field)
    checkValue(field, value)
    return value?.toInt()
  }

  override fun readLong(field: ResponseField): Long? {
    if (shouldSkip(field)) {
      return null
    }

    val value = valueFor<BigDecimal>(recordSet, field)
    checkValue(field, value)
    return value?.toLong()
  }

  override fun readDouble(field: ResponseField): Double? {
    if (shouldSkip(field)) {
      return null
    }

    val value = valueFor<BigDecimal>(recordSet, field)
    checkValue(field, value)
    return value?.toDouble()
  }

  override fun readBoolean(field: ResponseField): Boolean? {
    if (shouldSkip(field)) {
      return null
    }

    val value = valueFor<Boolean>(recordSet, field)
    return checkValue(field, value)
  }

  override fun <T : Any> readObject(field: ResponseField, objectReader: ResponseReader.ObjectReader<T>): T? {
    if (shouldSkip(field)) {
      return null
    }

    val value = valueFor<Map<String, Any>>(recordSet, field)
    checkValue(field, value)

    return value?.let {
      objectReader.read(SimpleResponseReader(it, variableValues, scalarTypeAdapters))
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> readList(field: ResponseField, listReader: ResponseReader.ListReader<T>): List<T?>? {
    if (shouldSkip(field)) {
      return null
    }

    val values = valueFor<List<*>>(recordSet, field)
    checkValue(field, values)

    return values?.map { value ->
      value?.let { listReader.read(ListItemReader(field, it)) }
    }
  }

  override fun <T : Any> readCustomType(field: ResponseField.CustomTypeField): T? {
    if (shouldSkip(field)) {
      return null
    }

    val value = valueFor<Any>(recordSet, field)
    checkValue(field, value)

    return value?.let {
      val typeAdapter = scalarTypeAdapters.adapterFor<T>(field.scalarType)
      typeAdapter.decode(CustomTypeValue.fromRawValue(it))
    }
  }

  override fun <T : Any> readFragment(field: ResponseField, objectReader: ResponseReader.ObjectReader<T>): T? {
    if (shouldSkip(field)) {
      return null
    }

    val value = valueFor<String>(recordSet, field)
    checkValue<String?>(field, value)

    return value?.let { typename ->
      field.conditions
          .firstOrNull { condition -> condition is ResponseField.TypeNameCondition && condition.typeNames.contains(typename) }
          ?.let { objectReader.read(this) }
    }
  }

  private fun shouldSkip(field: ResponseField): Boolean {
    for (condition in field.conditions) {
      if (condition is ResponseField.BooleanCondition) {
        val conditionValue = variableValues[condition.variableName] as Boolean
        if (condition.inverted) {
          // means it's a skip directive
          if (java.lang.Boolean.TRUE == conditionValue) {
            return true
          }
        } else {
          // means it's an include directive
          if (java.lang.Boolean.FALSE == conditionValue) {
            return true
          }
        }
      }
    }
    return false
  }

  private fun <V> checkValue(field: ResponseField, value: V?): V? {
    if (!field.optional && value == null) {
      throw NullPointerException("corrupted response reader, expected non null value for " + field.fieldName)
    }

    return value
  }

  private inner class ListItemReader internal constructor(
      private val field: ResponseField,
      private val value: Any
  ) : ResponseReader.ListItemReader {

    override fun readString(): String {
      return value as String
    }

    override fun readInt(): Int {
      return (value as BigDecimal).toInt()
    }

    override fun readLong(): Long {
      return (value as BigDecimal).toLong()
    }

    override fun readDouble(): Double {
      return (value as BigDecimal).toDouble()
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

  @Suppress("UNCHECKED_CAST")
  private fun <T> valueFor(map: Map<String, Any?>, field: ResponseField): T? {
    return map[field.responseName] as T?
  }
}
