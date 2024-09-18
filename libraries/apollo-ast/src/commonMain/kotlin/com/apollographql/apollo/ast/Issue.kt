package com.apollographql.apollo.ast

import com.apollographql.apollo.annotations.ApolloInternal

/**
 * All the issues that can be collected while analyzing a graphql document
 */
sealed interface Issue {
  val message: String
  val sourceLocation: SourceLocation?
}

/**
 * An issue from the GraphQL spec
 */
sealed interface GraphQLIssue : Issue

/**
 * A validation issue from the GraphQL spec
 */
sealed interface GraphQLValidationIssue : GraphQLIssue

/**
 * A custom issue specific to the Apollo compiler
 */
sealed interface ApolloIssue : Issue

/**
 * A grammar error
 */
class ParsingError(override val message: String, override val sourceLocation: SourceLocation?) : GraphQLIssue

/**
 * An unknown directive was found.
 *
 * In a perfect world everyone uses SDL schemas, and we can validate directives but in this world, a lot of users rely
 * on introspection schemas that do not contain directives. If this happens, we pass them through without validation.
 *
 * In some cases (e.g. `@oneOf`) we want to enforce that the directive is defined. In that case [requireDefinition] is true and the issue
 * will be raised as an error rather than warning.
 */
class UnknownDirective @ApolloInternal constructor(
    override val message: String,
    override val sourceLocation: SourceLocation?,
    @ApolloInternal
    val requireDefinition: Boolean,
) : GraphQLValidationIssue {
  constructor(message: String, sourceLocation: SourceLocation?) : this(message, sourceLocation, false)
}

/**
 * The definition is inconsistent with the expected one.
 */
class IncompatibleDefinition(
    name: String,
    expectedDefinition: String,
    override val sourceLocation: SourceLocation?,
) : GraphQLValidationIssue {
  override val message = "Unexpected '$name' definition. Expecting '$expectedDefinition'."
}

/**
 * Fields have different shapes and cannot be merged
 *
 */
class DifferentShape(override val message: String, override val sourceLocation: SourceLocation?) : GraphQLValidationIssue

class UnusedFragment(override val message: String, override val sourceLocation: SourceLocation?) : GraphQLValidationIssue

/**
 * Two type definitions have the same name
 */
class DuplicateTypeName(override val message: String, override val sourceLocation: SourceLocation?) : GraphQLValidationIssue

class DirectiveRedefinition(
    val name: String,
    existingSourceLocation: SourceLocation?,
    override val sourceLocation: SourceLocation?,
) : GraphQLValidationIssue {
  override val message = "Directive '${name}' is defined multiple times. First definition is: ${existingSourceLocation.pretty()}"
}

class NoQueryType(override val message: String, override val sourceLocation: SourceLocation?) : GraphQLValidationIssue

class AnonymousOperation(override val message: String, override val sourceLocation: SourceLocation?) : ApolloIssue

/**
 * Another GraphQL validation error as per the spec
 */
class OtherValidationIssue(override val message: String, override val sourceLocation: SourceLocation?) : GraphQLValidationIssue

/**
 * A deprecated field/inputField/enumValue/directive is used
 */
class DeprecatedUsage(override val message: String, override val sourceLocation: SourceLocation?) : ApolloIssue

/**
 * A variable is unused
 */
class UnusedVariable(override val message: String, override val sourceLocation: SourceLocation?) : ApolloIssue

/**
 * A fragment has an @include or @skip directive. While this is valid GraphQL, the responseBased codegen does not support that
 */
class ConditionalFragment(override val message: String, override val sourceLocation: SourceLocation?) : ApolloIssue

/**
 * When models are nested, upper case fields are not supported as Kotlin doesn't allow a property name
 * with the same name as a nested class.
 * If this happens, the easiest solution is to add an alias with a lower case first letter.
 * If there are a lot of such fields, the Apollo compiler option `flattenModels` can also be used to circumvent this
 * error at the price of possible suffixes in model names.
 *
 */
class UpperCaseField(override val message: String, override val sourceLocation: SourceLocation?) : ApolloIssue

/**
 * The GraphQL spec allows inline fragments without a type condition, but we currently forbid this because we need the type condition
 * to name the models in operation based codegen.
 */
class InlineFragmentWithoutTypeCondition(override val message: String, override val sourceLocation: SourceLocation?) : ApolloIssue

/**
 * Certain enum value names such as `type` are reserved for Apollo.
 *
 * This error is an Apollo Kotlin specific error
 */
class ReservedEnumValueName(override val message: String, override val sourceLocation: SourceLocation?) : ApolloIssue

class InvalidDeferLabel(override val message: String, override val sourceLocation: SourceLocation?) : ApolloIssue

class VariableDeferLabel(override val message: String, override val sourceLocation: SourceLocation?) : ApolloIssue

class InvalidDeferDirective(override val message: String, override val sourceLocation: SourceLocation?) : ApolloIssue
class DuplicateDeferLabel(override val message: String, override val sourceLocation: SourceLocation?) : ApolloIssue

/**
 * Checks that a list of issues is valid GraphQL per the spec.
 * This ignores any [ApolloIssue]
 *
 * @throws [SourceAwareException] if there is any GraphQL issue
 */
fun List<Issue>.checkValidGraphQL() {
  val error = firstOrNull { it is GraphQLIssue }
  if (error != null) {
    throw SourceAwareException(
        error.message,
        error.sourceLocation
    )
  }
}

/**
 * Checks that a list of issue is empty, regardless of the issues.
 * This may throw on Apollo specific issues.
 *
 * @throws [SourceAwareException] if there is any issue
 */
fun List<Issue>.checkEmpty() {
  val error = firstOrNull()
  if (error != null) {
    throw SourceAwareException(
        error.message,
        error.sourceLocation
    )
  }
}