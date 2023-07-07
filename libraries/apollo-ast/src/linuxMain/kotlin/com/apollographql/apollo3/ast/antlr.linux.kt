package com.apollographql.apollo3.ast

import okio.BufferedSource

internal actual fun parseDocumentWithAntlr(
    source: BufferedSource,
    filePath: String?,
): GQLResult<GQLDocument> {
  TODO("Not yet implemented")
}

internal actual fun parseValueWithAntlr(
    source: BufferedSource,
    filePath: String?,
): GQLResult<GQLValue> {
  TODO("Not yet implemented")
}

internal actual fun parseTypeWithAntlr(
    source: BufferedSource,
    filePath: String?,
): GQLResult<GQLType> {
  TODO("Not yet implemented")
}

internal actual fun parseSelectionsWithAntlr(
    source: BufferedSource,
    filePath: String?,
): GQLResult<List<GQLSelection>> {
  TODO("Not yet implemented")
}