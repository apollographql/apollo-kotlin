package com.apollographql.apollo3.ast

import okio.BufferedSource

internal actual fun parseDocumentWithAntlr(
    source: BufferedSource,
    filePath: String?,
): GQLResult<GQLDocument> {
  throw UnsupportedOperationException("Antlr parser implementation is for the JVM")
}

internal actual fun parseValueWithAntlr(
    source: BufferedSource,
    filePath: String?,
): GQLResult<GQLValue> {
  throw UnsupportedOperationException("Antlr parser implementation is for the JVM")
}

internal actual fun parseTypeWithAntlr(
    source: BufferedSource,
    filePath: String?,
): GQLResult<GQLType> {
  throw UnsupportedOperationException("Antlr parser implementation is for the JVM")
}

internal actual fun parseSelectionsWithAntlr(
    source: BufferedSource,
    filePath: String?,
): GQLResult<List<GQLSelection>> {
  throw UnsupportedOperationException("Antlr parser implementation is for the JVM")
}