package com.apollographql.apollo3.execution

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.GraphQLIssue
import com.apollographql.apollo3.ast.Issue
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.toGQLDocument
import com.apollographql.apollo3.ast.toSchema
import com.apollographql.apollo3.ast.validateAsExecutable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Suppress("UNCHECKED_CAST")
@OptIn(ApolloExperimental::class)
class ExecutableSchema internal constructor(
    private val schema: Schema,
    private val persistedDocumentCache: PersistedDocumentCache?,
    private val instrumentations: List<Instrumentation>,
    private val mainResolver: MainResolver,
    private val adapterRegistry: CustomScalarAdapters,
    private val roots: Roots,
) {

  class Builder {
    private var persistedDocumentCache: PersistedDocumentCache? = null
    private var instrumentations = mutableListOf<Instrumentation>()
    private var resolver: MainResolver? = null
    private var adapterRegistry: CustomScalarAdapters? = null
    private var schema: Schema? = null
    private var roots: Roots? = null

    fun persistedDocumentCache(persistedDocumentCache: PersistedDocumentCache?): Builder = apply {
      this.persistedDocumentCache = persistedDocumentCache
    }

    fun addInstrumentation(instrumentation: Instrumentation): Builder = apply {
      this.instrumentations.add(instrumentation)
    }

    fun adapterRegistry(adapters: CustomScalarAdapters): Builder = apply {
      this.adapterRegistry = adapters
    }

    fun schema(schema: Schema): Builder = apply {
      this.schema = schema
    }

    fun schema(schema: String): Builder = apply {
      this.schema = schema.toGQLDocument().toSchema()
    }

    fun resolver(mainResolver: MainResolver): Builder = apply {
      this.resolver = mainResolver
    }

    fun roots(roots: Roots): Builder = apply {
      this.roots = roots
    }

    fun build(): ExecutableSchema {
      return ExecutableSchema(
          schema ?: error("A schema is required to build an ExecutableSchema"),
          persistedDocumentCache,
          instrumentations,
          resolver ?: ThrowingResolver,
          adapterRegistry ?: CustomScalarAdapters.Empty,
          roots ?: NullRoots
      )
    }
  }

  @OptIn(ApolloInternal::class)
  private fun validateDocument(document: String): PersistedDocument {
    val parseResult = document.parseAsGQLDocument()
    if (parseResult.issues.any { it is GraphQLIssue }) {
      return PersistedDocument(null, parseResult.issues)
    }

    val gqlDocument = parseResult.getOrThrow()
    val validationResult = gqlDocument.validateAsExecutable(schema)
    if (validationResult.issues.any { it is GraphQLIssue }) {
      return PersistedDocument(null, validationResult.issues)
    }

    return PersistedDocument(gqlDocument, emptyList())
  }

  internal sealed interface DocumentResult
  internal class DocumentError(val response: GraphQLResponse) : DocumentResult
  internal class DocumentSuccess(val document: GQLDocument) : DocumentResult

  private fun getValidatedDocument(request: GraphQLRequest): DocumentResult {
    val persistedQuery = request.extensions.get("persistedQuery")
    var persistedDocument: PersistedDocument?
    if (persistedQuery != null) {
      if (persistedDocumentCache == null) {
        return DocumentError(errorResponse("PersistedQueryNotSupported"))
      }

      if (persistedQuery !is Map<*, *>) {
        return DocumentError(errorResponse("Expected 'persistedQuery' to be an object."))
      }

      persistedQuery as Map<String, Any?>

      val id = persistedQuery.get("sha256Hash") as? String

      if (id == null) {
        return DocumentError(errorResponse("'persistedQuery.sha256Hash' not found or not a string."))
      }

      persistedDocument = persistedDocumentCache.get(id)
      if (persistedDocument == null) {
        if (request.document == null) {
          return DocumentError(errorResponse("PersistedQueryNotFound"))
        }

        persistedDocument = validateDocument(request.document)

        /**
         * Note this code trusts the client for the id. Given that APQs are not a security
         * feature, I'm assuming this is OKAY. If not, change this
         */
        persistedDocumentCache.put(id, persistedDocument)
      }
    } else {
      if (request.document == null) {
        return DocumentError(errorResponse("no GraphQL document found"))
      }
      persistedDocument = validateDocument(request.document)
    }

    if (persistedDocument.issues.isNotEmpty()) {
      return DocumentError(errorResponse(persistedDocument.issues.toErrors()))
    }

    val gqlDocument = persistedDocument.document
    if (gqlDocument == null) {
      return DocumentError(errorResponse("no GraphQL document found (this is mostly an internal bug)"))
    }

    return DocumentSuccess(gqlDocument)
  }

  internal sealed interface OperationExecutorResult
  internal class OperationExecutorError(val response: GraphQLResponse) : OperationExecutorResult
  internal class OperationExecutorSuccess(val operationExecutor: OperationExecutor) : OperationExecutorResult

  private fun getOperationExecutor(request: GraphQLRequest, context: ExecutionContext): OperationExecutorResult {
    val documentResult = getValidatedDocument(request)

    if (documentResult is DocumentError) {
      return OperationExecutorError(documentResult.response)
    }

    val document = (documentResult as DocumentSuccess).document

    val operations = document.definitions.filterIsInstance<GQLOperationDefinition>()
    val operation = when {
      operations.isEmpty() -> {
        return OperationExecutorError(errorResponse("The document does not contain any operation."))
      }

      operations.size == 1 -> {
        operations.first()
      }

      else -> {
        if (request.operationName == null) {
          return OperationExecutorError(errorResponse("The document contains multiple operations. Use 'operationName' to indicate which one to execute."))
        }
        val ret = operations.firstOrNull { it.name == request.operationName }
        if (ret == null) {
          return OperationExecutorError(errorResponse("No operation named '${request.operationName}' found. Double check operationName."))
        }
        ret
      }
    }
    val fragments = document.definitions.filterIsInstance<GQLFragmentDefinition>().associateBy { it.name }

    /**
     * XXX: variables are validated down the road, together with arguments.
     * This is convenient but not spec-compliant
     */
    val variablesIncludingDefault = operation.variableDefinitions.mapNotNull {
      when {
        request.variables.containsKey(it.name) -> it.name to request.variables.get(it.name)
        it.defaultValue != null -> it.name to it.defaultValue!!.toJson(null)
        else -> null
      }
    }.toMap()

    return OperationExecutorSuccess(
        OperationExecutor(
            operation = operation,
            fragments = fragments,
            executionContext = context,
            variables = variablesIncludingDefault,
            schema = schema,
            mainResolver = mainResolver,
            adapters = adapterRegistry,
            instrumentations = instrumentations,
            roots = roots
        )
    )
  }

  fun execute(request: GraphQLRequest, context: ExecutionContext): GraphQLResponse {
    return when (val result = getOperationExecutor(request, context)) {
      is OperationExecutorError -> result.response
      is OperationExecutorSuccess -> result.operationExecutor.execute()
    }
  }

  fun executeSubscription(request: GraphQLRequest, context: ExecutionContext): Flow<SubscriptionItem> {
    return when (val result = getOperationExecutor(request, context)) {
      is OperationExecutorError -> flowOf(SubscriptionItemError(result.response.errors?.singleOrNull() ?: Error.Builder("Cannot execute operation").build()))
      is OperationExecutorSuccess -> result.operationExecutor.executeSubscription()
    }
  }

  private fun errorResponse(errors: List<Error>): GraphQLResponse {
    return GraphQLResponse(null, errors, null)
  }

  private fun List<Issue>.toErrors(): List<Error> {
    return map {
      Error.Builder(
          message = it.message,
      ).locations(
          listOf(Error.Location(it.sourceLocation!!.line, it.sourceLocation!!.column))
      ).build()
    }
  }
}

internal fun errorResponse(message: String): GraphQLResponse {
  return GraphQLResponse.Builder()
      .errors(listOf(Error.Builder(message).build()))
      .build()
}

sealed interface SubscriptionItem
class SubscriptionItemResponse(val response: GraphQLResponse): SubscriptionItem
class SubscriptionItemError(val error: Error): SubscriptionItem