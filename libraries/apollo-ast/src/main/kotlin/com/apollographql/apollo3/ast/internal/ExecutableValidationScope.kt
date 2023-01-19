package com.apollographql.apollo3.ast.internal

import com.apollographql.apollo3.ast.GQLArgument
import com.apollographql.apollo3.ast.GQLBooleanValue
import com.apollographql.apollo3.ast.GQLDirective
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
import com.apollographql.apollo3.ast.GQLNode
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
import com.apollographql.apollo3.ast.InferredVariable
import com.apollographql.apollo3.ast.Issue
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.SourceLocation
import com.apollographql.apollo3.ast.VariableUsage
import com.apollographql.apollo3.ast.definitionFromScope
import com.apollographql.apollo3.ast.findDeprecationReason
import com.apollographql.apollo3.ast.rawType
import com.apollographql.apollo3.ast.pretty
import com.apollographql.apollo3.ast.responseName
import com.apollographql.apollo3.ast.rootTypeDefinition
import com.apollographql.apollo3.ast.sharesPossibleTypesWith

/**
 * @param fragmentDefinitions: all the fragments in the current compilation unit.
 * This is required to check the type conditions as well as fields merging
 *
 * @param fieldsOnDisjointTypesMustMerge set to false to relax the standard GraphQL [FieldsInSetCanMerge](https://spec.graphql.org/draft/#FieldsInSetCanMerge())
 * and allow fields of different types at the same Json path as long as their parent types are disjoint.
 */
