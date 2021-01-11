package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.ResponseField


/**
 * A hierarchy of Json object with their fields attached used for normalization
 */
internal object NormalizationIR {
  sealed class Element {
    class Object(val fields: kotlin.collections.List<Field>) : Element() {
      data class Field(val fieldKey: String, val field: ResponseField, val element: Element)
    }
    class List(val elements: kotlin.collections.List<Element>) : Element()

    class Scalar(val value: Any?) : Element()
  }
}

internal fun NormalizationIR.Element.mergeWith(other: NormalizationIR.Element): NormalizationIR.Element {
  return when {
    this is NormalizationIR.Element.Scalar && other is NormalizationIR.Element.Scalar -> {
      check(this.value == other.value)
      this
    }
    this is NormalizationIR.Element.List && other is NormalizationIR.Element.List -> {
      check(this.elements.size == other.elements.size)

      NormalizationIR.Element.List(
          elements = elements.mapIndexed { index, element ->
            element.mergeWith(other.elements[index])
          }
      )
    }
    this is NormalizationIR.Element.Object && other is NormalizationIR.Element.Object -> {
      val newFields = fields.toMutableList()

      other.fields.forEach { otherField ->
        val index = newFields.indexOfFirst { it.fieldKey == otherField.fieldKey }
        if (index != -1) {
          val existingField = newFields.removeAt(index)
          newFields.add(index, existingField.mergeWith(otherField))
        } else {
          newFields.add(otherField)
        }
      }
      NormalizationIR.Element.Object(fields = newFields)
    }
    else -> throw  UnsupportedOperationException()
  }
}

internal fun NormalizationIR.Element.Object.Field.mergeWith(other: NormalizationIR.Element.Object.Field) = copy(element = element.mergeWith(other.element))