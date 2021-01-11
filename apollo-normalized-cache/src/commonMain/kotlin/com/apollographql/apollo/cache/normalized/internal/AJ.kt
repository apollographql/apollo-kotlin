package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.ResponseField

/**
 * A hierarchy of Json object with their fields attached used for normalization
 */
sealed class AJElement

/**
 * field might be null for the root object
 */
class AJObject(val fields: List<Field>) : AJElement() {
  data class Field(val fieldKey: String, val field: ResponseField, val element: AJElement)
}
class AJList(val elements: List<AJElement>) : AJElement()

class AJScalar(val value: Any?) : AJElement()

fun AJElement.mergeWith(other: AJElement): AJElement {
  return when {
    this is AJScalar && other is AJScalar -> {
      check(this.value == other.value)
      this
    }
    this is AJList && other is AJList -> throw UnsupportedOperationException()
    this is AJObject && other is AJObject -> {
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
      AJObject(fields = newFields)
    }
    else -> throw  UnsupportedOperationException()
  }
}

fun AJObject.Field.mergeWith(other: AJObject.Field) = copy(element = element.mergeWith(other.element))