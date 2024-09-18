package com.apollographql.apollo.ast.internal

import com.apollographql.apollo.ast.DeprecatedUsage
import com.apollographql.apollo.ast.GQLArgument
import com.apollographql.apollo.ast.GQLDirective
import com.apollographql.apollo.ast.GQLDirectiveDefinition
import com.apollographql.apollo.ast.GQLDirectiveLocation
import com.apollographql.apollo.ast.GQLEnumTypeDefinition
import com.apollographql.apollo.ast.GQLEnumValueDefinition
import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFieldDefinition
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLFragmentSpread
import com.apollographql.apollo.ast.GQLInlineFragment
import com.apollographql.apollo.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo.ast.GQLInputValueDefinition
import com.apollographql.apollo.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo.ast.GQLNode
import com.apollographql.apollo.ast.GQLNonNullType
import com.apollographql.apollo.ast.GQLNullValue
import com.apollographql.apollo.ast.GQLObjectTypeDefinition
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.GQLScalarTypeDefinition
import com.apollographql.apollo.ast.GQLSchemaDefinition
import com.apollographql.apollo.ast.GQLSchemaExtension
import com.apollographql.apollo.ast.GQLStringValue
import com.apollographql.apollo.ast.GQLTypeDefinition
import com.apollographql.apollo.ast.GQLUnionTypeDefinition
import com.apollographql.apollo.ast.GQLVariableDefinition
import com.apollographql.apollo.ast.Issue
import com.apollographql.apollo.ast.OtherValidationIssue
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.Schema.Companion.TYPE_POLICY
import com.apollographql.apollo.ast.SourceLocation
import com.apollographql.apollo.ast.UnknownDirective
import com.apollographql.apollo.ast.VariableUsage
import com.apollographql.apollo.ast.findDeprecationReason
import com.apollographql.apollo.ast.isVariableUsageAllowed
import com.apollographql.apollo.ast.parseAsGQLSelections
import com.apollographql.apollo.ast.pretty

internal interface IssuesScope {
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
    return foreignNames[name] ?: name
  }

  fun registerIssue(
      message: String,
      sourceLocation: SourceLocation?,
  ) {
    issues.add(
        OtherValidationIssue(
            message,
            sourceLocation,
        )
    )
  }
}

internal class DefaultValidationScope(
    override val typeDefinitions: Map<String, GQLTypeDefinition>,
    override val directiveDefinitions: Map<String, GQLDirectiveDefinition>,
    issues: MutableList<Issue>? = null,
    override val foreignNames: Map<String, String> = emptyMap(),
) : ValidationScope {
  constructor(schema: Schema) : this(schema.typeDefinitions, schema.directiveDefinitions)

  override val issues = issues ?: mutableListOf()
}

private fun ValidationScope.validateDirectiveInternal(
    directive: GQLDirective,
    directiveLocation: GQLDirectiveLocation,
    directiveDefinition: GQLDirectiveDefinition,
    registerVariableUsage: (VariableUsage) -> Unit,
) {
  if (directiveLocation !in directiveDefinition.locations) {
    registerIssue(
        message = "Directive '${directive.name}' cannot be applied on '$directiveLocation'",
        sourceLocation = directive.sourceLocation
    )

    return
  }

  validateArguments(
      directive.arguments,
      directive.sourceLocation,
      directiveDefinition.arguments,
      "directive '${directiveDefinition.name}'",
      registerVariableUsage
  )
}

/**
 * @param directiveContext the node representing the location where this directive is applied
 */
