package com.apollographql.apollo.compiler.frontend

/**
 * The result of a parsing operation.
 *
 * If value is null, issues will contain the parsing errors.
 * It is valid to have both value != null and issues not empty, for an example in the case of warnings.
 *
 * Use [orThrow] to get a non-null value or throw the first issue
 */
data class ParseResult<out T: Any>(
    val value: T?,
    val issues: List<Issue>
) {
  fun orThrow(): T {
    val firstError = issues.firstOrNull { it.severity == Issue.Severity.ERROR }
    if (firstError != null) {
      throw SourceAwareException(firstError.message, firstError.sourceLocation)
    }
    check (value !=null) {
      "null value and no issues"
    }
    return value
  }

  fun <R: Any> mapValue(transform: (T) -> R): ParseResult<R> {
    return ParseResult(
        value?.let {transform(it)},
        issues
    )
  }

  fun <R: Any> flatMap(transform: (T) -> ParseResult<R>): ParseResult<R> {
    val transformedResult = value?.let {transform(it)}
    return ParseResult(
        transformedResult?.value,
        issues + (transformedResult?.issues ?: emptyList())
    )
  }


  fun appendIssues(newIssues: (T) -> List<Issue>): ParseResult<T> {
    return ParseResult(
        value,
        issues + (value?.let {newIssues(it)} ?: emptyList())
    )
  }
}

/**
 * All the issues that can be collected while analyzing a graphql document
 */
sealed class Issue(
    val message: String,
    val sourceLocation: SourceLocation,
    val severity: Severity
) {
  class ValidationError(message: String, sourceLocation: SourceLocation) : Issue(message, sourceLocation, Severity.ERROR)
  class UnkownError(message: String, sourceLocation: SourceLocation) : Issue(message, sourceLocation, Severity.ERROR)
  class ParsingError(message: String, sourceLocation: SourceLocation) : Issue(message, sourceLocation, Severity.ERROR)
  class DeprecatedUsage(message: String, sourceLocation: SourceLocation) : Issue(message, sourceLocation, Severity.WARNING)

  enum class Severity {
    WARNING,
    ERROR
  }
}
