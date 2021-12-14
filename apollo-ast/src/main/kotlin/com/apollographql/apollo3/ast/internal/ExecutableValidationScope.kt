package com.apollographql.apollo3.ast.internal

import com.apollographql.apollo3.ast.GQLArgument
import com.apollographql.apollo3.ast.GQLBooleanValue
import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.GQLEnumValue
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFloatValue
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLIntValue
import com.apollographql.apollo3.ast.GQLListType
import com.apollographql.apollo3.ast.GQLListValue
import com.apollographql.apollo3.ast.GQLNamedType
import com.apollographql.apollo3.ast.GQLNonNullType
import com.apollographql.apollo3.ast.GQLNullValue
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLObjectValue
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.GQLScalarTypeDefinition
import com.apollographql.apollo3.ast.GQLSelectionSet
import com.apollographql.apollo3.ast.GQLStringValue
import com.apollographql.apollo3.ast.GQLType
import com.apollographql.apollo3.ast.GQLTypeDefinition
import com.apollographql.apollo3.ast.GQLValue
import com.apollographql.apollo3.ast.GQLVariableValue
import com.apollographql.apollo3.ast.Issue
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.VariableReference
import com.apollographql.apollo3.ast.definitionFromScope
import com.apollographql.apollo3.ast.findDeprecationReason
import com.apollographql.apollo3.ast.leafType
import com.apollographql.apollo3.ast.pretty
import com.apollographql.apollo3.ast.responseName
import com.apollographql.apollo3.ast.rootTypeDefinition
import com.apollographql.apollo3.ast.sharesPossibleTypesWith
import java.util.Locale

/**
 * @param fragmentDefinitions: all the fragments in the current compilation unit. This is required to check the type conditions as well as fields merging
 */
