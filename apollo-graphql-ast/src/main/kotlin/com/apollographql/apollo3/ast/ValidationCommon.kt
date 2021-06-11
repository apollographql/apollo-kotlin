package com.apollographql.apollo3.ast

private fun GQLDirective.validate(directiveLocation: GQLDirectiveLocation, directiveDefinitions: Map<String, GQLDirectiveDefinition>): List<Issue> {
  val issues = mutableListOf<Issue>()
  val directiveDefinition = directiveDefinitions[name]

  if (directiveDefinition == null) {
    issues.add(
        Issue.ValidationError(
            message = "Unknown directive '$name'",
            sourceLocation = sourceLocation,
            details = ValidationDetails.UnknownDirective,
            severity = Issue.Severity.WARNING
        )
    )
    return issues
  }

  if (directiveLocation !in directiveDefinition.locations) {
    issues.add(
        Issue.ValidationError(
            message = "Directive '$name' cannot be applied on '$directiveLocation'",
            sourceLocation = sourceLocation
        )
    )
    return issues
  }

  arguments?.validate(directiveDefinition.arguments, "directive '${directiveDefinition.name}'")

  if (name == "nonnull") {
    if (directiveLocation == GQLDirectiveLocation.FIELD && (arguments?.arguments?.size ?: 0) > 0) {
      issues.add(
          Issue.ValidationError(
              message = "'$name' cannot have arguments when applied on a field",
              sourceLocation = sourceLocation
          )
      )
    } else if (directiveLocation == GQLDirectiveLocation.OBJECT && (arguments?.arguments?.size ?: 0) == 0) {
      issues.add(
          Issue.ValidationError(
              message = "'$name' must contain a list of fields",
              sourceLocation = sourceLocation
          )
      )
    }
  }

  return issues
}

internal class IssueBuilder {
  val issues = mutableListOf<Issue>()
}
internal fun buildIssues(block: IssueBuilder.() -> Unit): List<Issue> {
  val issueBuilder = IssueBuilder()
  issueBuilder.block()
  return issueBuilder.issues
}

private fun GQLArgument.validate(inputValueDefinitions: List<GQLInputValueDefinition>, debug: String) = buildIssues {
  val schemaArgument = inputValueDefinitions.firstOrNull { it.name == name }
  if (schemaArgument == null) {
    issues.add(Issue.ValidationError(message = "Unknown argument `$name` on $debug", sourceLocation = sourceLocation))
    return@buildIssues
  }

  // 5.6.2 Input Object Field Names
  // Note that this does not modify the document, it calls coerce because it's easier
  // to validate at the same time but the coerced result is not used here
  val coercionResult = value.validateAndCoerce(schemaArgument.type, schema.typeDefinitions)
  variableReferences.addAll(coercionResult.variableReferences)
  issues.addAll(coercionResult.issues)
}

private fun GQLArguments.validate(inputValueDefinitions: List<GQLInputValueDefinition>, debug: String) {
  // 5.4.2 Argument Uniqueness
  arguments.groupBy { it.name }.filter { it.value.size > 1 }.toList().firstOrNull()?.let {
    issues.add(Issue.ValidationError(message = "Argument `${it.first}` is defined multiple times", sourceLocation = it.second.first().sourceLocation))
    return
  }

  // 5.4.2.1 Required arguments
  inputValueDefinitions.forEach { inputValueDefinition ->
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
    it.validate(inputValueDefinitions, debug)
  }
}

private fun validateVariable(operation: GQLOperationDefinition?, value: GQLVariableValue, expectedType: GQLType) {
  if (operation == null) {
    // if operation is null, it means we're currently validating a fragment outside the context of an operation
    return
  }

  val variableDefinition = operation.variableDefinitions.firstOrNull { it.name == value.name }
  if (variableDefinition == null) {
    issues.add(Issue.ValidationError(
        message = "Variable `${value.name}` is not defined by operation `${operation.name}`",
        sourceLocation = value.sourceLocation
    ))
    return
  }
  if (!variableDefinition.type.canInputValueBeAssignedTo(target = expectedType)) {
    issues.add(Issue.ValidationError(
        message = "Variable `${value.name}` of type `${variableDefinition.type.pretty()}` used in position expecting type `${expectedType.pretty()}`",
        sourceLocation = value.sourceLocation
    ))
  }
}