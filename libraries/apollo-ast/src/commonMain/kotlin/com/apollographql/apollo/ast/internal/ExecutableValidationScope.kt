package com.apollographql.apollo.ast.internal

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.ast.AnonymousOperation
import com.apollographql.apollo.ast.DeprecatedUsage
import com.apollographql.apollo.ast.ExecutableValidationResult
import com.apollographql.apollo.ast.FragmentCycle
import com.apollographql.apollo.ast.GQLBooleanValue
import com.apollographql.apollo.ast.GQLDirective
import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.GQLEnumTypeDefinition
import com.apollographql.apollo.ast.GQLEnumValue
import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFieldDefinition
import com.apollographql.apollo.ast.GQLFloatValue
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLFragmentSpread
import com.apollographql.apollo.ast.GQLInlineFragment
import com.apollographql.apollo.ast.GQLInputValueDefinition
import com.apollographql.apollo.ast.GQLIntValue
import com.apollographql.apollo.ast.GQLListType
import com.apollographql.apollo.ast.GQLListValue
import com.apollographql.apollo.ast.GQLNode
import com.apollographql.apollo.ast.GQLNonNullType
import com.apollographql.apollo.ast.GQLNullValue
import com.apollographql.apollo.ast.GQLObjectValue
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.GQLScalarTypeDefinition
import com.apollographql.apollo.ast.GQLSelection
import com.apollographql.apollo.ast.GQLStringValue
import com.apollographql.apollo.ast.GQLType
import com.apollographql.apollo.ast.GQLTypeDefinition
import com.apollographql.apollo.ast.GQLValue
import com.apollographql.apollo.ast.GQLVariableDefinition
import com.apollographql.apollo.ast.GQLVariableValue
import com.apollographql.apollo.ast.Issue
import com.apollographql.apollo.ast.OtherValidationIssue
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.UnusedFragment
import com.apollographql.apollo.ast.UnusedVariable
import com.apollographql.apollo.ast.VariableUsage
import com.apollographql.apollo.ast.definitionFromScope
import com.apollographql.apollo.ast.findCatch
import com.apollographql.apollo.ast.findDeprecationReason
import com.apollographql.apollo.ast.getArgumentValueOrDefault
import com.apollographql.apollo.ast.internal.pretty
import com.apollographql.apollo.ast.internal.validation.validateDeferLabels
import com.apollographql.apollo.ast.isInputType
import com.apollographql.apollo.ast.pretty
import com.apollographql.apollo.ast.rawType
import com.apollographql.apollo.ast.responseName
import com.apollographql.apollo.ast.rootTypeDefinition
import com.apollographql.apollo.ast.sharesPossibleTypesWith


