package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.BigDecimal
import com.apollographql.apollo.api.CustomScalarAdapter
import com.apollographql.apollo.api.JsonElement.Companion.fromRawValue
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.CustomScalar
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.toNumber

class RealResponseReader<R : Map<String, Any?>>(
    val operationVariables: Operation.Variables,
    private val recordSet: R,
    internal val fieldValueResolver: FieldValueResolver<R>,
    internal val customScalarAdapters: CustomScalarAdapters,
    internal val resolveDelegate: ResolveDelegate<R>
) : ResponseReader {
  private val variableValues: Map<String, Any?> = operationVariables.valueMap()
  private var selectedFieldIndex = -1

  override fun selectField(fields: Array<ResponseField>): Int {
    while (++selectedFieldIndex < fields.size) {
      if (!fields[selectedFieldIndex].shouldSkip(variableValues)) {
        return selectedFieldIndex
      }
    }
    return -1
  }

  override fun readString(field: ResponseField): String? {
    val value = fieldValueResolver.valueFor<String>(recordSet, field)
    checkValue(field, value)
    willResolve(field, value)
    if (value == null) {
      resolveDelegate.didResolveNull()
    } else {
      resolveDelegate.didResolveScalar(value)
    }
    didResolve(field)
    return value
  }

  override fun readInt(field: ResponseField): Int? {
    val value = fieldValueResolver.valueFor<BigDecimal>(recordSet, field)
    checkValue(field, value)
    willResolve(field, value)
    if (value == null) {
      resolveDelegate.didResolveNull()
    } else {
      resolveDelegate.didResolveScalar(value)
    }
    didResolve(field)
    return value?.toNumber()?.toInt()
  }

  override fun readDouble(field: ResponseField): Double? {
    val value = fieldValueResolver.valueFor<BigDecimal>(recordSet, field)
    checkValue(field, value)
    willResolve(field, value)
    if (value == null) {
      resolveDelegate.didResolveNull()
    } else {
      resolveDelegate.didResolveScalar(value)
    }
    didResolve(field)
    return value?.toNumber()?.toDouble()
  }

  override fun readBoolean(field: ResponseField): Boolean? {
    val value = fieldValueResolver.valueFor<Boolean>(recordSet, field)
    checkValue(field, value)
    willResolve(field, value)
    if (value == null) {
      resolveDelegate.didResolveNull()
    } else {
      resolveDelegate.didResolveScalar(value)
    }
    didResolve(field)
    return value
  }

  override fun <T : Any> readObject(field: ResponseField, block: (ResponseReader) -> T): T? {
    val value: R? = fieldValueResolver.valueFor(recordSet, field)
    checkValue(field, value)
    willResolve(field, value)
    resolveDelegate.willResolveObject(field, value)
    val parsedValue: T?
    parsedValue = if (value == null) {
      resolveDelegate.didResolveNull()
      null
    } else {
      block(RealResponseReader(operationVariables, value, fieldValueResolver, customScalarAdapters, resolveDelegate))
    }
    resolveDelegate.didResolveObject(field, value)
    didResolve(field)
    return parsedValue
  }

  override fun <T : Any> readList(field: ResponseField, block: (ResponseReader.ListItemReader) -> T): List<T?>? {
    val values = fieldValueResolver.valueFor<List<*>>(recordSet, field)
    checkValue(field, values)
    willResolve(field, values)
    val result = if (values == null) {
      resolveDelegate.didResolveNull()
      null
    } else {
      values.mapIndexed { index, value ->
        resolveDelegate.willResolveElement(index)
        if (value == null) {
          resolveDelegate.didResolveNull()
          null
        } else {
          block(ListItemReader(field, value))
        }.also { resolveDelegate.didResolveElement(index) }
      }.also { resolveDelegate.didResolveList(values) }
    }
    didResolve(field)
    return result
  }

  override fun <T : Any> readCustomScalar(field: ResponseField.CustomScalarField): T? {
    val value = fieldValueResolver.valueFor<Any>(recordSet, field)
    checkValue(field, value)
    willResolve(field, value)
    val result: T?
    if (value == null) {
      resolveDelegate.didResolveNull()
      result = null
    } else {
      val scalarTypeAdapter: CustomScalarAdapter<T> = customScalarAdapters.adapterFor(field.customScalar)
      result = scalarTypeAdapter.decode(fromRawValue(value))
      checkValue(field, result)
      resolveDelegate.didResolveScalar(value)
    }
    didResolve(field)
    return result
  }

  private fun willResolve(field: ResponseField, value: Any?) {
    resolveDelegate.willResolve(field, operationVariables, value)
  }

  private fun didResolve(field: ResponseField) {
    resolveDelegate.didResolve(field, operationVariables)
  }

  private fun checkValue(field: ResponseField, value: Any?) {
    check(field.optional || value != null) {
      "corrupted response reader, expected non null value for ${field.fieldName}"
    }
  }

  private fun ResponseField.shouldSkip(variableValues: Map<String, Any?>): Boolean {
    for (condition in conditions) {
      if (condition is ResponseField.BooleanCondition) {
        val conditionValue = variableValues[condition.variableName] as Boolean
        if (condition.isInverted) {
          // means it's a skip directive
          if (conditionValue) {
            return true
          }
        } else {
          // means it's an include directive
          if (!conditionValue) {
            return true
          }
        }
      }
    }
    return false
  }

  private inner class ListItemReader(
      private val field: ResponseField,
      private val value: Any
  ) : ResponseReader.ListItemReader {

    override fun readString(): String {
      resolveDelegate.didResolveScalar(value)
      return value as String
    }

    override fun readInt(): Int {
      resolveDelegate.didResolveScalar(value)
      return (value as BigDecimal).toNumber().toInt()
    }

    override fun readDouble(): Double {
      resolveDelegate.didResolveScalar(value)
      return (value as BigDecimal).toNumber().toDouble()
    }

    override fun readBoolean(): Boolean {
      resolveDelegate.didResolveScalar(value)
      return value as Boolean
    }

    override fun <T : Any> readCustomScalar(customScalar: CustomScalar): T {
      val scalarTypeAdapter: CustomScalarAdapter<T> = customScalarAdapters.adapterFor(customScalar)
      resolveDelegate.didResolveScalar(value)
      return scalarTypeAdapter.decode(fromRawValue(value))
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> readObject(block: (ResponseReader) -> T): T {
      val value = value as R
      resolveDelegate.willResolveObject(field, value)
      val item = block(RealResponseReader(operationVariables, value, fieldValueResolver, customScalarAdapters, resolveDelegate))
      resolveDelegate.didResolveObject(field, value)
      return item
    }

    override fun <T : Any> readList(block: (ResponseReader.ListItemReader) -> T): List<T?> {
      val values = value as List<*>
      val result = values.mapIndexed { index, value ->
        resolveDelegate.willResolveElement(index)
        if (value == null) {
          resolveDelegate.didResolveNull()
          null
        } else {
          block(ListItemReader(field, value))
        }.also { resolveDelegate.didResolveElement(index) }
      }
      resolveDelegate.didResolveList(values)
      return result
    }
  }
}
