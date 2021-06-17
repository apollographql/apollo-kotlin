package com.apollographql.apollo3.ast

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * The result of a parsing operation. Because syntax errors are fatal, this is either Success or Error
 */
sealed class ParseResult<out V:Any> {
  class Success<V: Any>(val value: V) : ParseResult<V>()
  class Error(val issues: List<Issue.ParsingError>) : ParseResult<Nothing>()

  fun getOrNull() = (this as? Success)?.value
  fun getOrThrow(): V {
    when (this) {
      is Success -> return value
      is Error -> {
        check (issues.isNotEmpty())
        /**
         * All parsing errors are fatal
         */
        val firstError = issues.first()
        throw SourceAwareException(firstError.message, firstError.sourceLocation)
      }
    }
  }
}