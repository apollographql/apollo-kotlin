package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.*
import java.math.BigDecimal
import java.util.*

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

  override fun <T> readObject(field: ResponseField, objectReader: ResponseReader.ObjectReader<T>): T? {
    if (shouldSkip(field)) {
      return null
    }

    val value = valueFor<Map<String, Any>>(recordSet, field)
    checkValue(field, value)
    return if (value != null) objectReader.read(SimpleResponseReader(value, variableValues, scalarTypeAdapters)) else null
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T> readList(field: ResponseField, listReader: ResponseReader.ListReader<T>): List<T>? {
    if (shouldSkip(field)) {
      return null
    }

    val values = valueFor<List<*>>(recordSet, field)
    checkValue(field, values)
    val result: MutableList<T>?
    if (values == null) {
      result = null
    } else {
      result = ArrayList()
      for (value in values) {
        if (value == null) {
          result.add(null as T)
        } else {
          val item = listReader.read(ListItemReader(field, value))
          result.add(item)
        }
      }
    }
    return if (result != null) Collections.unmodifiableList(result) else null
  }

  override fun <T> readCustomType(field: ResponseField.CustomTypeField): T? {
    if (shouldSkip(field)) {
      return null
    }

    val value = valueFor<Any>(recordSet, field)
    checkValue(field, value)
    val result: T?
    result = if (value == null) {
      null
    } else {
      val typeAdapter = scalarTypeAdapters.adapterFor<T>(field.scalarType())
      typeAdapter.decode(CustomTypeValue.fromRawValue(value))
    }
    return checkValue(field, result)
  }

  override fun <T> readConditional(field: ResponseField, conditionalTypeReader: ResponseReader.ConditionalTypeReader<T>): T? {
    if (shouldSkip(field)) {
      return null
    }

    val value = valueFor<String>(recordSet, field)
    checkValue(field, value)
    if (value == null) {
      return null
    } else {
      if (field.type() == ResponseField.Type.INLINE_FRAGMENT) {
        for (condition in field.conditions()) {
          if (condition is ResponseField.TypeNameCondition) {
            if (condition.typeName() == value) {
              return conditionalTypeReader.read(value, this)
            }
          }
        }
        return null
      } else {
        return conditionalTypeReader.read(value, this)
      }
    }
  }

  private fun shouldSkip(field: ResponseField): Boolean {
    for (condition in field.conditions()) {
      if (condition is ResponseField.BooleanCondition) {
        val conditionValue = variableValues[condition.variableName()] as Boolean
        if (condition.inverted()) {
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
    if (!field.optional() && value == null) {
      throw NullPointerException("corrupted response reader, expected non null value for " + field.fieldName())
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

    override fun <T> readCustomType(scalarType: ScalarType): T {
      val typeAdapter = scalarTypeAdapters.adapterFor<T>(scalarType)
      return typeAdapter.decode(CustomTypeValue.fromRawValue(value))
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> readObject(objectReader: ResponseReader.ObjectReader<T>): T {
      val value = this.value as Map<String, Any>
      return objectReader.read(SimpleResponseReader(value, variableValues, scalarTypeAdapters))
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> readList(listReader: ResponseReader.ListReader<T>): List<T> {
      val values = value as List<*>

      val result = ArrayList<T>()
      for (value in values) {
        if (value == null) {
          result.add(null as T)
        } else {
          result.add(listReader.read(ListItemReader(field, value)))
        }
      }
      return Collections.unmodifiableList(result)
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T> valueFor(map: Map<String, Any?>, field: ResponseField): T? {
    return map[field.responseName()] as T?
  }
}
