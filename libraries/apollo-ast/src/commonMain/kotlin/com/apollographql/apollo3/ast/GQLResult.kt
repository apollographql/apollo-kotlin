package com.apollographql.apollo.ast

import com.apollographql.apollo.annotations.ApolloDeprecatedSince

/**
 * The result of a parsing or validation operation. It's tri-state:
 *
 * - value and no-issue => success
 * - no value and issues => failure
 * - value and issues => partial success
 *
 * - no value and no issues => not possible
 *
 * @property issues issues found during parsing/validations. Some of them might be Apollo specific and will not throw in [getOrThrow]
 */
class GQLResult<out V : Any>(
    val value: V?,
    val issues: List<Issue>,
) {

  init {
    check(value != null || issues.isNotEmpty()) {
      "Apollo: GQLResult must contain a value or issues"
    }
  }

  @Deprecated("Use getOrThrow instead", replaceWith = ReplaceWith("getOrThrow()"))
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  fun valueAssertNoErrors(): V {
    return getOrThrow()
  }

  /**
   * @throws SourceAwareException if there are GraphQL errors
   *
   * [ApolloIssue] are ignored
   */
  fun getOrThrow(): V {
    issues.checkValidGraphQL()

    check(value != null) {
      "Apollo: no value and no error found"
    }
    return value
  }
}