package com.apollographql.apollo3.ast

class SourceLocation(
    val line: Int,
    val position: Int,
    /**
     * The path to the document containing the node
     * Might be null if the document origin is not known
     */
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
