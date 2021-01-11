package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.BigDecimal
import com.apollographql.apollo.api.CustomScalar
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseWriter
import com.apollographql.apollo.api.internal.Utils.shouldSkip

/**
 *
 */
class NormalizationIRResponseWriter(
    private val operationVariables: Operation.Variables,
    private val customScalarAdapters: CustomScalarAdapters,
) : ResponseWriter, ResponseWriter.ListItemWriter {

  val root by lazy {
    /**
     * close the root object  if needed
     */
    check(objectStack.size == 1)
    NormalizationIR.Element.Object(fields = objectStack.removeLast())
  }

  /**
   * Might be null if we're in a list
   */
  private val fieldStack = mutableListOf<ResponseField?>()
  private val objectStack = mutableListOf<MutableList<NormalizationIR.Element.Object.Field>>()
  private val listStack = mutableListOf<MutableList<NormalizationIR.Element>>()

  private val cacheKeyBuilder = RealCacheKeyBuilder()

  init {
    objectStack.add(mutableListOf())
  }

  private fun shouldSkip(field: ResponseField, value: Any?): Boolean {
    if (field.shouldSkip(variableValues = operationVariables.valueMap())) {
      return true
    }

    if (!field.optional && value == null) {
      throw NullPointerException("Mandatory response field `${field.responseName}` resolved with null value")
    }

    return false
  }

  override fun writeString(field: ResponseField, value: String?) {
    writeScalar(field, value)
  }

  override fun writeInt(field: ResponseField, value: Int?) {
    writeScalar(field, if (value != null) BigDecimal(value.toString()) else null)
  }

  override fun writeLong(field: ResponseField, value: Long?) {
    writeScalar(field, if (value != null) BigDecimal(value.toString()) else null)
  }

  override fun writeDouble(field: ResponseField, value: Double?) {
    writeScalar(field, if (value != null) BigDecimal(value.toString()) else null)
  }

  override fun writeBoolean(field: ResponseField, value: Boolean?) {
    writeScalar(field, value)
  }

  override fun writeCustom(field: ResponseField.CustomScalarField, value: Any?) {
    val typeAdapter = customScalarAdapters.adapterFor<Any>(field.customScalar)
    writeScalar(field, if (value != null) typeAdapter.encode(value).toRawValue() else null)
  }

  private fun outputElement(element: NormalizationIR.Element) {
    val field = fieldStack.last()
    if (field == null) {
      listStack.last().add(element)
    } else {
      val fieldKey = cacheKeyBuilder.build(field, operationVariables)

      val fields = objectStack.last()

      val oldField = fields.firstOrNull { it.fieldKey == fieldKey }
      val newField = if (oldField != null) {
        /**
         * We have already something at this position, most likely an alias or overlapping fragments
         *
         * Example:
         * {
         *   hero {
         *     name
         *   }
         *   r2: hero {
         *     id
         *   }
         * }
         *
         * Merge everything
         */
        oldField.copy(element = oldField.element.mergeWith(element))
      } else {
        NormalizationIR.Element.Object.Field(field = field, fieldKey = fieldKey, element = element)
      }

      fields.add(newField)
    }
  }

  private fun writeScalar(field: ResponseField, value: Any?) {
    if (shouldSkip(field, value)) {
      return
    }

    fieldStack.add(field)
    outputElement(NormalizationIR.Element.Scalar(value))
    fieldStack.removeLast()
  }

  override fun writeObject(field: ResponseField, block: ((ResponseWriter) -> Unit)?) {
    if (shouldSkip(field, block)) {
      return
    }

    fieldStack.add(field)
    outputObject(block)
    fieldStack.removeLast()
  }

  private fun outputObject(block: ((ResponseWriter) -> Unit)?) {
    if (block == null) {
      outputElement(NormalizationIR.Element.Scalar(null))
      return
    }
    objectStack.add(mutableListOf())

    block(this)

    val fields = objectStack.removeLast()

    outputElement(NormalizationIR.Element.Object(fields = fields))

  }

  private fun <T> MutableList<T>.removeLast(): T = if (isEmpty()) throw NoSuchElementException("List is empty.") else removeAt(lastIndex)

  override fun <T : Any> writeList(
      field: ResponseField,
      values: List<T?>?,
      block: (item: T, listItemWriter: ResponseWriter.ListItemWriter) -> Unit
  ) {
    if (shouldSkip(field, values)) {
      return
    }

    fieldStack.add(field)
    outputList(values, block)
    fieldStack.removeLast()
  }

  private fun <T : Any> outputList(items: List<T?>?, block: (item: T, listItemWriter: ResponseWriter.ListItemWriter) -> Unit) {
    if (items == null) {
      outputElement(NormalizationIR.Element.Scalar(null))
      return
    }
    fieldStack.add(null)
    listStack.add(mutableListOf())
    items.forEach { item ->
      if (item == null) {
        outputElement(NormalizationIR.Element.Scalar(null))
      } else {
        block(item, this)
      }
    }
    val list = listStack.removeLast()
    fieldStack.removeLast()

    outputElement(NormalizationIR.Element.List(list))
  }


  override fun writeString(value: String) {
    outputElement(NormalizationIR.Element.Scalar(value))
  }

  override fun writeInt(value: Int) {
    outputElement(NormalizationIR.Element.Scalar(BigDecimal(value.toString())))
  }

  override fun writeLong(value: Long) {
    outputElement(NormalizationIR.Element.Scalar(BigDecimal(value.toString())))
  }

  override fun writeDouble(value: Double) {
    outputElement(NormalizationIR.Element.Scalar(BigDecimal(value.toString())))
  }

  override fun writeBoolean(value: Boolean) {
    outputElement(NormalizationIR.Element.Scalar(value))
  }

  override fun writeCustom(customScalar: CustomScalar, value: Any) {
    val typeAdapter = customScalarAdapters.adapterFor<Any>(customScalar)
    outputElement(NormalizationIR.Element.Scalar(typeAdapter.encode(value).toRawValue()))
  }

  override fun writeObject(block: ((ResponseWriter) -> Unit)) {
    outputObject(block)
  }

  override fun <T : Any> writeList(items: List<T?>, block: (item: T, listItemWriter: ResponseWriter.ListItemWriter) -> Unit) {
    outputList(items, block)
  }
}