internal class ExecutableValidationScope(
    private val schema: Schema,
    private val fragmentDefinitions: Map<String, GQLFragmentDefinition>,
    private val fieldsOnDisjointTypesMustMerge: Boolean,
) : ValidationScope {
  override val typeDefinitions = schema.typeDefinitions
  override val directiveDefinitions = schema.directiveDefinitions

  override val foreignNames: Map<String, String>
    get() = schema.foreignNames

  override val issues = mutableListOf<Issue>()

  /**
   * As the tree is walked, variable references will be put here
   */
  private val variableUsages = mutableListOf<VariableUsage>()

  private val deferDirectiveLabels = mutableMapOf<String, SourceLocation>()
  private val deferDirectivePathAndLabels = mutableMapOf<String, SourceLocation>()

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

  /**
   * Infer variables from a fragment definition. If a variable is used in both a nullable and non-nullable
   * position, the variable is inferred as non-nullable
   *
   * @throws IllegalStateException if some incompatibles types are found. This should never happen
   * because this should be caught during previous operation-wide validation.
   */
  fun inferFragmentVariables(fragment: GQLFragmentDefinition): List<InferredVariable> {
    variableUsages.clear()
    fragment.validate()

    return variableUsages.groupBy {
      it.variable.name
    }.entries.mapNotNull {
      val types = it.value.map { it.locationType }
      val inferredType = types.findCompatibleType()
      if (inferredType == null) {
        error("Fragment ${fragment.name} uses different types for variable '${it.key}': ${types.joinToString()}")
      } else {
        InferredVariable(it.key, inferredType)
      }
    }
  }

  private fun List<GQLType>.findCompatibleType(): GQLType? {
    return drop(1).fold<GQLType, GQLType?>(first()) { acc, gqlType ->
      if (acc == null) {
        return@fold null
      }

      acc.mergeWith(gqlType)
    }
  }

  @Suppress("KotlinConstantConditions")
  private fun GQLType.mergeWith(other: GQLType): GQLType? {
    return if (this is GQLNonNullType && other is GQLNonNullType) {
      type.mergeWith(other.type)?.let { GQLNonNullType(SourceLocation.UNKNOWN, it) }
    } else if (this is GQLNonNullType && other !is GQLNonNullType) {
      type.mergeWith(other)?.let { GQLNonNullType(SourceLocation.UNKNOWN, it) }
    } else if (this !is GQLNonNullType && other is GQLNonNullType) {
      this.mergeWith(other.type)?.let { GQLNonNullType(SourceLocation.UNKNOWN, it) }
    } else if (this is GQLListType && other is GQLListType) {
      this.type.mergeWith(other.type)?.let { GQLListType(SourceLocation.UNKNOWN, it) }
    } else if (this is GQLListType && other !is GQLListType) {
      null
    } else if (this !is GQLListType && other is GQLListType) {
      null
    } else if (this is GQLNamedType && other is GQLNamedType){
      if (name != other.name) {
        null
      } else {
        this
      }
    } else {
      throw IllegalStateException()
    }
  }

  private fun GQLField.validate(parentTypeDefinition: GQLTypeDefinition, path: String) {
    val fieldDefinition = definitionFromScope(schema, parentTypeDefinition)
    if (fieldDefinition == null) {
      registerIssue(
          message = "Can't query `$name` on type `${parentTypeDefinition.name}`",
          sourceLocation = sourceLocation
      )
      return
    }

    if (fieldDefinition.directives.findDeprecationReason() != null) {
      issues.add(Issue.DeprecatedUsage(message = "Use of deprecated field `$name`", sourceLocation = sourceLocation))
    }

    validateArguments(
        arguments?.arguments ?: emptyList(),
        sourceLocation,
        fieldDefinition.arguments,
        "field `${fieldDefinition.name}`"
    ) {
      variableUsages.add(it)
    }

    val typeDefinition = typeDefinitions[fieldDefinition.type.rawType().name]

    if (typeDefinition == null) {
      registerIssue(
          message = "Unknown type `${fieldDefinition.type.rawType().name}`",
          sourceLocation = sourceLocation
      )
      return
    }

    if (typeDefinition !is GQLScalarTypeDefinition
        && typeDefinition !is GQLEnumTypeDefinition) {
      if (selectionSet == null) {
        registerIssue(
            message = "Field `$name` of type `${fieldDefinition.type.pretty()}` must have a selection of sub-fields",
            sourceLocation = sourceLocation
        )
        return
      }
      val fieldPath = if (path.isEmpty()) name else "$path.$name"
      selectionSet.validate(typeDefinition, this@validate, fieldPath)
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
      validateDirective(it, this) {
        variableUsages.add(it)
      }
    }
  }


  private fun GQLInlineFragment.validate(parentTypeDefinition: GQLTypeDefinition, selectionSetParent: GQLNode, path: String) {
    val inlineFragmentTypeDefinition = typeDefinitions[typeCondition.name]
    if (inlineFragmentTypeDefinition == null) {
      registerIssue(
          message = "Cannot find type `${typeCondition.name}` for inline fragment",
          sourceLocation = typeCondition.sourceLocation
      )
      return
    }

    if (!inlineFragmentTypeDefinition.sharesPossibleTypesWith(other = parentTypeDefinition, schema = schema)) {
      registerIssue(
          message = "Inline fragment cannot be spread here as result can never be of type `${typeCondition.name}`",
          sourceLocation = typeCondition.sourceLocation
      )
      return
    }

    selectionSet.validate(inlineFragmentTypeDefinition, this@validate, path)

    directives.forEach {
      validateDirective(it, this) {
        variableUsages.add(it)
      }
      if (it.name == "defer" && !path.startsWith('-')) it.validateDeferDirective(selectionSetParent, path)
    }
  }

  private fun GQLFragmentSpread.validate(parentTypeDefinition: GQLTypeDefinition, selectionSetParent: GQLNode, path: String) {
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

    if (!fragmentTypeDefinition.sharesPossibleTypesWith(other = parentTypeDefinition, schema = schema)) {
      registerIssue(
          message = "Fragment `$name` cannot be spread here as result can never be of type `${parentTypeDefinition.name}`",
          sourceLocation = sourceLocation
      )
      return
    }

    fragmentDefinition.selectionSet.validate(fragmentTypeDefinition, this@validate, path)

    directives.forEach {
      validateDirective(it, this) {
        variableUsages.add(it)
      }
      if (it.name == "defer" && !path.startsWith('-')) it.validateDeferDirective(selectionSetParent, path)
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
     * against all fragments is required.
     * Use "-" for the path as a signal to skip @defer specific validation, which is only relevant when considering the fragment in the
     * context of an operation.
     */
    selectionSet.validate(fragmentRootTypeDefinition, this, path = "-")

    fieldsInSetCanMerge(selectionSet.collectFields(fragmentRootTypeDefinition.name))
  }

  private fun GQLOperationDefinition.validate() {
    variableUsages.clear()
    deferDirectiveLabels.clear()
    deferDirectivePathAndLabels.clear()

    val rootTypeDefinition = rootTypeDefinition(schema)

    if (rootTypeDefinition == null) {
      registerIssue(
          message = "Cannot find a root type for operation type `$operationType`",
          sourceLocation = sourceLocation
      )
      return
    }

    selectionSet.validate(rootTypeDefinition, this)

    fieldsInSetCanMerge(selectionSet.collectFields(rootTypeDefinition.name))

    variableUsages.forEach {
      validateVariable(this, it)
    }
    val foundVariables = variableUsages.map { it.variable.name }.toSet()
    variableDefinitions.forEach {
      if (!foundVariables.contains(it.name)) {
        issues.add(Issue.UnusedVariable(
            message = "Variable `${it.name}` is unused",
            sourceLocation = it.sourceLocation
        ))
      }
    }
  }

  /**
   * - If a label is passed to a `@defer` directive, it must not be a variable, and it must be unique within all other `@defer` directives in
   * the document.
   * - The @defer directive is not allowed to be used on root fields of the mutation or subscription type.
   * - Check that the label can be used as part of an identifier name (Apollo-specific validation).
   * - Check that any `@defer` directive found when walking fragments on an operation have a unique path + label (Apollo-specific
   * validation). For instance: this is invalid:
   * ```
   * query Query1 {
   *   computers {
   *   ...ComputerFields @defer  # path is computer
   *   ...ComputerFields @defer  # path is computer
   *   }
   * }
   * ```
   *
   * Also invalid:
   * ```
   * query Query2 {
   *   computers {
   *     ...ComputerFields
   *     ...ComputerFields2
   *     }
   *   }
   *
   *   fragment ComputerFields on Computer {
   *     screen {
   *       ...ScreenFields @defer  # path is computer.screen
   *     }
   *   }
   *
   *   fragment ComputerFields2 on Computer {
   *     screen {
   *       ...ScreenFields @defer  # path is computer.screen
   *     }
   * }
   * ```
   */
  private fun GQLDirective.validateDeferDirective(selectionSetParent: GQLNode, path: String) {
    val label = arguments?.arguments?.firstOrNull { it.name == "label" }?.value
    if (label is GQLVariableValue) {
      registerIssue(
          message = "@defer label argument must not be a variable",
          sourceLocation = sourceLocation
      )
      return
    }

    if (selectionSetParent is GQLOperationDefinition && (selectionSetParent.operationType == "mutation" || selectionSetParent.operationType == "subscription")) {
      registerIssue(
          message = "The @defer directive is not allowed to be used on root fields of mutations or subscriptions",
          sourceLocation = sourceLocation
      )
    }

    var labelStringValue = ""
    if (label != null) {
      // If label is not a GQLStringValue, prior validation will have issued an error already, so we can ignore this one
      if (label !is GQLStringValue) return
      labelStringValue = label.value

      // We use the label in part of the synthetic field's name in the generated model, so it needs to be a valid Kotlin/Java identifier
      if (!labelStringValue.matches(Regex("[a-zA-Z0-9_]+"))) {
        registerIssue(
            message = "@defer label '$labelStringValue' must only contain letters, numbers, or underscores",
            sourceLocation = sourceLocation
        )
      }

      if (labelStringValue in deferDirectiveLabels) {
        registerIssue(
            message = "@defer label '$labelStringValue' must be unique within all other @defer directives in the document. " +
                "Same label found in ${deferDirectiveLabels[labelStringValue]!!.pretty()}",
            sourceLocation = sourceLocation
        )
        return
      }
      deferDirectiveLabels[labelStringValue] = sourceLocation
    }

    val pathAndLabel = "$path/$labelStringValue"
    if (pathAndLabel in deferDirectivePathAndLabels) {
      val labelMessage = if (labelStringValue.isEmpty()) "no label" else "label '$labelStringValue'"
      registerIssue(
          message = "A @defer directive with the same path '$path' and $labelMessage is already defined in ${deferDirectivePathAndLabels[pathAndLabel]!!.pretty()}. " +
              "Set a unique label to distinguish them.",
          sourceLocation = sourceLocation
      )
      return
    }
    deferDirectivePathAndLabels[pathAndLabel] = sourceLocation
  }

  private fun GQLSelectionSet.validate(parentTypeDefinition: GQLTypeDefinition, selectionSetParent: GQLNode, path: String = "") {
    if (selections.isEmpty()) {
      // This will never happen from parsing documents but is kept for reference and to catch bad manual document modifications
      registerIssue(
          message = "Selection of type `${parentTypeDefinition.name}` must have a selection of sub-fields",
          sourceLocation = sourceLocation
      )
      return
    }

    selections.forEach {
      when (it) {
        is GQLField -> it.validate(parentTypeDefinition, path)
        is GQLInlineFragment -> it.validate(parentTypeDefinition, selectionSetParent, path)
        is GQLFragmentSpread -> it.validate(parentTypeDefinition, selectionSetParent, path)
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

    val setA = fieldA.selectionSet?.collectFields(typeA.rawType().name) ?: emptyList()
    val setB = fieldB.selectionSet?.collectFields(typeB.rawType().name) ?: emptyList()

    fieldsInSetCanMerge(setA + setB)
  }

  private fun fieldsInSetCanMerge(fieldsWithParent: List<FieldWithParent>) {
    fieldsWithParent.groupBy { it.field.responseName() }
        .values
        .forEach { fieldsForName ->
          if (fieldsForName.size == 1) {
            val first = fieldsForName.first()
            val fieldDefinition = first.field.definitionFromScope(schema, first.parentTypeDefinition.name)
            if (fieldDefinition == null) {
              // This field is unknown. Let other validation rules catch this
              return@forEach
            }
            val set = first.field.selectionSet?.collectFields(fieldDefinition.type.rawType().name) ?: emptyList()
            // recurse in subfields
            fieldsInSetCanMerge(set)
          } else {
            fieldsForName.pairs().forEach {
              fieldPairCanMerge(it.first, it.second)
            }
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

  private fun buildMessage(fieldA: GQLField, fieldB: GQLField, message: String): String {
    return "`${fieldA.responseName()}` cannot be merged with `${fieldB.responseName()}` (at ${fieldB.sourceLocation.pretty()}): " +
        "$message. Use different aliases on the fields to fetch both if this was intentional."
  }

  private fun addFieldMergingIssue(fieldA: GQLField, fieldB: GQLField, message: String) {
    registerIssue(
        message = buildMessage(fieldA, fieldB, message),
        sourceLocation = fieldA.sourceLocation
    )
    // Also add the symmetrical error
    registerIssue(
        message = buildMessage(fieldB, fieldA, message),
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
    val setB = fieldWithParentB.field.selectionSet?.collectFields(parentTypeDefinitionB.name) ?: emptyList()

    (setA + setB).groupBy { it.field.responseName() }.values.forEach { fieldsForName ->
      if (fieldsForName.pairs().firstOrNull { sameResponseShapeRecursive(it.first, it.second) } != null) {
        return false
      }
    }

    return true
  }

  // 5.3.2 2.1
  private fun haveSameResponseShape(fieldWithParentA: FieldWithParent, fieldWithParentB: FieldWithParent): Boolean {
    if (!fieldsOnDisjointTypesMustMerge) {
      return true
    }
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