internal class ExecutableValidationScope(
    private val schema: Schema,
    private val fragmentDefinitions: Map<String, GQLFragmentDefinition>,
) : ValidationScope, VariableReferencesScope {
  override val typeDefinitions = schema.typeDefinitions
  override val directiveDefinitions = schema.directiveDefinitions

  override val issues = mutableListOf<Issue>()

  /**
   * As the tree is walked, variable references will be put here
   */
  override val variableReferences = mutableListOf<VariableReference>()

  fun validate(document: GQLDocument): List<Issue> {
    document.validateExecutable()

    val fragments = document.definitions.filterIsInstance<GQLFragmentDefinition>()
    fragments.checkDuplicateFragments()
    fragments.forEach {
      it.validate()
    }

    val operations = document.definitions.filterIsInstance<GQLOperationDefinition>()
    operations.checkDuplicateOperations()
    operations.forEach {
      it.validate()
    }

    return issues
  }

  fun validateOperation(operation: GQLOperationDefinition): List<Issue> {
    operation.validate()

    return issues
  }

  fun validateFragment(fragment: GQLFragmentDefinition): List<Issue> {
    fragment.validate()

    return issues
  }

  fun inferFragmentVariables(fragment: GQLFragmentDefinition): List<VariableReference> {
    variableReferences.clear()
    fragment.validate()

    variableReferences.groupBy {
      it.variable.name
    }.forEach {
      val types = it.value.map { it.expectedType.pretty() }.distinct()
      check(types.size == 1) {
        "Fragment ${fragment.name} uses different types for variable '${it.key}': ${types.joinToString()}"
      }
    }
    return variableReferences.distinctBy { it.variable.name }
  }

  private fun decapitalizeFirstLetter(name: String): String {
    val builder = StringBuilder(name.length)
    var isDecapitalized = false
    name.forEach {
      builder.append(if (!isDecapitalized && it.isLetter()) {
        isDecapitalized = true
        it.toString().lowercase()
      } else {
        it.toString()
      })
    }
    return builder.toString()
  }

  private fun isFirstLetterUpperCase(name: String): Boolean {
    return name.firstOrNull { it.isLetter() }?.isUpperCase() ?: true
  }

  private fun GQLField.validate(typeDefinitionInScope: GQLTypeDefinition) {
    val fieldDefinition = definitionFromScope(schema, typeDefinitionInScope)
    if (fieldDefinition == null) {
      registerIssue(
          message = "Can't query `$name` on type `${typeDefinitionInScope.name}`",
          sourceLocation = sourceLocation
      )
      return
    }

    if (alias != null) {
      if (isFirstLetterUpperCase(alias)) {
        issues.add(Issue.UpperCaseField(message = """
                      Capitalized alias '$alias' is not supported as it causes name clashes with the generated models. Use '${decapitalizeFirstLetter(alias)}' instead.
                    """.trimIndent(),
            sourceLocation = sourceLocation)
        )
      }
    } else if (isFirstLetterUpperCase(name)) {
      issues.add(Issue.UpperCaseField(message = """
                      Capitalized field '$name' is not supported as it causes name clashes with the generated models. Use an alias instead.
                    """.trimIndent(),
          sourceLocation = sourceLocation)
      )
    }

    if (fieldDefinition.directives.findDeprecationReason() != null) {
      issues.add(Issue.DeprecatedUsage(message = "Use of deprecated field `$name`", sourceLocation = sourceLocation))
    }
    arguments?.let {
      validateArguments(it, fieldDefinition.arguments, "field `${fieldDefinition.name}`")
    }

    val leafTypeDefinition = typeDefinitions[fieldDefinition.type.leafType().name]

    if (leafTypeDefinition == null) {
      registerIssue(
          message = "Unknown type `${fieldDefinition.type.leafType().name}`",
          sourceLocation = sourceLocation
      )
      return
    }

    if (leafTypeDefinition !is GQLScalarTypeDefinition
        && leafTypeDefinition !is GQLEnumTypeDefinition) {
      if (selectionSet == null) {
        registerIssue(
            message = "Field `$name` of type `${fieldDefinition.type.pretty()}` must have a selection of sub-fields",
            sourceLocation = sourceLocation
        )
        return
      }
      selectionSet.validate(leafTypeDefinition)
    } else {
      if (selectionSet != null) {
        registerIssue(
            message = "Field `$name` of type `${fieldDefinition.type.pretty()}` must not have a selection of sub-fields",
            sourceLocation = sourceLocation
        )
        return
      }
    }

    directives.forEach {
      validateDirective(it, this)
    }
  }


  private fun GQLInlineFragment.validate(typeDefinitionInScope: GQLTypeDefinition) {
    val inlineFragmentTypeDefinition = typeDefinitions[typeCondition.name]
    if (inlineFragmentTypeDefinition == null) {
      registerIssue(
          message = "Cannot find type `${typeCondition.name}` for inline fragment",
          sourceLocation = typeCondition.sourceLocation
      )
      return
    }

    if (!inlineFragmentTypeDefinition.sharesPossibleTypesWith(other = typeDefinitionInScope, schema = schema)) {
      registerIssue(
          message = "Inline fragment cannot be spread here as result can never be of type `${typeCondition.name}`",
          sourceLocation = typeCondition.sourceLocation
      )
      return
    }

    selectionSet.validate(inlineFragmentTypeDefinition)

    directives.forEach {
      validateDirective(it, this)
    }
  }

  private fun GQLFragmentSpread.validate(typeDefinitionInScope: GQLTypeDefinition) {
    val fragmentDefinition = fragmentDefinitions[name]
    if (fragmentDefinition == null) {
      registerIssue(
          message = "Cannot find fragment `$name`",
          sourceLocation = sourceLocation
      )
      return
    }

    val fragmentTypeDefinition = typeDefinitions[fragmentDefinition.typeCondition.name]
    if (fragmentTypeDefinition == null) {
      registerIssue(
          message = "Cannot find type `${fragmentDefinition.typeCondition.name}` for fragment $name",
          sourceLocation = fragmentDefinition.typeCondition.sourceLocation
      )
      return
    }

    if (!fragmentTypeDefinition.sharesPossibleTypesWith(other = typeDefinitionInScope, schema = schema)) {
      registerIssue(
          message = "Fragment `$name` cannot be spread here as result can never be of type `${typeDefinitionInScope.name}`",
          sourceLocation = sourceLocation
      )
      return
    }

    fragmentDefinition.selectionSet.validate(fragmentTypeDefinition)

    directives.forEach {
      validateDirective(it, this)
    }
  }

  private fun GQLDocument.validateExecutable() {
    definitions.firstOrNull { it !is GQLOperationDefinition && it !is GQLFragmentDefinition }
        ?.let {
          registerIssue(message = "Found an non-executable definition.", sourceLocation = it.sourceLocation)
          return
        }
  }

  private fun GQLFragmentDefinition.validate() {
    val fragmentRootTypeDefinition = typeDefinitions[typeCondition.name]
    if (fragmentRootTypeDefinition == null) {
      registerIssue(
          message = "Cannot find type `${typeCondition.name}` for fragment `$name`",
          sourceLocation = typeCondition.sourceLocation
      )
      return
    }

    /**
     * Validate the fragment outside the context of an operation
     * This can be helpful to show warnings in the IDE while editing fragments of a parent module and the fragment may appear unused
     * This will not catch field merging conflicts and missing variables so ultimately, validation
     * against all fragments is required
     */
    selectionSet.validate(fragmentRootTypeDefinition)

    fieldsInSetCanMerge(selectionSet.collectFields(fragmentRootTypeDefinition.name))
  }

  private fun GQLOperationDefinition.validate() {
    variableReferences.clear()

    val rootTypeDefinition = rootTypeDefinition(schema)

    if (rootTypeDefinition == null) {
      registerIssue(
          message = "Cannot find a root type for operation type `$operationType`",
          sourceLocation = sourceLocation
      )
      return
    }

    selectionSet.validate(rootTypeDefinition)

    fieldsInSetCanMerge(selectionSet.collectFields(rootTypeDefinition.name))

    variableReferences.forEach {
      validateVariable(this, it.variable, it.expectedType)
    }
    val foundVariables = variableReferences.map { it.variable.name }.toSet()
    variableDefinitions.forEach {
      if (!foundVariables.contains(it.name)) {
        issues.add(Issue.UnusedVariable(
            message = "Variable `${it.name}` is unused",
            sourceLocation = it.sourceLocation
        ))
      }
    }
  }

  private fun GQLSelectionSet.validate(typeDefinitionInScope: GQLTypeDefinition) {
    if (selections.isEmpty()) {
      // This will never happen from parsing documents but is kept for reference and to catch bad manual document modifications
      registerIssue(
          message = "Selection of type `${typeDefinitionInScope.name}` must have a selection of sub-fields",
          sourceLocation = sourceLocation
      )
      return
    }

    selections.forEach {
      when (it) {
        is GQLField -> it.validate(typeDefinitionInScope)
        is GQLInlineFragment -> it.validate(typeDefinitionInScope)
        is GQLFragmentSpread -> it.validate(typeDefinitionInScope)
      }
    }
  }

  /**
   * XXX: optimize by not visiting the same fields several times
   */
  private fun fieldPairCanMerge(fieldWithParentA: FieldWithParent, fieldWithParentB: FieldWithParent) {
    val parentTypeDefinitionA = fieldWithParentA.parentTypeDefinition
    val parentTypeDefinitionB = fieldWithParentB.parentTypeDefinition

    if (parentTypeDefinitionA.name != parentTypeDefinitionB.name
        && parentTypeDefinitionA is GQLObjectTypeDefinition
        && parentTypeDefinitionB is GQLObjectTypeDefinition) {
      // 5.3.2 2.b disjoint objects merge only if they have the same shape
      sameResponseShapeRecursive(fieldWithParentA, fieldWithParentB)
      return
    }

    val fieldA = fieldWithParentA.field
    val fieldB = fieldWithParentB.field

    val typeA = fieldA.definitionFromScope(schema, parentTypeDefinitionA)?.type
    val typeB = fieldB.definitionFromScope(schema, parentTypeDefinitionB)?.type
    if (typeA == null || typeB == null) {
      // will be caught by other validation rules
      return
    }

    if (!areTypesEqual(typeA, typeB)) {
      addFieldMergingIssue(fieldWithParentA.field, fieldWithParentB.field, "they have different types")
      return
    }

    if (!areArgumentsEqual(
            fieldA.arguments?.arguments ?: emptyList(),
            fieldB.arguments?.arguments ?: emptyList())) {
      addFieldMergingIssue(fieldWithParentA.field, fieldWithParentB.field, "they have different arguments")
      return
    }

    // no need to call the recursive version here, it will be taken care at the end of this iteration
    if (!haveSameResponseShape(fieldWithParentA, fieldWithParentB)) {
      addFieldMergingIssue(fieldWithParentA.field, fieldWithParentB.field, "they have different shapes")
      return
    }

    val setA = fieldA.selectionSet?.collectFields(parentTypeDefinitionA.name) ?: emptyList()
    val setB = fieldB.selectionSet?.collectFields(parentTypeDefinitionB.name) ?: emptyList()

    fieldsInSetCanMerge(setA + setB)
  }

  private fun fieldsInSetCanMerge(fieldsWithParent: List<FieldWithParent>) {
    fieldsWithParent.groupBy { it.field.responseName() }
        .values
        .forEach { fieldsForName ->
          fieldsForName.pairs().forEach {
            fieldPairCanMerge(it.first, it.second)
          }
        }
  }

  private fun areTypesEqual(typeA: GQLType, typeB: GQLType): Boolean {
    return when (typeA) {
      is GQLNonNullType -> (typeB is GQLNonNullType) && areTypesEqual(typeA.type, typeB.type)
      is GQLListType -> (typeB is GQLListType) && areTypesEqual(typeA.type, typeB.type)
      is GQLNamedType -> (typeB is GQLNamedType) && typeA.name == typeB.name
    }
  }

  private fun areArgumentsEqual(argumentsA: List<GQLArgument>, argumentsB: List<GQLArgument>): Boolean {
    if (argumentsA.size != argumentsB.size) {
      return false
    }

    (argumentsA + argumentsB).groupBy {
      // other validations will ensure no duplicates so it's safe to
      // put everything in a Map here
      it.name
    }.values.forEach {
      if (it.size != 2) {
        // some argument is missing
        return false
      }
      if (!areValuesEqual(it[0].value, it[1].value)) {
        return false
      }
    }

    return true
  }

  private fun addFieldMergingIssue(fieldA: GQLField, fieldB: GQLField, message: String) {
    registerIssue(
        message = "`${fieldA.responseName()}` cannot be merged with `${fieldB.responseName()}`: $message",
        sourceLocation = fieldA.sourceLocation
    )
    // Also add the symmetrical error
    registerIssue(
        message = "`${fieldB.responseName()}` cannot be merged with `${fieldA.responseName()}`: $message",
        sourceLocation = fieldB.sourceLocation
    )
  }

  private fun areValuesEqual(valueA: GQLValue, valueB: GQLValue): Boolean {
    return when (valueA) {
      is GQLIntValue -> (valueB as? GQLIntValue)?.value == valueA.value
      is GQLFloatValue -> (valueB as? GQLFloatValue)?.value == valueA.value
      is GQLStringValue -> (valueB as? GQLStringValue)?.value == valueA.value
      is GQLBooleanValue -> (valueB as? GQLBooleanValue)?.value == valueA.value
      is GQLEnumValue -> (valueB as? GQLEnumValue)?.value == valueA.value
      is GQLNullValue -> valueB is GQLNullValue
      is GQLListValue -> {
        if (valueB !is GQLListValue) {
          return false
        }

        for (i in 0.until(valueA.values.size)) {
          if (!areValuesEqual(valueA.values[i], valueB.values[i])) {
            return false
          }
        }
        true
      }
      is GQLObjectValue -> {
        if (valueB !is GQLObjectValue) {
          return false
        }

        (valueA.fields + valueB.fields).groupBy { it.name }.values.forEach {
          if (it.size != 2) {
            return false
          }
          if (!areValuesEqual(it[0].value, it[1].value)) {
            return false
          }
        }
        true
      }
      is GQLVariableValue -> (valueB as? GQLVariableValue)?.name == valueA.name
    }
  }

  private fun sameResponseShapeRecursive(fieldWithParentA: FieldWithParent, fieldWithParentB: FieldWithParent): Boolean {
    if (!haveSameResponseShape(fieldWithParentA, fieldWithParentB)) {
      addFieldMergingIssue(fieldWithParentA.field, fieldWithParentB.field, "they have different shapes")
      return false
    }

    val parentTypeDefinitionA = fieldWithParentA.parentTypeDefinition
    val parentTypeDefinitionB = fieldWithParentB.parentTypeDefinition

    val setA = fieldWithParentA.field.selectionSet?.collectFields(parentTypeDefinitionA.name) ?: emptyList()
    val setB = fieldWithParentA.field.selectionSet?.collectFields(parentTypeDefinitionB.name) ?: emptyList()

    (setA + setB).groupBy { it.field.responseName() }.values.forEach { fieldsForName ->
      if (fieldsForName.pairs().firstOrNull { sameResponseShapeRecursive(it.first, it.second) } != null) {
        return false
      }
    }

    return true
  }

  // 5.3.2 2.1
  private fun haveSameResponseShape(fieldWithParentA: FieldWithParent, fieldWithParentB: FieldWithParent): Boolean {
    val fieldA = fieldWithParentA.field
    val fieldB = fieldWithParentB.field

    val parentTypeDefinitionA = fieldWithParentA.parentTypeDefinition
    val parentTypeDefinitionB = fieldWithParentB.parentTypeDefinition

    val fieldDefinitionA = fieldA.definitionFromScope(schema, parentTypeDefinitionA)
    val fieldDefinitionB = fieldB.definitionFromScope(schema, parentTypeDefinitionB)

    if (fieldDefinitionA == null || fieldDefinitionB == null) {
      // will be caught by other validation rules
      return true
    }

    var typeA = fieldDefinitionA.type
    var typeB = fieldDefinitionB.type

    while (typeA is GQLNonNullType || typeA is GQLListType || typeB is GQLNonNullType || typeB is GQLListType) {
      when {
        typeA is GQLNonNullType && typeB !is GQLNonNullType -> return false
        typeA !is GQLNonNullType && typeB is GQLNonNullType -> return false
        typeA is GQLNonNullType && typeB is GQLNonNullType -> {
          // both are non-null, unwrap
          typeA = typeA.type
          typeB = typeB.type
        }
      }
      when {
        typeA is GQLListType && typeB !is GQLListType -> return false
        typeA !is GQLListType && typeB is GQLListType -> return false
        typeA is GQLListType && typeB is GQLListType -> {
          // both are list, unwrap
          typeA = typeA.type
          typeB = typeB.type
        }
      }
    }

    check(typeA is GQLNamedType && typeB is GQLNamedType) {
      "${typeA.pretty()} and ${typeB.pretty()} should be GQLNamedType"
    }

    val typeDefinitionA = typeDefinitions[typeA.name]
    val typeDefinitionB = typeDefinitions[typeB.name]

    if (typeDefinitionA == null || typeDefinitionB == null) {
      // will be caught by other validation rules
      return true
    }

    if (typeDefinitionA is GQLScalarTypeDefinition || typeDefinitionA is GQLEnumTypeDefinition
        || typeDefinitionB is GQLScalarTypeDefinition || typeDefinitionB is GQLEnumTypeDefinition) {
      return typeDefinitionA.name == typeDefinitionB.name
    }

    return true
  }

  private fun <T> List<T>.pairs(): List<Pair<T, T>> {
    val pairs = mutableListOf<Pair<T, T>>()
    for (i in 0.until(size)) {
      for (j in (i + 1).until(size)) {
        pairs.add(get(i) to get(j))
      }
    }
    return pairs
  }

  private class FieldWithParent(val field: GQLField, val parentTypeDefinition: GQLTypeDefinition)

  private fun GQLSelectionSet.collectFields(parentType: String): List<FieldWithParent> {
    return selections.flatMap { selection ->
      when (selection) {
        is GQLField -> listOf(typeDefinitions[parentType]).mapNotNull { typeDefinition ->
          // typeDefinition should never be null here
          // if it is, we just skip this field and let other validation report the error
          typeDefinition?.let { FieldWithParent(selection, it) }
        }
        is GQLInlineFragment -> selection.collectFields()
        is GQLFragmentSpread -> selection.collectFields()
      }
    }
  }

  private fun GQLInlineFragment.collectFields() = selectionSet.collectFields(typeCondition.name)

  private fun GQLFragmentSpread.collectFields(): List<FieldWithParent> {
    val fragmentDefinition = fragmentDefinitions[name]
    if (fragmentDefinition == null) {
      // will be caught by other validation rules
      return emptyList()
    }

    return fragmentDefinition
        .selectionSet
        .collectFields(fragmentDefinition.typeCondition.name)
  }


  private fun List<GQLFragmentDefinition>.checkDuplicateFragments(): List<Issue> {
    val filtered = mutableMapOf<String, GQLFragmentDefinition>()
    forEach {
      val existing = filtered.putIfAbsent(it.name, it)
      if (existing != null) {
        issues.add(Issue.ValidationError(
            message = "Fragment ${it.name} is already defined",
            sourceLocation = it.sourceLocation,
        ))
      }
    }
    return issues
  }

  private fun List<GQLOperationDefinition>.checkDuplicateOperations(): List<Issue> {
    val filtered = mutableMapOf<String, GQLOperationDefinition>()

    forEach {
      if (it.name == null) {
        issues.add(Issue.ValidationError(
            message = "Apollo does not support anonymous operations",
            sourceLocation = it.sourceLocation,
        ))
        return@forEach
      }
      val existing = filtered.putIfAbsent(it.name, it)
      if (existing != null) {
        issues.add(Issue.ValidationError(
            message = "Operation ${it.name} is already defined",
            sourceLocation = it.sourceLocation,
        ))
      }
    }
    return issues
  }
}
