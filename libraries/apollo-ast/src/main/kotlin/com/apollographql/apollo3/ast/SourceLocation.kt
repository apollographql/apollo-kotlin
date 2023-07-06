package com.apollographql.apollo3.ast

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince

/**
 * @param line the line where the symbol starts, starting at 1
 *
 * @param column the column where the symbol starts, starting at 1
 *
 * @param endLine the line where the symbol ends, inclusive, starting at 1
 *
 * @param endColumn the column where the symbol ends, inclusive, starting at 1
 * *
 * @param filePath The path to the document containing the node
 * Might be null if the document origin is not known
 */
class SourceLocation(
    val line: Int,
    val column: Int,
    val endLine: Int,
    val endColumn: Int,
    val filePath: String?
) {
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  @Deprecated("Use column instead", ReplaceWith("column - 1"), DeprecationLevel.ERROR)
  val position: Int
    get() = column - 1

  override fun toString(): String {
    return "($line:$column)"
  }

  fun pretty(): String = "$filePath: ($line, $column)"

  companion object {
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
    @Deprecated("SourceLocation is now nullable and this is replaced by null", ReplaceWith("null"), level = DeprecationLevel.ERROR)
    val UNKNOWN = SourceLocation(-1, -1, -1, -1, null)
  }
}

fun SourceLocation?.pretty() = this?.pretty() ?: "(unknown location)"