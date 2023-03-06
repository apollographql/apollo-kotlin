package com.apollographql.apollo3.ast.internal

import com.apollographql.apollo3.ast.GQLArgument
import com.apollographql.apollo3.ast.GQLDirective
import com.apollographql.apollo3.ast.GQLDirectiveDefinition
import com.apollographql.apollo3.ast.GQLDirectiveLocation
import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.GQLEnumValueDefinition
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFieldDefinition
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLInputValueDefinition
import com.apollographql.apollo3.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo3.ast.GQLNode
import com.apollographql.apollo3.ast.GQLNonNullType
import com.apollographql.apollo3.ast.GQLNullValue
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.GQLOperationTypeDefinition
import com.apollographql.apollo3.ast.GQLScalarTypeDefinition
import com.apollographql.apollo3.ast.GQLSchemaDefinition
import com.apollographql.apollo3.ast.GQLSchemaExtension
import com.apollographql.apollo3.ast.GQLStringValue
import com.apollographql.apollo3.ast.GQLTypeDefinition
import com.apollographql.apollo3.ast.GQLUnionTypeDefinition
import com.apollographql.apollo3.ast.GQLVariableDefinition
import com.apollographql.apollo3.ast.Issue
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.Schema.Companion.TYPE_POLICY
import com.apollographql.apollo3.ast.SourceLocation
import com.apollographql.apollo3.ast.ValidationDetails
import com.apollographql.apollo3.ast.VariableUsage
import com.apollographql.apollo3.ast.findDeprecationReason
import com.apollographql.apollo3.ast.isVariableUsageAllowed
import com.apollographql.apollo3.ast.parseAsGQLSelections
import com.apollographql.apollo3.ast.pretty
import okio.Buffer

interface IssuesScope {
  val issues: MutableList<Issue>
}

/**
 * The base interface for different validation scopes
 * A validation scope is a mutable class that keeps track of issues.
 * It also has typeDefinitions and directiveDefinitions from the schema. Some methods are shared between schema and executable validation.
 *
 */
internal interface ValidationScope : IssuesScope {
  override val issues: MutableList<Issue>

  val typeDefinitions: Map<String, GQLTypeDefinition>
  val directiveDefinitions: Map<String, GQLDirectiveDefinition>
  val foreignNames: Map<String, String>

  fun originalDirectiveName(name: String): String {
    return foreignNames["@$name"]?.substring(1) ?: name
  }

  fun originalTypeName(name: String): String {
    return foreignNames[name]?: name
  }

  fun registerIssue(
      message: String,
      sourceLocation: SourceLocation,
      severity: Issue.Severity = Issue.Severity.ERROR,
      details: ValidationDetails = ValidationDetails.Other,
  ) {
    issues.add(
        Issue.ValidationError(
            message,
            sourceLocation,
            severity,
            details
        )
    )
  }
}

internal class DefaultValidationScope(
    override val typeDefinitions: Map<String, GQLTypeDefinition>,
    override val directiveDefinitions: Map<String, GQLDirectiveDefinition>,
    issues: MutableList<Issue>? = null,
    override val foreignNames: Map<String, String> = emptyMap()
) : ValidationScope {
  constructor(schema: Schema) : this(schema.typeDefinitions, schema.directiveDefinitions)

  override val issues = issues ?: mutableListOf()
}

/**
 * @param directiveContext the node representing the location where this directive is applied
 */
