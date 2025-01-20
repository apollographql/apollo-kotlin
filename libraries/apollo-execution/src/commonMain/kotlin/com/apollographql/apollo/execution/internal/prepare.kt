@file:Suppress("UNCHECKED_CAST")
@file:OptIn(ApolloExperimental::class)

package com.apollographql.apollo.execution.internal

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.ast.*
import com.apollographql.apollo.execution.Coercing
import com.apollographql.apollo.execution.ErrorPersistedDocument
import com.apollographql.apollo.execution.ExternalValue
import com.apollographql.apollo.execution.GraphQLRequest
import com.apollographql.apollo.execution.InternalValue
import com.apollographql.apollo.execution.PersistedDocument
import com.apollographql.apollo.execution.PersistedDocumentCache
import com.apollographql.apollo.execution.ValidPersistedDocument

internal class PreparedRequest(
  val operation: GQLOperationDefinition,
  val fragments: Map<String, GQLFragmentDefinition>,
  val variables: Map<String, InternalValue>
)

/**
 * Parses and validates a document. When using persisted documents, the result of this function may be
 * cached for future reuse.
 */
internal fun validateDocument(schema: Schema, document: String): Either<List<Issue>, GQLDocument> {
  val parseResult = document.parseAsGQLDocument()
  var issues = parseResult.issues.filter { it is GraphQLIssue }
  if (issues.isNotEmpty()) {
    return issues.left()
  }

  val gqlDocument = parseResult.getOrThrow()
  val validationResult = gqlDocument.validateAsExecutable(schema)
  issues = validationResult.issues.filter { it is GraphQLIssue }
  if (issues.isNotEmpty()) {
    return issues.left()
  }

  return gqlDocument.right()
}

/**
 * Prepares a request for final execution:
 * - finds the operation
 * - extracts fragments
 * When using persisted documents, this function must be called for every request.
 *
 * @param document a validated [GQLDocument]
 * @param operationName the name of the operation to execute if any
 */
internal fun Raise<String>.prepareRequest(
  schema: Schema,
  coercings: Map<String, Coercing<*>>,
  document: GQLDocument,
  operationName: String?,
  variables: Map<String, ExternalValue>
): PreparedRequest {
  val operations = document.definitions.filterIsInstance<GQLOperationDefinition>()
  val operation = when {
    operations.isEmpty() -> {
      raise("The document does not contain any operation.")
    }

    operations.size == 1 -> {
      operations.first()
    }

    else -> {
      if (operationName == null) {
        raise("The document contains multiple operations. Use 'operationName' to indicate which one to execute.")
      }
      val ret = operations.firstOrNull { it.name == operationName }
      if (ret == null) {
        raise("No operation named '${operationName}' found. Double check operationName.")
      }
      ret
    }
  }
  val fragments = document.definitions.filterIsInstance<GQLFragmentDefinition>().associateBy { it.name }

  val variableValues = try {
    coerceVariableValues(schema = schema, operation.variableDefinitions, variables, coercings = coercings)
  } catch (e: Exception) {
    raise("Cannot coerce variable values: '${e.message}'")
  }
  return PreparedRequest(operation, fragments, variableValues)
}

/**
 * Returns a [com.apollographql.apollo.execution.PersistedDocument]. If no cache is configured, a new [com.apollographql.apollo.execution.PersistedDocument] is computed for each request.
 */
internal fun Raise<String>.getPersistedDocument(schema: Schema, persistedDocumentCache: PersistedDocumentCache?, request: GraphQLRequest): PersistedDocument {
  val persistedQuery = request.extensions.get("persistedQuery")
  var persistedDocument: PersistedDocument?
  if (persistedQuery != null) {
    if (persistedDocumentCache == null) {
      raise("PersistedQueryNotSupported")
    }

    if (persistedQuery !is Map<*, *>) {
      raise("Expected 'persistedQuery' to be an object.")
    }

    persistedQuery as Map<String, Any?>

    val id = persistedQuery.get("sha256Hash") as? String

    if (id == null) {
      raise("'persistedQuery.sha256Hash' not found or not a string.")
    }

    persistedDocument = persistedDocumentCache.get(id)
    if (persistedDocument == null) {
      if (request.document == null) {
        raise("PersistedQueryNotFound")
      }

      persistedDocument = validateDocument(schema, request.document).toPersistedDocument()

      /**
       * Note this code trusts the client for the id. Given that APQs are not a security
       * feature, I'm assuming this is OKAY. If not, change this
       */
      persistedDocumentCache.put(id, persistedDocument)
    }
  } else {
    if (request.document == null) {
      raise("no GraphQL document found")
    }
    persistedDocument = validateDocument(schema, request.document).toPersistedDocument()
  }

  return persistedDocument
}

private fun Either<List<Issue>, GQLDocument>.toPersistedDocument() = fold(
  ifLeft = {
    ErrorPersistedDocument(it)
  },
  ifRight = {
    ValidPersistedDocument(it)
  }
)

internal fun Raise<List<Error>>.prepareRequest(
  schema: Schema,
  coercings: Map<String, Coercing<*>>,
  persistedDocumentCache: PersistedDocumentCache?,
  request: GraphQLRequest
): PreparedRequest {
  val persistedDocument = withError({
    singleGraphQLError(it)
  }) {
    getPersistedDocument(schema, persistedDocumentCache, request)
  }

  if (persistedDocument is ErrorPersistedDocument) {
    raise(persistedDocument.issues.toErrors())
  }

  check(persistedDocument is ValidPersistedDocument)

  return withError({
    singleGraphQLError(it)
  }) {
    prepareRequest(schema, coercings, persistedDocument.document, request.operationName, request.variables)
  }
}

internal fun prepareRequest(
  schema: Schema,
  coercings: Map<String, Coercing<*>>,
  persistedDocumentCache: PersistedDocumentCache?,
  request: GraphQLRequest
): Either<List<Error>, PreparedRequest> = either {
  prepareRequest(schema, coercings, persistedDocumentCache, request)
}