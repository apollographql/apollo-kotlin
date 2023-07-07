package com.apollographql.apollo3.ast

import com.apollographql.apollo3.ast.internal.antlrParse
import com.apollographql.apollo3.ast.internal.toGQLDocument
import com.apollographql.apollo3.ast.internal.toGQLSelection
import com.apollographql.apollo3.ast.internal.toGQLType
import com.apollographql.apollo3.ast.internal.toGQLValue
import okio.BufferedSource

internal actual fun parseDocumentWithAntlr(
    source: BufferedSource,
    filePath: String?,
): GQLResult<GQLDocument> {
  return antlrParse(source, filePath, { it.document() }, { it.toGQLDocument(filePath) })
}

internal actual fun parseValueWithAntlr(
    source: BufferedSource,
    filePath: String?,
): GQLResult<GQLValue> {
  return antlrParse(source, filePath, { it.value() }, { it.toGQLValue(filePath) })
}

internal actual fun parseTypeWithAntlr(
    source: BufferedSource,
    filePath: String?,
): GQLResult<GQLType> {
  return antlrParse(source, filePath, { it.type() }, { it.toGQLType(filePath) })
}

internal actual fun parseSelectionsWithAntlr(
    source: BufferedSource,
    filePath: String?,
): GQLResult<List<GQLSelection>> {
  return antlrParse(source, filePath, { it.selections() }, { it.selection().map { it.toGQLSelection(filePath) } })
}