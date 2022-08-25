package com.apollographql.apollo.internal.response

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ScalarType
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.internal.ResolveDelegate
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo.api.internal.ResponseWriter
import java.math.BigDecimal
import java.util.ArrayList
import java.util.LinkedHashMap

class RealResponseWriter(private val operationVariables: Operation.Variables, private val scalarTypeAdapters: ScalarTypeAdapters) : ResponseWriter {
  val buffer: MutableMap<String, FieldDescriptor> = LinkedHashMap()
  override fun writeString(field: ResponseField, value: String?) {
    writeScalarFieldValue(field, value)
  }

  override fun writeInt(field: ResponseField, value: Int?) {
    writeScalarFieldValue(field, if (value != null) BigDecimal.valueOf(value.toLong()) else null)
  }

  override fun writeLong(field: ResponseField, value: Long?) {
    writeScalarFieldValue(field, if (value != null) BigDecimal.valueOf(value) else null)
  }

  override fun writeDouble(field: ResponseField, value: Double?) {
    writeScalarFieldValue(field, if (value != null) BigDecimal.valueOf(value) else null)
  }

  override fun writeBoolean(field: ResponseField, value: Boolean?) {
    writeScalarFieldValue(field, value)
  }

  override fun writeCustom(field: ResponseField.CustomTypeField, value: Any?) {
    val typeAdapter = scalarTypeAdapters.adapterFor<Any>(field.scalarType)
    writeScalarFieldValue(field, if (value != null) typeAdapter.encode(value).value else null)
  }

  override fun writeObject(field: ResponseField, marshaller: ResponseFieldMarshaller?) {
    checkFieldValue(field, marshaller)
    if (marshaller == null) {
      buffer[field.responseName] = FieldDescriptor(field, null)
      return
    }
    val nestedResponseWriter = RealResponseWriter(operationVariables, scalarTypeAdapters)
    marshaller.marshal(nestedResponseWriter)
    buffer[field.responseName] = deepMergeObjects(field, buffer[field.responseName]?.value, nestedResponseWriter.buffer)
  }

  private fun deepMergeObjects(field: ResponseField, oldValue: Any?, newValue: Map<String, FieldDescriptor>): FieldDescriptor {
    return if (oldValue == null || oldValue !is Map<*, *>) {
      FieldDescriptor(field, newValue)
    } else {
      val oldMap = oldValue as Map<String, FieldDescriptor>

      val mergedCommonValues = oldMap.keys.intersect(newValue.keys)
          .filter { newValue[it]?.value is Map<*, *> }
          .map { deepMergeObjects(oldMap[it]!!.field, oldMap[it]?.value, newValue[it]!!.value as Map<String, FieldDescriptor>) }
          .associateBy { it.field.responseName }

      FieldDescriptor(field, oldMap + newValue + mergedCommonValues)
    }
  }

  override fun writeFragment(marshaller: ResponseFieldMarshaller?) {
    marshaller?.marshal(this)
  }

  override fun <T> writeList(field: ResponseField, values: List<T>?, listWriter: ResponseWriter.ListWriter<T>) {
    checkFieldValue(field, values)
    if (values == null) {
      buffer[field.responseName] = FieldDescriptor(field, null)
      return
    }
    val accumulated = ArrayList<Any?>()
    listWriter.write(values, ListItemWriter(operationVariables, scalarTypeAdapters, accumulated))
    buffer[field.responseName] = FieldDescriptor(field, accumulated)
  }

  fun resolveFields(delegate: ResolveDelegate<Map<String, Any>?>) {
    resolveFields(operationVariables, delegate, buffer)
  }

  private fun writeScalarFieldValue(field: ResponseField, value: Any?) {
    checkFieldValue(field, value)
    buffer[field.responseName] = FieldDescriptor(field, value)
  }

  private fun rawFieldValues(buffer: Map<String, FieldDescriptor>): Map<String, Any?> {
    val fieldValues: MutableMap<String, Any?> = LinkedHashMap()
    for ((fieldResponseName, value) in buffer) {
      val fieldValue = value.value
      if (fieldValue == null) {
        fieldValues[fieldResponseName] = null
      } else if (fieldValue is Map<*, *>) {
        val nestedMap = rawFieldValues(fieldValue as Map<String, FieldDescriptor>)
        fieldValues[fieldResponseName] = nestedMap
      } else if (fieldValue is List<*>) {
        fieldValues[fieldResponseName] = rawListFieldValues(fieldValue)
      } else {
        fieldValues[fieldResponseName] = fieldValue
      }
    }
    return fieldValues
  }

  private fun rawListFieldValues(values: List<*>): List<*> {
    val listValues = ArrayList<Any?>()
    for (value in values) {
      if (value is Map<*, *>) {
        listValues.add(rawFieldValues(value as Map<String, FieldDescriptor>))
      } else if (value is List<*>) {
        listValues.add(rawListFieldValues(value))
      } else {
        listValues.add(value)
      }
    }
    return listValues
  }