internal fun ValidationScope.validateDirectives(
    directives: List<GQLDirective>,
    directiveContext: GQLNode,
    registerVariableUsage: (VariableUsage) -> Unit,
) {
  val directiveLocation = when (directiveContext) {
    is GQLField -> GQLDirectiveLocation.FIELD
    is GQLInlineFragment -> GQLDirectiveLocation.INLINE_FRAGMENT
    is GQLFragmentSpread -> GQLDirectiveLocation.FRAGMENT_SPREAD
    is GQLObjectTypeDefinition -> GQLDirectiveLocation.OBJECT
    is GQLOperationDefinition -> {
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

  val pairs = directives.mapNotNull { directive ->
    val directiveDefinition = directiveDefinitions[directive.name]
    if (directiveDefinition == null) {
      when (val originalName = originalDirectiveName(directive.name)) {
        Schema.OPTIONAL,
        Schema.NONNULL,
        Schema.TYPE_POLICY,
        Schema.FIELD_POLICY,
        Schema.REQUIRES_OPT_IN,
        Schema.TARGET_NAME,
        -> {
          /**
           * This validation is lenient for historical reasons. We don't want to break users relying on this.
           * If you're reading this and there's a good reason to, you can move directives out of this branch and require user to
           * specify the correct `@link` directive
           */
          issues.add(UnknownDirective("Unknown directive '@${directive.name}'", directive.sourceLocation, requireDefinition = false))
        }
        else -> {
          issues.add(UnknownDirective("No directive definition found for '@${originalName}'", directive.sourceLocation, requireDefinition = true))
        }
      }

      return@mapNotNull null
    }

    directive to directiveDefinition
  }

  pairs.forEach {
    validateDirectiveInternal(it.first, directiveLocation, it.second, registerVariableUsage)

    /**
     * Apollo specific validation
     */
    if (originalDirectiveName(it.first.name) == Schema.NONNULL) {
      extraValidateNonNullDirective(it.first, directiveContext)
    }
    if (originalDirectiveName(it.first.name) == TYPE_POLICY) {
      extraValidateTypePolicyDirective(it.first, directiveContext)
    }
  }

  pairs.groupBy { it.first.name }.values.filter { it.size > 1 }.forEach { listOfPairs ->
    val definition = listOfPairs.first().second
    if (!definition.repeatable) {
      listOfPairs.forEach {
        issues.add(OtherValidationIssue("Directive '@${it.first.name}' cannot be repeated", it.first.sourceLocation))
      }
    }
  }
}

/**
 * Extra Apollo-specific validation for @nonnull
 */
internal fun ValidationScope.extraValidateNonNullDirective(directive: GQLDirective, directiveContext: GQLNode) {
  issues.add(
      DeprecatedUsage(message = "Using `@nonnull` is deprecated. Use `@semanticNonNull` and/or `@catch` instead. See https://go.apollo.dev/ak-nullability.", directive.sourceLocation)
  )
  if (directiveContext is GQLField && directive.arguments.isNotEmpty()) {
    registerIssue(
        message = "'${directive.name}' cannot have arguments when applied on a field",
        sourceLocation = directive.sourceLocation
    )

  } else if (directiveContext is GQLObjectTypeDefinition && directive.arguments.isEmpty()) {
    registerIssue(
        message = "'${directive.name}' must contain a selection of fields",
        sourceLocation = directive.sourceLocation
    )
    val stringValue = (directive.arguments.first().value as GQLStringValue).value

    val selections = stringValue.parseAsGQLSelections().getOrThrow()

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
internal fun ValidationScope.extraValidateTypePolicyDirective(directive: GQLDirective, directiveContext: GQLNode) {
  val fieldDefinitions: List<GQLFieldDefinition>
  val typeName: String
  when (directiveContext) {
    is GQLInterfaceTypeDefinition -> {
      fieldDefinitions = directiveContext.fields
      typeName = directiveContext.name
    }

    is GQLObjectTypeDefinition -> {
      fieldDefinitions = directiveContext.fields
      typeName = directiveContext.name
    }

    is GQLUnionTypeDefinition -> {
      fieldDefinitions = emptyList()
      typeName = directiveContext.name
    }

    else -> {
      // Should be caught by previous validation steps
      return
    }
  }

  validateTypePolicyArgument(directive, "keyFields", typeName, fieldDefinitions)
  validateTypePolicyArgument(directive, "embeddedFields", typeName, fieldDefinitions)
  validateTypePolicyArgument(directive, "connectionFields", typeName, fieldDefinitions)
}

private fun ValidationScope.validateTypePolicyArgument(
    directive: GQLDirective,
    argumentName: String,
    typeName: String,
    fieldDefinitions: List<GQLFieldDefinition>,
) {
  val keyFieldsArg = directive.arguments.firstOrNull { it.name == argumentName }
  if (keyFieldsArg != null) {
    (keyFieldsArg.value as GQLStringValue).value.parseAsGQLSelections().getOrThrow().forEach { selection ->
      if (selection !is GQLField) {
        registerIssue("Fragments are not supported in @$TYPE_POLICY directives", keyFieldsArg.sourceLocation)
      } else if (selection.selections.isNotEmpty()) {
        registerIssue("Composite fields are not supported in @$TYPE_POLICY directives", keyFieldsArg.sourceLocation)
      } else {
        val definition = fieldDefinitions.firstOrNull { it.name == selection.name }
        if (definition == null) {
          registerIssue("No such field: '$typeName.${selection.name}'", keyFieldsArg.sourceLocation)
        }
      }
    }
  }
}

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
    issues.add(DeprecatedUsage(message = "Use of deprecated argument `$name`", sourceLocation = sourceLocation))
  }

  // 5.6.2 Input Object Field Names
  // Note that this does not modify the document, it calls coerce because it's easier
  // to validate at the same time but the coerced result is not used here
  validateAndCoerceValue(
      value = argument.value,
      expectedType = schemaArgument.type,
      hasLocationDefaultValue = schemaArgument.defaultValue != null,
      isOneOfInputField = false,
      registerVariableUsage = registerVariableUsage
  )
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
    sourceLocation: SourceLocation?,
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

  if (variableUsage.isOneOfInputField && variableDefinition.type !is GQLNonNullType) {
    registerIssue(
        message = "Variable `${variable.name}` of type `${variableDefinition.type.pretty()}` used in a OneOf input type must be a non-null type",
        sourceLocation = variable.sourceLocation
    )
  }

  if (!isVariableUsageAllowed(variableDefinition = variableDefinition, usage = variableUsage)) {
    registerIssue(
        message = "Variable `${variable.name}` of type `${variableDefinition.type.pretty()}` used in position expecting type `${variableUsage.locationType.pretty()}`",
        sourceLocation = variable.sourceLocation
    )
  }
}
