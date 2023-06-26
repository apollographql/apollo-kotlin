package com.apollographql.apollo3.ast

/**
 * @param line the line number of the source location, starting at 1
 *
 * @param position the position in the current line, starting at 0
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

  // line starts at 1
  // column starts at 0
  // The reasons are mainly historical. I think Antlr used to return this
  fun pretty(): String = "$filePath: ($line, ${position + 1})"

  companion object {
    val UNKNOWN = SourceLocation(-1, -1, null)
  }
}
