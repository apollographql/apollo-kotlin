package com.apollographql.apollo.compiler.parser

import com.apollographql.apollo.compiler.ir.Field
import com.apollographql.apollo.compiler.ir.InlineFragment
import com.apollographql.apollo.compiler.ir.SourceLocation

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
        message = "Field `$responseName`${other.sourceLocation} conflicts with the same field at $sourceLocation " +
            "as they have different schema names. Use different aliases on the fields.",
        sourceLocation = other.sourceLocation
    )
  }

  if (type != other.type) {
    throw ParseException(
        message = "Field `$responseName`${other.sourceLocation} conflicts with the same field at $sourceLocation " +
            "as they have different schema types. Use different aliases on the fields.",
        sourceLocation = other.sourceLocation
    )
  }

  val locationIndependentArgs = args.map { it.copy(sourceLocation = SourceLocation.UNKNOWN) }
  val otherLocationIndependentArgs = other.args.map { it.copy(sourceLocation = SourceLocation.UNKNOWN) }
  if (!locationIndependentArgs.containsAll(otherLocationIndependentArgs)) {
    throw ParseException(
        message = "Field `$responseName`${other.sourceLocation} conflicts with the same field at $sourceLocation " +
            "as they have different arguments. Use different aliases on the fields.",
        sourceLocation = other.sourceLocation
    )
  }

  return copy(
      fields = fields.mergeFields(other.fields),
      inlineFragments = inlineFragments.mergeInlineFragments(other.inlineFragments),
      fragmentSpreads = (fragmentSpreads).union(other.fragmentSpreads).toList()
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
          fragmentSpreads = fragment.fragmentSpreads.union(other.fragmentSpreads).toList()
      )
    } else {
      fragment
    }
  } + missing
}
