package com.apollographql.apollo3.ast


/**
 * All the issues that can be collected while analyzing a graphql document
 */
sealed class Issue(
    val message: String,
    val sourceLocation: SourceLocation,
    val severity: Severity,
) {
  /**
   * A grammar error
   */
  class ParsingError(message: String, sourceLocation: SourceLocation) : Issue(message, sourceLocation, Severity.ERROR)

  /**
   * A GraphqQL validation error as per the spec
   */
  class ValidationError(
      message: String,
      sourceLocation: SourceLocation,
      severity: Severity = Severity.ERROR,
      val details: ValidationDetails = ValidationDetails.Other,
  ) : Issue(message, sourceLocation, severity)

  /**
   * A deprecated field/enum is used
   */
  class DeprecatedUsage(message: String, sourceLocation: SourceLocation) : Issue(message, sourceLocation, Severity.WARNING)

  /**
   * A variable is unused
   */
  class UnusedVariable(message: String, sourceLocation: SourceLocation) : Issue(message, sourceLocation, Severity.WARNING)

  /**
   * A fragment has an @include or @skip directive. While this is valid GraphQL, the responseBased codegen does not support that
   */
  class ConditionalFragment(message: String, sourceLocation: SourceLocation) : Issue(message, sourceLocation, Severity.ERROR)

  /**
   * When models are nested, upper case fields are not supported as Kotlin doesn't allow a property name
   * with the same name as a nested class.
   * If this happens, the easiest solution is to add an alias with a lower case first letter.
   * If there are a lot of such fields, the Apollo compiler option `flattenModels` can also be used to circumvent this
   * error at the price of possible suffixes in model names.
   *
   * This error is an Apollo Kotlin specific error
   */
  class UpperCaseField(message: String, sourceLocation: SourceLocation) : Issue(message, sourceLocation, Severity.ERROR)

  /**
   * Certain enum value names such as `type` are reserved for Apollo.
   *
   * This error is an Apollo Kotlin specific error
   */
  class ReservedEnumValueName(message: String, sourceLocation: SourceLocation) : Issue(message, sourceLocation, Severity.ERROR)

  enum class Severity {
    WARNING,
    ERROR,
  }
}

fun List<Issue>.checkNoErrors() {
  val error = firstOrNull { it.severity == Issue.Severity.ERROR }
  if (error != null) {
    throw SourceAwareException(
        error.message,
        error.sourceLocation
    )
  }
}

fun List<Issue>.containsError(): Boolean = any { it.severity == Issue.Severity.ERROR }


enum class ValidationDetails {
  /**
   * An unknown directive was found.
   *
   * In a perfect world everyone uses SDL schemas and we can validate directives but in this world, a lot of users rely
   * on introspection schemas that do not contain directives. If this happens, we pass them through without validation.
   */
  UnknownDirective,

  /**
   * Two type definitions have the same name
   */
  DuplicateTypeName,
  Other
}
