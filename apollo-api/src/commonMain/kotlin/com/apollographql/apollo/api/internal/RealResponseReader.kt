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
    return value
  }

  override fun readInt(field: ResponseField): Int? {
    val value = fieldValueResolver.valueFor<BigDecimal>(recordSet, field)
    checkValue(field, value)
    
    return value?.toNumber()?.toInt()
  }

  override fun readDouble(field: ResponseField): Double? {
    val value = fieldValueResolver.valueFor<BigDecimal>(recordSet, field)
    checkValue(field, value)
    
    return value?.toNumber()?.toDouble()
  }

  override fun readBoolean(field: ResponseField): Boolean? {
    val value = fieldValueResolver.valueFor<Boolean>(recordSet, field)
    checkValue(field, value)
    
    return value
  }

  override fun <T : Any> readObject(field: ResponseField, block: (ResponseReader) -> T): T? {
    val value: R? = fieldValueResolver.valueFor(recordSet, field)
    checkValue(field, value)
    val parsedValue: T? = if (value == null) {
      null
    } else {
      block(RealResponseReader(operationVariables, value, fieldValueResolver, customScalarAdapters))
    }
    return parsedValue
  }

  override fun <T : Any> readList(field: ResponseField, block: (ResponseReader.ListItemReader) -> T): List<T?>? {
    val values = fieldValueResolver.valueFor<List<*>>(recordSet, field)
    checkValue(field, values)
    val result = if (values == null) {
      null
    } else {
      values.mapIndexed { index, value ->
        if (value == null) {
          null
        } else {
          block(ListItemReader(field, value))
        }
      }
    }
    return result
  }

  override fun <T : Any> readCustomScalar(field: ResponseField.CustomScalarField): T? {
    val value = fieldValueResolver.valueFor<Any>(recordSet, field)
    checkValue(field, value)
    val result: T?
    if (value == null) {
      result = null
    } else {
      val scalarTypeAdapter: CustomScalarAdapter<T> = customScalarAdapters.adapterFor(field.customScalar)
      result = scalarTypeAdapter.decode(fromRawValue(value))
      checkValue(field, result)
    }
    return result
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

    override fun <T : Any> readCustomScalar(customScalar: CustomScalar): T {
      val scalarTypeAdapter: CustomScalarAdapter<T> = customScalarAdapters.adapterFor(customScalar)
      return scalarTypeAdapter.decode(fromRawValue(value))
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> readObject(block: (ResponseReader) -> T): T {
      val value = value as R
      val item = block(RealResponseReader(operationVariables, value, fieldValueResolver, customScalarAdapters))
      return item
    }

    override fun <T : Any> readList(block: (ResponseReader.ListItemReader) -> T): List<T?> {
      val values = value as List<*>
      val result = values.mapIndexed { index, value ->
        if (value == null) {
          null
        } else {
          block(ListItemReader(field, value))
        }
      }
      return result
    }
  }
}