  private fun resolveFields(operationVariables: Operation.Variables,
                            delegate: ResolveDelegate<Map<String, Any>?>, buffer: Map<String, FieldDescriptor>) {
    val rawFieldValues = rawFieldValues(buffer)
    for (fieldResponseName in buffer.keys) {
      val fieldDescriptor = buffer[fieldResponseName]
      val rawFieldValue = rawFieldValues[fieldResponseName]
      delegate.willResolve(fieldDescriptor!!.field, operationVariables, fieldDescriptor.value)
      when (fieldDescriptor.field.type) {
        ResponseField.Type.OBJECT -> {
          resolveObjectFields(fieldDescriptor, rawFieldValue as Map<String, Any>?, delegate)
        }
        ResponseField.Type.LIST -> {
          resolveListField(fieldDescriptor.field, fieldDescriptor.value as List<*>?, rawFieldValue as List<*>?, delegate)
        }
        else -> {
          if (rawFieldValue == null) {
            delegate.didResolveNull()
          } else {
            delegate.didResolveScalar(rawFieldValue)
          }
        }
      }
      delegate.didResolve(fieldDescriptor.field, operationVariables)
    }
  }

  private fun resolveObjectFields(fieldDescriptor: FieldDescriptor,
                                  rawFieldValues: Map<String, Any>?, delegate: ResolveDelegate<Map<String, Any>?>) {
    delegate.willResolveObject(fieldDescriptor.field, rawFieldValues)
    val value = fieldDescriptor.value
    if (value == null) {
      delegate.didResolveNull()
    } else {
      resolveFields(operationVariables, delegate, value as Map<String, FieldDescriptor>)
    }
    delegate.didResolveObject(fieldDescriptor.field, rawFieldValues)
  }

  private fun resolveListField(listResponseField: ResponseField, fieldValues: List<*>?,
                               rawFieldValues: List<*>?, delegate: ResolveDelegate<Map<String, Any>?>) {
    if (fieldValues == null) {
      delegate.didResolveNull()
      return
    }
    fieldValues.forEachIndexed { i, fieldValue ->
      delegate.willResolveElement(i)

      when (fieldValue) {
        is Map<*, *> -> {
          delegate.willResolveObject(listResponseField, rawFieldValues!![i] as Map<String, Any>?)
          resolveFields(operationVariables, delegate, fieldValue as Map<String, FieldDescriptor>)
          delegate.didResolveObject(listResponseField, rawFieldValues[i] as Map<String, Any>?)
        }
        is List<*> -> {
          resolveListField(listResponseField, fieldValue, rawFieldValues!![i] as List<*>?, delegate)
        }
        else -> {
          delegate.didResolveScalar(rawFieldValues!![i])
        }
      }
      delegate.didResolveElement(i)
    }
    delegate.didResolveList(rawFieldValues!!)
  }

  private class ListItemWriter internal constructor(val operationVariables: Operation.Variables, val scalarTypeAdapters: ScalarTypeAdapters, val accumulator: MutableList<Any?>) : ResponseWriter.ListItemWriter {
    override fun writeString(value: String?) {
      accumulator.add(value)
    }

    override fun writeInt(value: Int?) {
      accumulator.add(if (value != null) BigDecimal.valueOf(value.toLong()) else null)
    }

    override fun writeLong(value: Long?) {
      accumulator.add(if (value != null) BigDecimal.valueOf(value) else null)
    }

    override fun writeDouble(value: Double?) {
      accumulator.add(if (value != null) BigDecimal.valueOf(value) else null)
    }

    override fun writeBoolean(value: Boolean?) {
      accumulator.add(value)
    }

    override fun writeCustom(scalarType: ScalarType, value: Any?) {
      val typeAdapter = scalarTypeAdapters.adapterFor<Any>(scalarType)
      accumulator.add(if (value != null) typeAdapter.encode(value).value else null)
    }

    override fun writeObject(marshaller: ResponseFieldMarshaller?) {
      val nestedResponseWriter = RealResponseWriter(operationVariables, scalarTypeAdapters)
      marshaller!!.marshal(nestedResponseWriter)
      accumulator.add(nestedResponseWriter.buffer)
    }

    override fun <T> writeList(items: List<T>?, listWriter: ResponseWriter.ListWriter<T>) {
      if (items == null) {
        accumulator.add(null)
      } else {
        val nestedAccumulated = ArrayList<Any?>()
        listWriter.write(items, ListItemWriter(operationVariables, scalarTypeAdapters, nestedAccumulated))
        accumulator.add(nestedAccumulated)
      }
    }

  }

  class FieldDescriptor internal constructor(val field: ResponseField, val value: Any?)

  companion object {
    private fun checkFieldValue(field: ResponseField, value: Any?) {
      if (!field.optional && value == null) {
        throw NullPointerException(String.format("Mandatory response field `%s` resolved with null value",
            field.responseName))
      }
    }
  }

}
