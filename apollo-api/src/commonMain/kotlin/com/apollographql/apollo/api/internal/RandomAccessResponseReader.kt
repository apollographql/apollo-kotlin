package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.BigDecimal
import com.apollographql.apollo.api.CustomScalarAdapter
import com.apollographql.apollo.api.JsonElement.Companion.fromRawValue
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.CustomScalar
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.toNumber

/**
 * [RandomAccessResponseReader] is a [ResponseReader] that can read arbitrary fields from a root object
 *
 * @param M a map-like type
 * @param root the root object
 * @param a [ValueResolver] that can retrieve a given field from the map-like type M
 *
 */
class RandomAccessResponseReader<M : Map<String, Any?>>(
    private val variable: Operation.Variables,
    private val root: M,
    internal val valueResolver: ValueResolver<M>,
    internal val customScalarAdapters: CustomScalarAdapters,
) : ResponseReader {
  private val variableValues: Map<String, Any?> = variable.valueMap()
  private var selectedFieldIndex = -1

  override fun selectField(fields: Array<ResponseField>): Int {
    /**
     * Since we can read any field, just return all fields asked
     */
    while (++selectedFieldIndex < fields.size) {
      if (!fields[selectedFieldIndex].shouldSkip(variableValues)) {
        return selectedFieldIndex
      }
    }
    return -1
  }

  override fun readString(field: ResponseField): String? {
    val value = valueResolver.valueFor<String>(root, field)
    checkValue(field, value)
    return value
  }

  override fun readInt(field: ResponseField): Int? {
    val value = valueResolver.valueFor<BigDecimal>(root, field)
    checkValue(field, value)
    
    return value?.toNumber()?.toInt()
  }

  override fun readDouble(field: ResponseField): Double? {
    val value = valueResolver.valueFor<BigDecimal>(root, field)
    checkValue(field, value)
    
    return value?.toNumber()?.toDouble()
  }

  override fun readBoolean(field: ResponseField): Boolean? {
    val value = valueResolver.valueFor<Boolean>(root, field)
    checkValue(field, value)
    
    return value
  }

  override fun <T : Any> readObject(field: ResponseField, block: (ResponseReader) -> T): T? {
    val value: M? = valueResolver.valueFor(root, field)
    checkValue(field, value)
    val parsedValue: T? = if (value == null) {
      null
    } else {
      block(RandomAccessResponseReader(variable, value, valueResolver, customScalarAdapters))
    }
    return parsedValue
  }

  override fun <T : Any> readList(field: ResponseField, block: (ResponseReader.ListItemReader) -> T): List<T?>? {
    val values = valueResolver.valueFor<List<*>>(root, field)
    checkValue(field, values)
    val result = if (values == null) {
      null
    } else {
      values.mapIndexed { _, value ->
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
    val value = valueResolver.valueFor<Any>(root, field)
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
      val value = value as M
      val item = block(RandomAccessResponseReader(variable, value, valueResolver, customScalarAdapters))
      return item
    }

    override fun <T : Any> readList(block: (ResponseReader.ListItemReader) -> T): List<T?> {
      val values = value as List<*>
      val result = values.mapIndexed { _, value ->
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
