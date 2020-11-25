package com.apollographql.apollo.compiler.ast

import com.apollographql.apollo.compiler.frontend.ir.CodeGenerationIR
import com.apollographql.apollo.compiler.frontend.ir.Field
import com.apollographql.apollo.compiler.frontend.ir.InlineFragment

internal fun CodeGenerationIR.flattenInlineFragments(): CodeGenerationIR {
  return copy(
      operations = operations.map { operation ->
        operation.copy(
            fields = operation.fields.flattenInlineFragments()
        )
      },
      fragments = fragments.map { fragment ->
        fragment.copy(
            fields = fragment.fields.flattenInlineFragments(),
            inlineFragments = fragment.inlineFragments.flatten()
        )
      }
  )
}

private fun List<Field>.flattenInlineFragments(): List<Field> {
  return map { field -> field.flattenInlineFragments() }
}

private fun Field.flattenInlineFragments(): Field {
  return takeIf { it.inlineFragments.isEmpty() } ?: copy(
      inlineFragments = inlineFragments.flatten()
  )
}

private fun List<InlineFragment>.flatten(): List<InlineFragment> {
  return flatMap { inlineFragment -> inlineFragment.flatten() }
}

private fun InlineFragment.flatten(): List<InlineFragment> {
  return listOf(
      copy(
          inlineFragments = emptyList(),
          fields = fields.flattenInlineFragments()
      )
  ) + inlineFragments.flatMap { it.flatten() }
}

internal fun List<Field>.merge(others: List<Field>): List<Field> {
  return others.fold(this.toMutableList()) { mergedFields, field ->
    val existingFieldIndex = mergedFields.indexOfFirst{ existingField -> existingField.responseName == field.responseName }
    if (existingFieldIndex == -1) {
      mergedFields.add(field)
    } else {
      val existingField = mergedFields[existingFieldIndex]
      mergedFields[existingFieldIndex] = existingField.copy(
          fields = existingField.fields.merge(field.fields)
      )
    }
    mergedFields
  }
}
