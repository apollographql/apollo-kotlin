package com.apollographql.apollo.ast

import com.apollographql.apollo.annotations.ApolloDeprecatedSince

/**
 * @param start the offset where the symbol starts, inclusive, starting at 0
 * Note that because the parser works on UTF16 Strings, the offset is in terms of UTF16 chars and not unicode Chars
 *
 * @param end the offset where the symbol ends, exclusive
 * Because [end] is exclusive, you can use str.substring(start, end) to get the symbol text
 * Note that because the parser works on UTF16 Strings, the offset is in terms of UTF16 chars and not unicode Chars
 *
 * @param line the line where the symbol starts, starting at 1
 *
 * @param column the column where the symbol starts, starting at 1
 * Note that because the parser works on UTF16 Strings, the column is in terms of UTF16 chars and not unicode Chars
 *
 * @param filePath The path to the document containing the node
 * Might be null if the document origin is not known (parsing from a String for an example)
 */
class SourceLocation(
    val start: Int,
    val end: Int,
    val line: Int,
    val column: Int,
    val filePath: String?
) {
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  @Deprecated("Use column instead", ReplaceWith("column - 1"), DeprecationLevel.ERROR)
  val position: Int
    get() = column - 1

  override fun toString(): String {
    return pretty()
  }

  fun pretty(): String = "$filePath: ($line, $column)"

  companion object {
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
    @Deprecated("SourceLocation is now nullable and this is replaced by null", ReplaceWith("null"), level = DeprecationLevel.ERROR)
    val UNKNOWN = SourceLocation.forPath(null)

    /**
     * Constructs a [SourceLocation] that only contains a filePath for the moments when we're constructing nodes programmatically
     * but still want to carry around the path of the original nodes for debugging purposes.
     * TODO: I'm not sure how much this is helping vs confusing. We might want to remove that and just set a null sourceLocation in those cases
     */
    internal fun forPath(filePath: String?): SourceLocation {
      return SourceLocation(
          start = 0,
          end = 1,
          line = -1,
          column = -1,
          filePath = filePath
      )
    }
  }
}

fun SourceLocation?.pretty() = this?.pretty() ?: "(unknown location)"