internal fun ValidationScope.validateDirective(
    directive: GQLDirective,
    directiveContext: GQLNode,
    registerVariableUsage: (VariableUsage) -> Unit,
) {
  val directiveLocation = when (directiveContext) {
    is GQLField -> GQLDirectiveLocation.FIELD
    is GQLInlineFragment -> GQLDirectiveLocation.INLINE_FRAGMENT
    is GQLFragmentSpread -> GQLDirectiveLocation.FRAGMENT_SPREAD
    is GQLObjectTypeDefinition -> GQLDirectiveLocation.OBJECT
    is GQLOperationTypeDefinition -> {
      when (directiveContext.operationType) {
        "query" -> GQLDirectiveLocation.QUERY
        "mutation" -> GQLDirectiveLocation.MUTATION
        "subscription" -> GQLDirectiveLocation.SUBSCRIPTION
        else -> error("unknown operation: $directiveContext")
      }
    }
    is GQLFragmentDefinition -> GQLDirectiveLocation.FRAGMENT_DEFINITION
    is GQLVariableDefinition -> GQLDirectiveLocation.VARIABLE_DEFINITION
    is GQLSchemaDefinition, is GQLSchemaExtension -> GQLDirectiveLocation.SCHEMA
    is GQLScalarTypeDefinition -> GQLDirectiveLocation.SCALAR
    is GQLFieldDefinition -> GQLDirectiveLocation.FIELD_DEFINITION
    is GQLInputValueDefinition -> error("validating directives on input values is not supported yet as we need to distinguish between arguments and inputfields")
    is GQLInterfaceTypeDefinition -> GQLDirectiveLocation.INTERFACE
    is GQLUnionTypeDefinition -> GQLDirectiveLocation.UNION
    is GQLEnumTypeDefinition -> GQLDirectiveLocation.ENUM
    is GQLEnumValueDefinition -> GQLDirectiveLocation.ENUM_VALUE
    is GQLInputObjectTypeDefinition -> GQLDirectiveLocation.INPUT_OBJECT
    else -> error("Cannot determine directive location for $directiveContext")

  }
  val directiveDefinition = directiveDefinitions[directive.name]

  if (directiveDefinition == null) {
    // TODO: make this an error
    registerIssue(
        message = "Unknown directive '@${directive.name}'",
        sourceLocation = directive.sourceLocation,
        details = ValidationDetails.UnknownDirective,
        severity = Issue.Severity.WARNING
    )

    return
  }

  if (directiveLocation !in directiveDefinition.locations) {
    registerIssue(
        message = "Directive '${directive.name}' cannot be applied on '$directiveLocation'",
        sourceLocation = directive.sourceLocation
    )

    return
  }

  validateArguments(
      directive.arguments?.arguments ?: emptyList(),
      directive.sourceLocation,
      directiveDefinition.arguments,
      "directive '${directiveDefinition.name}'",
      registerVariableUsage
  )

  /**
   * Apollo specific validation
   */
  if (originalDirectiveName(directive.name) == Schema.NONNULL) {
    extraValidateNonNullDirective(directive, directiveContext)
  }
  if (originalDirectiveName(directive.name) == Schema.FIELD_POLICY) {
    extraValidateTypePolicyDirective(directive)
  }
}

/**
 * Extra Apollo-specific validation for @nonnull
 */
internal fun ValidationScope.extraValidateNonNullDirective(directive: GQLDirective, directiveContext: GQLNode) {
  if (directiveContext is GQLField && (directive.arguments?.arguments?.size ?: 0) > 0) {
    registerIssue(
        message = "'${directive.name}' cannot have arguments when applied on a field",
        sourceLocation = directive.sourceLocation
    )

  } else if (directiveContext is GQLObjectTypeDefinition && (directive.arguments?.arguments?.size ?: 0) == 0) {
    registerIssue(
        message = "'${directive.name}' must contain a selection of fields",
        sourceLocation = directive.sourceLocation
    )
    val stringValue = (directive.arguments!!.arguments.first().value as GQLStringValue).value

    val selections = stringValue.buffer().parseAsGQLSelections().getOrThrow()

    val badSelection = selections.firstOrNull { it !is GQLField }
    check(badSelection == null) {
      "'$badSelection' cannot be made non-null. '$stringValue' should only contain fields."
    }

    val nonNullFields = selections.map { (it as GQLField).name }.toSet()
    val schemaFields = directiveContext.fields.map { it.name }.toSet()

    val unknownFields = nonNullFields - schemaFields
    check(unknownFields.isEmpty()) {
      "Fields '${unknownFields.joinToString()}' are not defined in ${directiveContext.name}"
    }
  }
}

/**
 * Extra Apollo-specific validation for @typePolicy
 */
