package com.apollographql.apollo3.ast


/**
 * The result of a parsing or validation operation. It's tri-state:
 *
 * - value and no-issue => success
 * - no value and issues => failure
 * - value and issues => partial success
 *
 * - no value and no issues => not possible
 */
class GQLResult<out V:Any>(
    val value: V?,
    val issues: List<Issue>
) {

  init {
    check (value != null || issues.containsError()) {
      "Apollo: GQLResult must contain a value or an error"
    }
  }

  /**
   * @throws SourceAwareException if there are validation errors
   */
   fun valueAssertNoErrors(): V {
    issues.checkNoErrors()

    check (value != null) {
      "Apollo: no value and no error found"
    }
    return value
  }
}