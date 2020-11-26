package com.apollographql.apollo.compiler.frontend.gql

/**
 * @param fragmentDefinitions: all the fragments in the current compilation unit. This is required to check the type conditions as well as fields merging
 */
private class ExecutableDocumentValidator(val schema: Schema, val fragmentDefinitions: Map<String, GQLFragmentDefinition>) {
  private val typeDefinitions = schema.typeDefinitions

  private val issues = mutableListOf<Issue>()

  fun validateDocument(document: GQLDocument): List<Issue> {
    document.validateExecutable()
    document.validateFragments()
    document.validateOperations()

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

  private fun GQLField.validate(operation: GQLOperationDefinition?, typeDefinitionInScope: GQLTypeDefinition) {
    val fieldDefinition = definitionFromScope(schema, typeDefinitionInScope)
    if (fieldDefinition == null) {
      issues.add(Issue.ValidationError(
          message = "Can't query `$name` on type `${typeDefinitionInScope.name}`",
          sourceLocation = sourceLocation
      ))
      return
    }

    if (fieldDefinition.isDeprecated()) {
      issues.add(Issue.DeprecatedUsage(message = "Use of deprecated field `$name`", sourceLocation = sourceLocation))
    }
    arguments?.validate(operation, fieldDefinition)

    val leafTypeDefinition = typeDefinitions[fieldDefinition.type.leafType().name]

    if (leafTypeDefinition == null) {
      issues.add(Issue.ValidationError(
          message = "Unknown type `${fieldDefinition.type.leafType().name}`",
          sourceLocation = sourceLocation
      ))
      return
    }

    if (leafTypeDefinition !is GQLScalarTypeDefinition
        && leafTypeDefinition !is GQLEnumTypeDefinition) {
      if (selectionSet == null) {
        issues.add(Issue.ValidationError(
            message = "Field `$name` of type `${fieldDefinition.type.pretty()}` must have a selection of sub-fields",
            sourceLocation = sourceLocation
        ))
        return
      }
      selectionSet.validate(operation, leafTypeDefinition)
    } else {
      if (selectionSet != null) {
        issues.add(Issue.ValidationError(
            message = "Field `$name` of type `${fieldDefinition.type.pretty()}` must not have a selection of sub-fields",
            sourceLocation = sourceLocation
        ))
        return
      }
    }
  }

  private fun GQLInlineFragment.validate(operation: GQLOperationDefinition?, typeDefinitionInScope: GQLTypeDefinition) {
    val inlineFragmentTypeDefinition = typeDefinitions[typeCondition.name]
    if (inlineFragmentTypeDefinition == null) {
      issues.add(Issue.ValidationError(
          message = "Cannot find type `${typeCondition.name}` for inline fragment",
          sourceLocation = typeCondition.sourceLocation
      ))
      return
    }

    if (!inlineFragmentTypeDefinition.sharesPossibleTypesWith(other = typeDefinitionInScope, typeDefinitions = typeDefinitions)) {
      issues.add(Issue.ValidationError(
          message = "Inline fragment cannot be spread here as result can never be of type `${typeCondition.name}`",
          sourceLocation = typeCondition.sourceLocation
      ))
      return
    }

    selectionSet.validate(operation, inlineFragmentTypeDefinition)
  }

  private fun GQLFragmentSpread.validate(operation: GQLOperationDefinition?, typeDefinitionInScope: GQLTypeDefinition) {
    val fragmentDefinition = fragmentDefinitions[name]
    if (fragmentDefinition == null) {
      issues.add(Issue.ValidationError(
          message = "Cannot find fragment `$name`",
          sourceLocation = sourceLocation
      ))
      return
    }

    val fragmentTypeDefinition = typeDefinitions[fragmentDefinition.typeCondition.name]
    if (fragmentTypeDefinition == null) {
      issues.add(Issue.ValidationError(
          message = "Cannot find type `${fragmentDefinition.typeCondition.name}` for fragment $name",
          sourceLocation = fragmentDefinition.typeCondition.sourceLocation
      ))
      return
    }

    if (!fragmentTypeDefinition.sharesPossibleTypesWith(other = typeDefinitionInScope, typeDefinitions = typeDefinitions)) {
      issues.add(Issue.ValidationError(
          message = "Fragment `$name` cannot be spread here as result can never be of type `${typeDefinitionInScope.name}`",
          sourceLocation = sourceLocation
      ))
      return
    }

    fragmentDefinition.selectionSet.validate(operation, fragmentTypeDefinition)
  }

  private fun GQLDocument.validateExecutable() {
    definitions.firstOrNull { it !is GQLOperationDefinition && it !is GQLFragmentDefinition }
        ?.let {
          issues.add(Issue.ValidationError(message = "Found an non-executable definition.", sourceLocation = it.sourceLocation))
          return
        }
  }

  private fun GQLDocument.validateOperations() {
    definitions.filterIsInstance<GQLOperationDefinition>().forEach {
      it.validate()
    }
  }

  private fun GQLDocument.validateFragments() {
    definitions.filterIsInstance<GQLFragmentDefinition>().forEach {
      it.validate()
    }
  }

  private fun GQLFragmentDefinition.validate() {
    val fragmentRootTypeDefinition = typeDefinitions[typeCondition.name]
    if (fragmentRootTypeDefinition == null) {
      issues.add(Issue.ValidationError(
          message = "Cannot find type `${typeCondition.name}` for fragment `$name`",
          sourceLocation = typeCondition.sourceLocation
      ))
      return
    }

    /**
     * Validate the fragment outside the context of an operation
     * This is still helpful to show warnings in the IDE while editing fragments of a parent module and the fragment may appear unused
     * This will not catch field merging conflicts and missing variables so ultimately, validation
     * against all fragments is required
     */
    selectionSet.validate(null, fragmentRootTypeDefinition)

    fieldsInSetCanMerge(selectionSet.collectFields(fragmentRootTypeDefinition.name))
  }

  private fun GQLOperationDefinition.validate() {
    val rootTypeDefinition = rootTypeDefinition(schema)

    if (rootTypeDefinition == null) {
      issues.add(Issue.ValidationError(
          message = "Cannot find a root type for operation type `$operationType`",
          sourceLocation = sourceLocation
      ))
      return
    }

    selectionSet.validate(this, rootTypeDefinition)

    fieldsInSetCanMerge(selectionSet.collectFields(rootTypeDefinition.name))
  }

  private fun GQLSelectionSet.validate(operation: GQLOperationDefinition?, typeDefinitionInScope: GQLTypeDefinition) {
    if (selections.isEmpty()) {
      // This will never happen from parsing documents but is kept for reference and to catch bad manual document modifications
      issues.add(Issue.ValidationError(
          message = "Selection of type `${typeDefinitionInScope.name}` must have a selection of sub-fields",
          sourceLocation = sourceLocation
      ))
      return
    }

    selections.forEach {
      when (it) {
        is GQLField -> it.validate(operation, typeDefinitionInScope)
        is GQLInlineFragment -> it.validate(operation, typeDefinitionInScope)
        is GQLFragmentSpread -> it.validate(operation, typeDefinitionInScope)
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
    issues.add(Issue.ValidationError(
        message = "`${fieldA.responseName()}` cannot be merged with `${fieldB.responseName()}`: $message",
        sourceLocation = fieldA.sourceLocation
    ))
    // Also add the symmetrical error
    issues.add(Issue.ValidationError(
        message = "`${fieldB.responseName()}` cannot be merged with `${fieldA.responseName()}`: $message",
        sourceLocation = fieldB.sourceLocation
    ))
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

  private fun GQLArgument.validate(operation: GQLOperationDefinition?, fieldDefinition: GQLFieldDefinition) {
    val schemaArgument = fieldDefinition.arguments.firstOrNull { it.name == name }
    if (schemaArgument == null) {
      issues.add(Issue.ValidationError(message = "Unknown argument `$name` on field `${fieldDefinition.name}`", sourceLocation = sourceLocation))
      return
    }

    // 5.6.2 Input Object Field Names
    issues.addAll(value.validateAndCoerce(schemaArgument.type, schema, operation).issues)
  }

  private fun GQLArguments.validate(operation: GQLOperationDefinition?, field: GQLFieldDefinition) {
    // 5.4.2 Argument Uniqueness
    arguments.groupBy { it.name }.filter { it.value.size > 1 }.toList().firstOrNull()?.let {
      issues.add(Issue.ValidationError(message = "Argument `${it.first}` is defined multiple times", sourceLocation = it.second.first().sourceLocation))
      return
    }

    // 5.4.2.1 Required arguments
    field.arguments.forEach { inputValueDefinition ->
      if (inputValueDefinition.type is GQLNonNullType && inputValueDefinition.defaultValue == null) {
        val argumentValue = arguments.firstOrNull { it.name == inputValueDefinition.name }?.value
        if (argumentValue is GQLNullValue) {
          // This will be caught later when validating individual arguments
          // issues.add(Issue.ValidationError(message = "Cannot pass `null` for a required argument", sourceLocation = argumentValue.sourceLocation))
        } else if (argumentValue == null) {
          issues.add(Issue.ValidationError(message = "No value passed for required argument ${inputValueDefinition.name}", sourceLocation = sourceLocation))
        }
      }
    }

    arguments.forEach {
      it.validate(operation, field)
    }
  }

}

private fun GQLFieldDefinition.isDeprecated(): Boolean {
  return directives.firstOrNull { it.name == "deprecated" } != null
}

private fun GQLEnumValueDefinition.isDeprecated(): Boolean {
  return directives.firstOrNull { it.name == "deprecated" } != null
}

fun GQLValue.validateAndCoerce(expectedType: GQLType, schema: Schema, operation: GQLOperationDefinition?) = InputValueValidationScope(schema).validateAndCoerce(operation, this, expectedType)

private class InputValueValidationScope(val schema: Schema) {
  val issues = mutableListOf<Issue>()

  fun registerIssue(value: GQLValue, expectedType: GQLType) {
    issues.add(Issue.ValidationError(message = "Value `${value.toUtf8()}` cannot be used in position expecting `${expectedType.pretty()}`", sourceLocation = value.sourceLocation))
  }

  private fun validateAndCoerceInputObject(operation: GQLOperationDefinition?, value: GQLValue, expectedTypeDefinition: GQLInputObjectTypeDefinition): GQLValue {
    val expectedType = GQLNamedType(name = expectedTypeDefinition.name)
    if (value !is GQLObjectValue) {
      registerIssue(value, expectedType)
      return value
    }

    // 3.10 All required input fields must have a value
    expectedTypeDefinition.inputFields.forEach { inputValueDefinition ->
      if (inputValueDefinition.type is GQLNonNullType
          && inputValueDefinition.defaultValue == null
          && value.fields.firstOrNull { it.name == inputValueDefinition.name } == null
      ) {
        issues.add(Issue.ValidationError(message = "No value passed for required inputField ${inputValueDefinition.name}", sourceLocation = value.sourceLocation))
      }
    }

    return GQLObjectValue(fields = value.fields.mapNotNull { field ->
      val inputField = expectedTypeDefinition.inputFields.firstOrNull { it.name == field.name }
      if (inputField == null) {
        // 3.10 Input values coercion: extra values are errors
        issues.add(Issue.ValidationError(message = "Field ${field.name} is not defined by ${expectedType.pretty()}", sourceLocation = field.sourceLocation))
        return@mapNotNull null
      }
      GQLObjectField(
          name = field.name,
          value = validateAndCoerceInternal(operation, field.value, inputField.type)
      )
    })
  }

  private fun validateAndCoerceEnum(value: GQLValue, enumTypeDefinition: GQLEnumTypeDefinition): GQLValue {
    val expectedType = GQLNamedType(name = enumTypeDefinition.name)
    if (value !is GQLEnumValue) {
      registerIssue(value, expectedType)
      return value
    }

    val enumValue = enumTypeDefinition.enumValues.firstOrNull { it.name == value.value }
    if (enumValue == null) {
      issues.add(Issue.ValidationError(
          message = "Cannot find enum value `${value.value}` of type `${enumTypeDefinition.name}`",
          sourceLocation = value.sourceLocation
      ))
    } else if (enumValue.isDeprecated()) {
      issues.add(Issue.DeprecatedUsage(
          message = "Use of deprecated enum value `${value.value}` of type `${enumTypeDefinition.name}`",
          sourceLocation = value.sourceLocation
      ))
    }
    return value
  }

  private fun validateAndCoerceScalar(value: GQLValue, expectedType: GQLNamedType): GQLValue {
    return when (expectedType.name) {
      "Int" -> {
        if (value !is GQLIntValue) {
          registerIssue(value, expectedType)
        }
        value
      }
      "Float" -> {
        when (value) {
          is GQLFloatValue -> value
          // Int get coerced to floats
          is GQLIntValue -> GQLFloatValue(value = value.value.toDouble())
          else -> {
            registerIssue(value, expectedType)
            value
          }
        }
      }
      "String" -> {
        if (value !is GQLStringValue) {
          registerIssue(value, expectedType)
        }
        value
      }
      "Boolean" -> {
        if (value !is GQLBooleanValue) {
          registerIssue(value, expectedType)
        }
        value
      }
      "ID" -> {
        // 3.5.5 ID can be either string or int
        if (value !is GQLStringValue && value !is GQLIntValue) {
          registerIssue(value, expectedType)
        }
        value
      }
      else -> {
        registerIssue(value, expectedType)
        value
      }
    }
  }

  fun validateAndCoerce(operation: GQLOperationDefinition?, value: GQLValue, expectedType: GQLType) = ParseResult(
      validateAndCoerceInternal(operation, value, expectedType),
      issues
  )

  private fun validateAndCoerceInternal(operation: GQLOperationDefinition?, value: GQLValue, expectedType: GQLType): GQLValue {
    if (value is GQLVariableValue) {
      return validateAndCoerceVariable(operation, value, expectedType)
    }
    when (expectedType) {
      is GQLNonNullType -> {
        if (value is GQLNullValue) {
          registerIssue(value, expectedType)
          return value
        }
        return validateAndCoerceInternal(operation, value, expectedType.type)
      }
      is GQLListType -> {
        if (value !is GQLListValue) {
          registerIssue(value, expectedType)
          return value
        }
        return GQLListValue(
            values = value.values.map { validateAndCoerceInternal(operation, it, expectedType.type) }
        )
      }
      is GQLNamedType -> {
        when (val expectedTypeDefinition = schema.typeDefinition(expectedType.name)) {
          is GQLInputObjectTypeDefinition -> {
            return validateAndCoerceInputObject(operation, value, expectedTypeDefinition)
          }
          is GQLScalarTypeDefinition -> {
            if (!expectedTypeDefinition.isBuiltIn()) {
              // custom scalar types are passed through
              return value
            }
            return validateAndCoerceScalar(value, expectedType)
          }
          is GQLEnumTypeDefinition -> {
            return validateAndCoerceEnum(value, expectedTypeDefinition)
          }
          else -> {
            issues.add(Issue.ValidationError("Value cannot be of non-input type ${expectedType.pretty()}", value.sourceLocation))
            return value
          }
        }
      }
    }
  }

  private fun validateAndCoerceVariable(operation: GQLOperationDefinition?, value: GQLVariableValue, expectedType: GQLType): GQLValue {
    if (operation == null) {
      // if operation is null, it means we're currently validating a fragment outside the context of an operation
      return value
    }
    val variableDefinition = operation.variableDefinitions.firstOrNull { it.name == value.name }
    if (variableDefinition == null) {
      issues.add(Issue.ValidationError(
          message = "Variable `${value.name}` is not defined by operation `${operation.name}`",
          sourceLocation = value.sourceLocation
      ))
      return value
    }
    if (!variableDefinition.type.canInputValueBeAssignedTo(target = expectedType)) {
      issues.add(Issue.ValidationError(
          message = "Variable `${value.name}` of type `${variableDefinition.type.pretty()}` used in position expecting type `${expectedType.pretty()}`",
          sourceLocation = value.sourceLocation
      ))
    }
    return value
  }
}

private fun GQLDocument.checkSingleOperation(): List<Issue> {
  val operations = definitions.filterIsInstance<GQLOperationDefinition>()
  return when {
    operations.isEmpty() -> listOf(Issue.ValidationError("No operation found", sourceLocation))
    else -> emptyList()
  }
}

internal fun List<GQLFragmentDefinition>.checkDuplicateFragments(): List<Issue> {
  val filtered = mutableMapOf<String, GQLFragmentDefinition>()
  val issues = mutableListOf<Issue>()

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

internal fun List<GQLOperationDefinition>.checkDuplicateOperations(): List<Issue> {
  val filtered = mutableMapOf<String, GQLOperationDefinition>()
  val issues = mutableListOf<Issue>()

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

internal fun List<GQLDefinition>.checkDuplicates(): List<Issue> {
  return filterIsInstance<GQLOperationDefinition>().checkDuplicateOperations() + filterIsInstance<GQLFragmentDefinition>().checkDuplicateFragments()
}

fun GQLOperationDefinition.validate(schema: Schema, fragments: Map<String, GQLFragmentDefinition>) = ExecutableDocumentValidator(schema, fragments).validateOperation(this)

fun GQLFragmentDefinition.validate(schema: Schema, fragments: Map<String, GQLFragmentDefinition>) = ExecutableDocumentValidator(schema, fragments).validateFragment(this)

fun GQLDocument.validateAsOperations(schema: Schema): List<Issue> {
  val fragments = definitions.filterIsInstance<GQLFragmentDefinition>().associateBy { it.name }
  val validationIssues = ExecutableDocumentValidator(schema, fragments).validateDocument(this)
  val duplicateIssues = definitions.checkDuplicates()

  val singleOperationIssues = checkSingleOperation()
  // check for unused fragments
  return validationIssues + duplicateIssues + singleOperationIssues
}