internal fun ValidationScope.extraValidateTypePolicyDirective(directive: GQLDirective) {
  (directive.arguments!!.arguments.first().value as GQLStringValue).value.buffer().parseAsGQLSelections().getOrThrow().forEach {
    if (it !is GQLField) {
      registerIssue("Fragments are not supported in @$TYPE_POLICY directives", it.sourceLocation)
    } else if (it.selectionSet != null) {
      registerIssue("Composite fields are not supported in @$TYPE_POLICY directives", it.sourceLocation)
    }
  }
}

internal fun String.buffer() = Buffer().writeUtf8(this)

private fun ValidationScope.validateArgument(
    argument: GQLArgument,
    inputValueDefinitions: List<GQLInputValueDefinition>,
    debug: String,
    registerVariableUsage: (VariableUsage) -> Unit,
) = with(argument) {
  val schemaArgument = inputValueDefinitions.firstOrNull { it.name == name }
  if (schemaArgument == null) {
    registerIssue(message = "Unknown argument `$name` on $debug", sourceLocation = sourceLocation)
    return@with
  }
  if (schemaArgument.directives.findDeprecationReason() != null) {
    issues.add(Issue.DeprecatedUsage(message = "Use of deprecated argument `$name`", sourceLocation = sourceLocation))
  }

  // 5.6.2 Input Object Field Names
  // Note that this does not modify the document, it calls coerce because it's easier
  // to validate at the same time but the coerced result is not used here
  validateAndCoerceValue(argument.value, schemaArgument.type, schemaArgument.defaultValue != null, registerVariableUsage)
}

/**
 * validates fields or directive arguments
 *
 * See https://spec.graphql.org/draft/#sec-Validation.Arguments
 *
 * @param sourceLocation the location of the field or directive for error reporting
 * @param registerVariableUsage a callback when a variable is found
 */
internal fun ValidationScope.validateArguments(
    arguments: List<GQLArgument>,
    sourceLocation: SourceLocation,
    inputValueDefinitions: List<GQLInputValueDefinition>,
    debug: String,
    registerVariableUsage: (VariableUsage) -> Unit,
) {
  // 5.4.2 Argument Uniqueness
  arguments.groupBy { it.name }.filter { it.value.size > 1 }.toList().firstOrNull()?.let {
    registerIssue(message = "Argument `${it.first}` is defined multiple times", sourceLocation = it.second.first().sourceLocation)
    return
  }

  // 5.4.2.1 Required arguments
  inputValueDefinitions.forEach { inputValueDefinition ->
    if (inputValueDefinition.type is GQLNonNullType && inputValueDefinition.defaultValue == null) {
      val argumentValue = arguments.firstOrNull { it.name == inputValueDefinition.name }?.value
      if (argumentValue is GQLNullValue) {
        // This will be caught later when validating individual arguments
        // registerIssue((message = "Cannot pass `null` for a required argument", sourceLocation = argumentValue.sourceLocation))
      } else if (argumentValue == null) {
        registerIssue(message = "No value passed for required argument '${inputValueDefinition.name}'", sourceLocation = sourceLocation)
      }
    }
  }

  arguments.forEach {
    validateArgument(it, inputValueDefinitions, debug, registerVariableUsage)
  }
}

internal fun ValidationScope.validateVariable(
    operation: GQLOperationDefinition?,
    variableUsage: VariableUsage,
) {
  if (operation == null) {
    // if operation is null, it means we're currently validating a fragment outside the context of an operation
    return
  }

  val variable = variableUsage.variable
  val variableDefinition = operation.variableDefinitions.firstOrNull { it.name == variable.name }
  if (variableDefinition == null) {
    registerIssue(
        message = "Variable `${variable.name}` is not defined by operation `${operation.name}`",
        sourceLocation = variable.sourceLocation
    )
    return
  }
  if (!isVariableUsageAllowed(variableDefinition = variableDefinition, usage = variableUsage)) {
    registerIssue(
        message = "Variable `${variable.name}` of type `${variableDefinition.type.pretty()}` used in position expecting type `${variableUsage.locationType.pretty()}`",
        sourceLocation = variable.sourceLocation
    )
  }
}
