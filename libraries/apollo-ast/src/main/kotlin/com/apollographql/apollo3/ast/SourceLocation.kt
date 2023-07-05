package com.apollographql.apollo3.ast

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince

/**
 * @param line the line number where the symbol starts, starting at 1
 *
 * @param column the column in the current line where the symbol starts, starting at 1
 *
 * @param endLine the line number where the symbol end, inclusive, starting at 1
 *
 * @param endColumn the column in the current line where the symbol end, inclusive, starting at 1
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
    val UNKNOWN = SourceLocation(-1, -1, -1, -1, null)
  }
}
