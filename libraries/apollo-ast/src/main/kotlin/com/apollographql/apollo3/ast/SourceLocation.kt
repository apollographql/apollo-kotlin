package com.apollographql.apollo3.ast

/**
 * @param line the line number of the source location, starting at 1
 *
 * @param column the position in the current line, starting at 0
 *
 * @param filePath The path to the document containing the node
 * Might be null if the document origin is not known
 */
class SourceLocation(
    val line: Int,
    val position: Int,
    val filePath: String?
) {

  override fun toString(): String {
    return "($line:$position)"
  }

  // antlr is 0-indexed but IntelliJ is 1-indexed. Add 1 so that clicking the link will land on the correct location
  fun pretty(): String = "$filePath: ($line, ${position + 1})"

  companion object {
    val UNKNOWN = SourceLocation(-1, -1, null)
  }
}
