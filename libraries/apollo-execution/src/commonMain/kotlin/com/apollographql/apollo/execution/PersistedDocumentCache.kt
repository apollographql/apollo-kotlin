package com.apollographql.apollo.execution

import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.Issue

sealed interface PersistedDocument

class ValidPersistedDocument(
    val document: GQLDocument
): PersistedDocument

class ErrorPersistedDocument(
    val issues: List<Issue>
): PersistedDocument

interface PersistedDocumentCache {
    fun get(id: String): PersistedDocument?
    fun put(id: String, persistedDocument: PersistedDocument)
}