package com.apollographql.apollo.ast

/**
 * All the issues that can be collected while analyzing a graphql document
 */
// TODO: support multiple sourceLocations. A single issue like a redefinition for an example, might have several impacted sourceLocations.
sealed interface Issue {
  val message: String
  val sourceLocation: SourceLocation?
}

/**
 * An issue from the GraphQL spec, either a parsing error or a validation issue.
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
 * In case a user rely on non-introspection schemas (that do not contain directives definitions), the apollo compiler:
 * - adds the built-in directives (`@include`, `@skip`, ...)
 * - adds the `kotlin_labs/v3` directives (for legacy reasons, will be removed in a future version)
 *
 * For anything else, including `@defer` and `@oneOf`, a full schema is required so that the compiler can do feature
 * detection and validate operation based on what the server actually supports.
 */
class UnknownDirective(
    override val message: String,
    override val sourceLocation: SourceLocation?,
) : GraphQLValidationIssue

/**
 * The provided schema contains a definition that doesn't match the GraphQL specification.
 *
 * This may happen in particular for directive that are in the process of being specified like `@defer` (as of Jul 2025).
 *
 * Because the Apollo compiler expects a certain shape of directive and arguments, anything else may trigger
 * a crash in the compiler. By validating explicitly, the error is more explicit.
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
 * There is a discussion about whether this can be relaxed.
 * See https://github.com/apollographql/apollo-kotlin/issues/4320.
 */
class DifferentShape(override val message: String, override val sourceLocation: SourceLocation?) : GraphQLValidationIssue

class UnusedFragment(override val message: String, override val sourceLocation: SourceLocation?) : GraphQLValidationIssue

/**
 * There is a fragment cycle.
 *
 * See https://spec.graphql.org/September2025/#sec-Fragment-Spreads-Must-Not-Form-Cycles
 */
class FragmentCycle(override val message: String, override val sourceLocation: SourceLocation?) : GraphQLValidationIssue

/**
 * Two type definitions have the same name
 */
class DuplicateTypeName(override val message: String, override val sourceLocation: SourceLocation?) : GraphQLValidationIssue

/**
 * This is a bit abused for kotlin_labs directive that override the existing ones for compatibility reasons.
 * This is so that `ApolloCompiler` can later on treat them as warnings.
 */
class DirectiveRedefinition(
    val name: String,
    existingSourceLocation: SourceLocation?,
    override val sourceLocation: SourceLocation?,
) : GraphQLValidationIssue {
  override val message = "Implicit kotlin_labs definition '@${name}' overrides explicit one provided by the schema. Import kotlin_labs explicitly using @link."
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
 * @nonnull is used
 */
class NonNullUsage(override val message: String, override val sourceLocation: SourceLocation?): ApolloIssue
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