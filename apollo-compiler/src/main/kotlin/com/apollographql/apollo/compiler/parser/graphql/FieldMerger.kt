package com.apollographql.apollo.compiler.parser.graphql

import com.apollographql.apollo.compiler.ir.Field
import com.apollographql.apollo.compiler.ir.InlineFragment
import com.apollographql.apollo.compiler.parser.error.ParseException

internal fun List<Field>.mergeFields(others: List<Field>): List<Field> {
  val (missing, conflicted) = others.partition { otherField ->
    find { field -> field.responseName == otherField.responseName } == null
  }
  return map { field ->
    val otherField = conflicted.find { it.responseName == field.responseName }
    if (otherField != null) {
      field.merge(otherField)
    } else {
      field
    }
  } + missing
}

internal fun Field.merge(other: Field): Field {
  if (fieldName != other.fieldName) {
    throw ParseException(
        message = "Field `$responseName`$sourceLocation and `$responseName`${other.sourceLocation} conflicts because " +
            "they have different schema names. Use different aliases on the fields to fetch both.",
        sourceLocation = other.sourceLocation
    )
  }

  if (type != other.type) {
    throw ParseException(
        message = "Field `$responseName`$sourceLocation and `$responseName`${other.sourceLocation} conflicts because " +
            "they have different schema types. Use different aliases on the fields to fetch both.",
        sourceLocation = other.sourceLocation
    )
  }

  if (!args.containsAll(other.args)) {
    throw ParseException(
        message = "Field `$responseName`$sourceLocation and `$responseName`${other.sourceLocation} conflicts because " +
            "they have different arguments. Use different aliases on the fields to fetch both.",
        sourceLocation = other.sourceLocation
    )
  }

  return copy(
      fields = fields.mergeFields(other.fields),
      inlineFragments = inlineFragments.mergeInlineFragments(other.inlineFragments),
      fragmentRefs = (fragmentRefs).union(other.fragmentRefs).toList()
  )
}

private fun List<InlineFragment>.mergeInlineFragments(others: List<InlineFragment>): List<InlineFragment> {
  val (missing, conflicted) = others.partition { otherFragment ->
    find { fragment -> fragment.typeCondition == otherFragment.typeCondition } == null
  }
  return map { fragment ->
    val other = conflicted.find { it.typeCondition == fragment.typeCondition }
    if (other != null) {
      fragment.copy(
          fields = fragment.fields.mergeFields(other.fields),
          fragments = fragment.fragments.union(other.fragments).toList()
      )
    } else {
      fragment
    }
  } + missing
}
