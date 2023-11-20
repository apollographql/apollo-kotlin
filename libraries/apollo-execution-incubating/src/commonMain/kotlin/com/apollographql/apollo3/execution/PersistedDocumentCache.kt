package com.apollographql.apollo3.execution

import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.Issue

class PersistedDocument(
    val document: GQLDocument?,
    val issues: List<Issue>
)

interface PersistedDocumentCache {
    fun get(id: String): PersistedDocument?
    fun put(id: String, persistedDocument: PersistedDocument)
}