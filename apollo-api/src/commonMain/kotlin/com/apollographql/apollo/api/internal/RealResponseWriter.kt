package com.apollographql.apollo.internal.response

import com.apollographql.apollo.api.BigDecimal
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ScalarType
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.internal.ResolveDelegate
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo.api.internal.ResponseWriter

class RealResponseWriter(
    private val operationVariables: Operation.Variables,
    private val scalarTypeAdapters: ScalarTypeAdapters
) : ResponseWriter {

  private val buffer: MutableMap<String, FieldDescriptor> = LinkedHashMap()

  override fun writeString(field: ResponseField, value: String?) {
    writeScalarFieldValue(field, value)
  }

  override fun writeInt(field: ResponseField, value: Int?) {
    writeScalarFieldValue(field, if (value != null) BigDecimal(value.toLong()) else null)
  }

  override fun writeLong(field: ResponseField, value: Long?) {
    writeScalarFieldValue(field, if (value != null) BigDecimal(value) else null)
  }

  override fun writeDouble(field: ResponseField, value: Double?) {
    writeScalarFieldValue(field, if (value != null) BigDecimal(value) else null)
  }

  override fun writeBoolean(field: ResponseField, value: Boolean?) {
    writeScalarFieldValue(field, value)
  }

  override fun writeCustom(field: ResponseField.CustomScalarField, value: Any?) {
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
    buffer[field.responseName] = FieldDescriptor(field, nestedResponseWriter.buffer)
  }

  override fun <T> writeList(
      field: ResponseField, values: List<T>?,
      block: (items: List<T>?, listItemWriter: ResponseWriter.ListItemWriter) -> Unit
  ) {
    checkFieldValue(field, values)
    if (values == null) {
      buffer[field.responseName] = FieldDescriptor(field, null)
      return
    }
    val accumulated = ArrayList<Any?>()
    block(values, ListItemWriter(operationVariables, scalarTypeAdapters, accumulated))
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

  private class ListItemWriter(
      private val operationVariables: Operation.Variables,
      private val scalarTypeAdapters: ScalarTypeAdapters,
      private val accumulator: MutableList<Any?>
  ) : ResponseWriter.ListItemWriter {
    override fun writeString(value: String?) {
      accumulator.add(value)
    }

    override fun writeInt(value: Int?) {
      accumulator.add(if (value != null) BigDecimal(value.toLong()) else null)
    }

    override fun writeLong(value: Long?) {
      accumulator.add(if (value != null) BigDecimal(value) else null)
    }

    override fun writeDouble(value: Double?) {
      accumulator.add(if (value != null) BigDecimal(value) else null)
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

    override fun <T> writeList(items: List<T>?, block: (items: List<T>?, listItemWriter: ResponseWriter.ListItemWriter) -> Unit) {
      if (items == null) {
        accumulator.add(null)
      } else {
        val nestedAccumulated = ArrayList<Any?>()
        block(items, ListItemWriter(operationVariables, scalarTypeAdapters, nestedAccumulated))
        accumulator.add(nestedAccumulated)
      }
    }
  }

  class FieldDescriptor internal constructor(val field: ResponseField, val value: Any?)

  companion object {
    private fun checkFieldValue(field: ResponseField, value: Any?) {
      if (!field.optional && value == null) {
        throw NullPointerException("Mandatory response field `${field.responseName}` resolved with null value")
      }
    }
  }
}