@OptIn(ApolloInternal::class, ApolloExperimental::class)
internal class ExecutableValidationScope(
    private val schema: Schema,
) : ValidationScope {
  override val typeDefinitions = schema.typeDefinitions
  override val directiveDefinitions = schema.directiveDefinitions

  override val foreignNames: Map<String, String>
    get() = schema.foreignNames

  /**
   * These are scoped to the current document being validated
   */
  override val issues = mutableListOf<Issue>()
  private val fragmentDefinitions = mutableMapOf<String, GQLFragmentDefinition>()

  /**
   * Contains the direct variable usages in the given fragment definition that are not defined by the fragment itself.
   */
  private val fragmentOperationVariableUsages = mutableMapOf<String, List<VariableUsage>>()
  private val cyclicFragments = mutableSetOf<String>()
  private val usedFragments = mutableSetOf<String>()
  private val hasCatchByDefault = schema.directiveDefinitions.any { schema.originalDirectiveName(it.key) == Schema.CATCH_BY_DEFAULT }

  /**
   * These are scoped to the current operation/fragment being validated
   */
  private val variableUsages = mutableListOf<VariableUsage>()

  private fun GQLFragmentDefinition.detectCycles(
      globallyVisitedFragments: MutableSet<String>,
      locallyVisitedFragments: MutableSet<String>,
      path: List<String>,
  ) {
    if (globallyVisitedFragments.contains(name)) {
      return
    }
    locallyVisitedFragments.add(name)

    if (typeDefinitions[typeCondition.name] == null) {
      return
    }

    selections.detectCycles(globallyVisitedFragments, locallyVisitedFragments, path)
  }

  private fun List<GQLSelection>.detectCycles(
      globallyVisitedFragments: MutableSet<String>,
      locallyVisitedFragments: MutableSet<String>,
      path: List<String>,
  ) {
    forEach {
      when (it) {
        is GQLField -> {
          it.selections.detectCycles(globallyVisitedFragments, locallyVisitedFragments, path + it.responseName())
        }

        is GQLInlineFragment -> {
          it.selections.detectCycles(globallyVisitedFragments, locallyVisitedFragments, path + "__on${it.typeCondition?.name ?: ""}")
        }

        is GQLFragmentSpread -> {
          val fragment = fragmentDefinitions.get(it.name)
          if (fragment != null) {
            val name = fragment.name
            val index = path.indexOf("__${name}")
            val nextPath = path + "__${fragment.name}"
            if (index != -1) {
              issues.add(FragmentCycle("Fragment '$name' spreads itself, creating a cycle at '${nextPath.subList(index, nextPath.size).joinToString(".")}'", it.sourceLocation))
              cyclicFragments.add(name)
              return@forEach
            }

            fragment.detectCycles(globallyVisitedFragments, locallyVisitedFragments, nextPath)
          }
        }
      }
    }
  }

  fun validate(document: GQLDocument): ExecutableValidationResult {
    document.validateExecutable()

    document.definitions.filterIsInstance<GQLFragmentDefinition>().forEach {
      fragmentDefinitions[it.name] = it
    }

    val globallyVisitedFragments = mutableSetOf<String>()

    fragmentDefinitions.forEach {
      val locallyVisitedFragments = mutableSetOf<String>()
      it.value.detectCycles(globallyVisitedFragments, locallyVisitedFragments, listOf("__${it.value.name}"))
      globallyVisitedFragments.addAll(locallyVisitedFragments)
    }

    val fragments = document.definitions.filterIsInstance<GQLFragmentDefinition>()
    fragments.checkDuplicateFragments()
    fragments.forEach {
      // Needs to happen before validating the operations to set `fragmentVariableUsages`
      it.validate()
    }

    val operations = document.definitions.filterIsInstance<GQLOperationDefinition>()
    operations.checkDuplicateOperations()
    operations.forEach {
      it.validate()
    }

    (this.fragmentDefinitions.keys - usedFragments).forEach {
      issues.add(
          UnusedFragment(
              message = "Fragment '$it' is not used",
              sourceLocation = fragmentDefinitions[it]?.sourceLocation,
          )
      )
    }

    val fragmentVariables = fragments.associate {
      it.name to (setOf(it.name) + it.selections.collectFragmentSpreads()).fold(emptyList<VariableUsage>()) { acc, item ->
        acc + fragmentOperationVariableUsages.get(item).orEmpty()
      }
    }
    return ExecutableValidationResult(fragmentVariables, issues)
  }


  private fun GQLField.validate(parentTypeDefinition: GQLTypeDefinition) {
    val fieldDefinition = definitionFromScope(schema, parentTypeDefinition)
    if (fieldDefinition == null) {
      registerIssue(
          message = "Can't query `$name` on type `${parentTypeDefinition.name}`",
          sourceLocation = sourceLocation
      )
      return
    }

    if (fieldDefinition.directives.findDeprecationReason() != null) {
      issues.add(DeprecatedUsage(message = "Use of deprecated field `$name`", sourceLocation = sourceLocation))
    }

    validateArguments(
        arguments,
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
        && typeDefinition !is GQLEnumTypeDefinition
    ) {
      if (selections.isEmpty()) {
        registerIssue(
            message = "Field `$name` of type `${fieldDefinition.type.pretty()}` must have a selection of sub-fields",
            sourceLocation = sourceLocation
        )
        return
      }
      selections.validate(typeDefinition)
    } else {
      if (selections.isNotEmpty()) {
        registerIssue(
            message = "Field `$name` of type `${fieldDefinition.type.pretty()}` must not have a selection of sub-fields",
            sourceLocation = sourceLocation
        )
        return
      }
    }

    validateDirectives(directives, this) {
      variableUsages.add(it)
    }
    if (hasCatchByDefault) {
      validateCatches(fieldDefinition)
    }
  }

  private fun GQLType.maxDimension(): Int {
    var dimension = 0
    var type = this
    while (true) {
      when (type) {
        is GQLListType -> {
          dimension++
          type = type.type
        }

        is GQLNonNullType -> {
          type = type.type
        }

        else -> return dimension
      }
    }
  }

  private fun GQLField.validateCatches(fieldDefinition: GQLFieldDefinition) {
    val catches = directives.filter {
      schema.originalDirectiveName(it.name) == Schema.CATCH
    }

    if (catches.size > 1) {
      // "caught" (ahah) by other validation rules
      return
    }

    val catch = catches.singleOrNull()
    if (catch == null) {
      return
    }

    val levels = catch.getArgumentValueOrDefault("levels", schema)
    val maxDimension = fieldDefinition.type.maxDimension()
    if (levels is GQLListValue) {
      levels.values.forEach {
        if (it is GQLIntValue) {
          val asInt = it.value.toIntOrNull()
          if (asInt == null) {
            registerIssue("Invalid value: '${it.value}'", it.sourceLocation)
            return@forEach
          }
          if (asInt > maxDimension) {
            registerIssue(
                message = "Invalid 'levels' value '$asInt' for `@catch` usage: this field has a max list level of $maxDimension",
                sourceLocation = it.sourceLocation
            )
          } else if (asInt < 0) {
            registerIssue(
                message = "'levels' values must be positive ints",
                sourceLocation = it.sourceLocation
            )
          }
        }
      }
    }
  }

  private fun GQLInlineFragment.validate(parentTypeDefinition: GQLTypeDefinition) {
    val tc = typeCondition?.name ?: parentTypeDefinition.name
    val inlineFragmentTypeDefinition = typeDefinitions[tc]
    if (inlineFragmentTypeDefinition == null) {
      registerIssue(
          message = "Cannot find type `${tc}` for inline fragment",
          sourceLocation = typeCondition?.sourceLocation ?: sourceLocation
      )
      return
    }

    if (!inlineFragmentTypeDefinition.sharesPossibleTypesWith(other = parentTypeDefinition, schema = schema)) {
      registerIssue(
          message = "Inline fragment cannot be spread here as result can never be of type `${tc}`",
          sourceLocation = typeCondition?.sourceLocation ?: sourceLocation
      )
      return
    }

    selections.validate(inlineFragmentTypeDefinition)

    validateDirectives(directives, this) {
      variableUsages.add(it)
    }
  }

  private fun GQLFragmentSpread.validate(parentTypeDefinition: GQLTypeDefinition) {
    usedFragments.add(name)

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
      return
    }

    if (!fragmentTypeDefinition.sharesPossibleTypesWith(other = parentTypeDefinition, schema = schema)) {
      registerIssue(
          message = "Fragment `$name` cannot be spread here as result can never be of type `${parentTypeDefinition.name}`",
          sourceLocation = sourceLocation
      )
      return
    }

    validateArguments(
        arguments,
        sourceLocation,
        fragmentDefinition.variableDefinitions.map {
          GQLInputValueDefinition(
              sourceLocation = it.sourceLocation,
              description = it.description,
              name = it.name,
              directives = it.directives,
              type = it.type,
              defaultValue = it.defaultValue
          )
        },
        debug = "fragment `$name`"
    ) {
      variableUsages.add(it)
    }

    validateDirectives(directives, this) {
      variableUsages.add(it)
    }
  }

  private fun GQLDocument.validateExecutable() {
    definitions.firstOrNull { it !is GQLOperationDefinition && it !is GQLFragmentDefinition }
        ?.let {
          registerIssue(message = "Found an non-executable definition.", sourceLocation = it.sourceLocation)
          return
        }
  }

  /**
   * Validate the fragment outside the context of an operation
   * This can be helpful to show warnings in the IDE while editing fragments of a parent module, and the fragment may appear unused
   * This will not catch field merging conflicts and missing variables. Ultimately, validation
   * in the context of an operation is required.
   */
  private fun GQLFragmentDefinition.validate() {
    val rootTypeDefinition = typeDefinitions[typeCondition.name]
    if (rootTypeDefinition == null) {
      registerIssue(
          message = "Cannot find type `${typeCondition.name}` for fragment `$name`",
          sourceLocation = typeCondition.sourceLocation
      )
      return
    }

    validateCommon(this, selections, directives, variableDefinitions, rootTypeDefinition)

    if (schema.errorAware) {
      validateCatch(selections.collectFieldsNoFragments())
    }

    val ownVariables = variableDefinitions.map { it.name }.toSet()
    val (ownVariableUsages, operationVariableUsages) = variableUsages.partition { it.variable.name in ownVariables }
    validateVariableUsages(variableDefinitions, ownVariableUsages, context = "fragment `$name`")

    /**
     * We only track the variables that are not defined by this fragment.
     */
    fragmentOperationVariableUsages[name] = operationVariableUsages
  }

  /**
   * Common validation code between operations and fragments
   * This does not do any checking of merged fields or recurse into fragment spreads
   */
  private fun validateCommon(
      directiveContext: GQLNode,
      selections: List<GQLSelection>,
      directives: List<GQLDirective>,
      variableDefinitions: List<GQLVariableDefinition>,
      rootTypeDefinition: GQLTypeDefinition,
  ) {
    variableUsages.clear()

    selections.validate(rootTypeDefinition)

    validateDirectives(directives, directiveContext) {
      /**
       * ```graphql
       * query GetFoo($arg: Int!) @myDirective(arg: $arg) {
       *   foo
       * }
       * ```
       */
      variableUsages.add(it)
    }

    variableDefinitions.forEach { varDef ->
      if (!varDef.type.isInputType(typeDefinitions)) {
        registerIssue(
            message = "Variable `${varDef.name}` of type `${varDef.type.pretty()}` is not an input type",
            sourceLocation = varDef.sourceLocation
        )
      }
      if (varDef.defaultValue != null) {
        validateAndCoerceValue(
            value = varDef.defaultValue,
            expectedType = varDef.type,
            hasLocationDefaultValue = false,
            isOneOfInputField = false
        ) {
          issues.add(it.constContextError())
        }
      }

      validateDirectivesInConstContext(varDef.directives, varDef)
    }
  }

  private fun GQLOperationDefinition.validate() {
    val rootTypeDefinition = rootTypeDefinition(schema)

    if (rootTypeDefinition == null) {
      registerIssue(
          message = "Cannot find a root type for operation type `$operationType`",
          sourceLocation = sourceLocation
      )
      return
    }
    validateCommon(this, selections, directives, variableDefinitions, rootTypeDefinition)

    if (schema.errorAware) {
      validateCatch(selections.collectFieldsNoFragments())
    }

    fieldsInSetCanMerge(this, schema, fragmentDefinitions.filter {
      /**
       * Prevent infinite recursion. We could probably do both validations at the
       * same time and save a bit of time.
       * We could also detect more issues, see `finds_invalid_case_even_with_immediately_recursive_fragment.graphql`
       */
      it.key !in cyclicFragments
    })

    validateDirectives(directives, this) {
      variableUsages.add(it)
    }
    issues.addAll(validateDeferLabels(this, rootTypeDefinition.name, schema, fragmentDefinitions))

    val allVariableUsages = selections.collectFragmentSpreads().flatMap { fragmentOperationVariableUsages.get(it) ?: emptyList() } + variableUsages
    validateVariableUsages(variableDefinitions, allVariableUsages, context = this.pretty())
  }

  private fun validateVariableUsages(variableDefinitions: List<GQLVariableDefinition>, variableUsages: List<VariableUsage>, context: String) {
    variableUsages.forEach {
      validateVariableUsage(
          variableUsage = it,
          variableDefinitions = variableDefinitions,
          context = context,
      )
    }
    val foundVariables = variableUsages.map { it.variable.name }.toSet()
    variableDefinitions.forEach {
      if (!foundVariables.contains(it.name)) {
        issues.add(UnusedVariable(
            message = "Variable `${it.name}` is unused in $context",
            sourceLocation = it.sourceLocation
        )
        )
      }
    }

  }

  private fun List<GQLSelection>.collectFragmentSpreads(): Set<String> {
    val collectedFragments = mutableSetOf<String>()
    val stack = mutableListOf<List<GQLSelection>>()
    stack.add(this)
    while (stack.isNotEmpty()) {
      val selections = stack.removeFirst()
      selections.forEach {
        when (it) {
          is GQLField -> {
            stack.add(it.selections)
          }

          is GQLFragmentSpread -> {
            if (!collectedFragments.contains(it.name)) {
              fragmentDefinitions.get(it.name)?.also {
                collectedFragments.add(it.name)
                stack.add(it.selections)
              }
            }
          }

          is GQLInlineFragment -> {
            stack.add(it.selections)
          }
        }
      }
    }

    return collectedFragments
  }

  private fun List<GQLSelection>.validate(parentTypeDefinition: GQLTypeDefinition) {
    if (isEmpty()) {
      // This will never happen from parsing documents but is kept for reference and to catch bad manual document modifications
      registerIssue(
          message = "Selection of type `${parentTypeDefinition.name}` must have a selection of sub-fields",
          sourceLocation = null
      )
      return
    }

    forEach {
      when (it) {
        is GQLField -> it.validate(parentTypeDefinition)
        is GQLInlineFragment -> it.validate(parentTypeDefinition)
        is GQLFragmentSpread -> it.validate(parentTypeDefinition)
      }
    }
  }

  private fun validateCatch(fields: List<GQLField>) {
    fields.groupBy { it.responseName() }
        .forEach {
          it.value.pairs().forEach {
            if (it.first.directives.findCatch(schema) != it.second.directives.findCatch(schema)) {
              addFieldMergingIssue(it.first, it.second, "they have different `@catch` directives")
            }
          }

          it.value.flatMap { it.selections }.collectFieldsNoFragments()
        }
  }

  private fun addFieldMergingIssue(fieldA: GQLField, fieldB: GQLField, message: String) {
    registerIssue(
        message = buildMessage(fieldA, fieldB, message),
        sourceLocation = fieldA.sourceLocation,
    )
    // Also add the symmetrical error
    registerIssue(
        message = buildMessage(fieldB, fieldA, message),
        sourceLocation = fieldB.sourceLocation,
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

  private fun <T> List<T>.pairs(): List<Pair<T, T>> {
    val pairs = mutableListOf<Pair<T, T>>()
    for (i in indices) {
      for (j in (i + 1).until(size)) {
        pairs.add(get(i) to get(j))
      }
    }
    return pairs
  }

  private fun List<GQLSelection>.collectFieldsNoFragments(): List<GQLField> {
    return flatMap { selection ->
      when (selection) {
        is GQLField -> listOf(selection)
        // XXX: we could relax the validation here as different inline fragments on different type
        // conditions might have different models
        is GQLInlineFragment -> selection.selections.collectFieldsNoFragments()
        is GQLFragmentSpread -> emptyList()
      }
    }
  }

  private fun List<GQLFragmentDefinition>.checkDuplicateFragments(): List<Issue> {
    val filtered = mutableMapOf<String, GQLFragmentDefinition>()
    forEach {
      val existing = filtered.putIfAbsentMpp(it.name, it)
      if (existing != null) {
        issues.add(OtherValidationIssue(
            message = "Fragment ${it.name} is already defined",
            sourceLocation = it.sourceLocation,
        )
        )
      }
    }
    return issues
  }

  private fun List<GQLOperationDefinition>.checkDuplicateOperations(): List<Issue> {
    val filtered = mutableMapOf<String, GQLOperationDefinition>()

    forEach {
      if (it.name == null) {
        issues.add(AnonymousOperation(
            message = "Apollo does not support anonymous operations",
            sourceLocation = it.sourceLocation,
        )
        )
        return@forEach
      }
      val existing = filtered.putIfAbsentMpp(it.name, it)
      if (existing != null) {
        issues.add(OtherValidationIssue(
            message = "Operation ${it.name} is already defined",
            sourceLocation = it.sourceLocation,
        )
        )
      }
    }
    return issues
  }
}

internal fun <T> MutableMap<String, T>.putIfAbsentMpp(key: String, value: T): T? {
  val existing = get(key)
  if (existing == null) {
    put(key, value)
    return null
  } else {
    return existing
  }
}


private fun buildMessage(fieldA: GQLField, fieldB: GQLField, message: String): String {
  return "`${fieldA.responseName()}` cannot be merged with `${fieldB.responseName()}` (at ${fieldB.sourceLocation.pretty()}): " +
      "$message. Use different aliases on the fields to fetch both if this was intentional."
}