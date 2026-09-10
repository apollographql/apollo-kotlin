package com.apollographql.apollo.execution

import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.OnError
import com.apollographql.apollo.ast.GQLArgument
import com.apollographql.apollo.ast.GQLCapability
import com.apollographql.apollo.ast.GQLDefinition
import com.apollographql.apollo.ast.GQLDirectiveDefinition
import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLListType
import com.apollographql.apollo.ast.GQLSchemaDefinition
import com.apollographql.apollo.ast.GQLServiceDefinition
import com.apollographql.apollo.ast.GQLStringValue
import com.apollographql.apollo.ast.GQLTypeDefinition
import com.apollographql.apollo.ast.ParserOptions
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.builtinDefinitions
import com.apollographql.apollo.ast.fieldDefinitions
import com.apollographql.apollo.ast.rawType
import com.apollographql.apollo.ast.serviceCapabilitiesDefinitions
import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.ast.toSchema
import com.apollographql.apollo.execution.internal.Either
import com.apollographql.apollo.execution.internal.OperationContext
import com.apollographql.apollo.execution.internal.either
import com.apollographql.apollo.execution.internal.graphqlErrorResponse
import com.apollographql.apollo.execution.internal.introspectionCoercings
import com.apollographql.apollo.execution.internal.introspectionResolver
import com.apollographql.apollo.execution.internal.prepareRequest
import com.apollographql.apollo.execution.internal.subscriptionError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * The entry point to serve GraphQL requests:
 * - validates the document (or retrieves and validates a persisted document)
 * - coerces variables
 * - executes the resulting [PreparedRequest]
 *
 * [ExecutableSchema] also includes handling for persisted documents. This part is not technically part of the main GraphQL spec, but it is popular that we add first party support to it.
 */
class ExecutableSchema internal constructor(
    private val schema: Schema,
    private val coercings: Map<String, Coercing<*>>,
    private val resolver: Resolver,
    private val typeResolver: TypeResolver,
    private val instrumentations: List<Instrumentation>,
    private val onError: OnError,
    private val queryRoot: RootResolver?,
    private val mutationRoot: RootResolver?,
    private val subscriptionRoot: RootResolver?,
    private val persistedDocumentCache: PersistedDocumentCache?,
    private val parserOptions: ParserOptions,
) {
  private val introspectionResolver: Resolver = introspectionResolver(schema)

  private fun prepareRequest(
      request: GraphQLRequest
  ) = prepareRequest(schema, coercings, persistedDocumentCache, parserOptions, request)

  suspend fun execute(
      request: GraphQLRequest,
      executionContext: ExecutionContext = ExecutionContext.Empty,
  ): GraphQLResponse {
    return prepareRequest(request).fold(
        ifLeft = {
          graphqlErrorResponse(it)
        },
        ifRight = {
          execute(it, executionContext)
        }
    )
  }

  fun subscribe(
      request: GraphQLRequest,
      executionContext: ExecutionContext = ExecutionContext.Empty,
  ): Flow<SubscriptionEvent> {
    return prepareRequest(request).fold(
        ifLeft = {
          flowOf(SubscriptionResponse(graphqlErrorResponse(it)))
        },
        ifRight = {
          subscribe(it, executionContext)
        }
    )
  }

  private fun resolveRootNoFragment(preparedRequest: PreparedRequest): Either<String, Any?> = either {
    val rootResolver = when (preparedRequest.type) {
      RequestType.Query -> queryRoot
      RequestType.Mutation -> mutationRoot
      RequestType.Subscription -> subscriptionRoot
      RequestType.Fragment -> error("resolveRoot can")
    }
    try {
      rootResolver?.resolveRoot()
    } catch (e: Exception) {
      raise("Error resolving root object: ${e.message}")
    }
  }

  private suspend fun resolveRoot(preparedRequest: PreparedRequest): Either<String, Any?> = either {
    if (preparedRequest.type == RequestType.Fragment) {
      when(preparedRequest.typename) {
        schema.queryTypeDefinition.name -> return@either queryRoot?.resolveRoot()
        schema.mutationTypeDefinition?.name -> return@either mutationRoot?.resolveRoot()
        schema.subscriptionTypeDefinition?.name -> return@either subscriptionRoot?.resolveRoot()
      }

      val id = preparedRequest.id
      if (id == null) {
        raise("Resolving a fragment on non-root types requires an 'id'.")
      }

      val root = queryRoot?.resolveRoot()

      val candidates = schema.queryTypeDefinition.fieldDefinitions(schema)
          .filter {
            if (it.type is GQLListType) {
              return@filter false
            }
            it.type.rawType().name == preparedRequest.typename && it.arguments.size == 1 && it.arguments[0].name == "id"
          }

      if (candidates.isEmpty()) {
        raise("Cannot resolve fragment: no root query field found returning '${preparedRequest.typename}' and having a single 'id' argument.")
      } else if (candidates.size > 1) {
        raise("Cannot resolve fragment: cannot disambiguate between these root fields: ${candidates.joinToString { it.name }}.")
      }

      val field = GQLField(null, null, candidates.single().name, emptyList(), emptyList(), emptyList(), false)
      try {
        resolver.resolve(ResolveInfo(root, preparedRequest.typename, ExecutionContext.Empty, listOf(field), schema, mapOf("id" to id), emptyList()))
      } catch (e: Exception) {
        raise("Cannot resolve the fragment root: ${e.message}")
      }
    } else {
      resolveRootNoFragment(preparedRequest).fold(
          ifLeft = { raise(it) },
          ifRight = { it }
      )
    }
  }

  suspend fun execute(preparedRequest: PreparedRequest, executionContext: ExecutionContext = ExecutionContext.Empty): GraphQLResponse {
    if (preparedRequest.type == RequestType.Subscription) {
      return graphqlErrorResponse("Cannot execute subscription '${preparedRequest.name}', use subscribe(), not execute() ")
    }
    return resolveRoot(preparedRequest).fold(
        ifLeft = { graphqlErrorResponse(it) },
        ifRight = { operationContext(preparedRequest, it, executionContext).execute() }
    )
  }

  fun subscribe(preparedRequest: PreparedRequest, executionContext: ExecutionContext = ExecutionContext.Empty): Flow<SubscriptionEvent> {
    if (preparedRequest.type != RequestType.Subscription) {
      return subscriptionError("Cannot subscribe to operation '${preparedRequest.name}', use execute(), not subscribe() ")
    }

    return resolveRootNoFragment(preparedRequest).fold(
        ifLeft = { subscriptionError(it) },
        ifRight = { operationContext(preparedRequest, it, executionContext).subscribe() }
    )
  }

  private fun operationContext(preparedRequest: PreparedRequest, rootObject: Any?, executionContext: ExecutionContext): OperationContext {
    return OperationContext(
        schema = schema,
        coercings = coercings + introspectionCoercings,
        introspectionResolver = introspectionResolver,
        resolver = resolver,
        typeResolver = typeResolver,
        instrumentations = instrumentations,
        rootObject = rootObject,
        rootSelections = preparedRequest.rootSelections,
        typename = preparedRequest.typename,
        fragments = preparedRequest.fragments,
        variableValues = preparedRequest.variables,
        onError = preparedRequest.onError ?: onError,
        debugName = preparedRequest.name,
        serial = preparedRequest.type == RequestType.Mutation,
        executionContext = executionContext,
    )
  }

  class Builder {
    private var schema: GQLDocument? = null
    private val coercings = mutableMapOf<String, Coercing<*>>()
    private var resolver: Resolver? = null
    private var queryRoot: RootResolver? = null
    private var mutationRoot: RootResolver? = null
    private var subscriptionRoot: RootResolver? = null
    private var typeResolver: TypeResolver? = null
    private val instrumentations = mutableListOf<Instrumentation>()
    private var persistedDocumentCache: PersistedDocumentCache? = null
    private var onError: OnError = OnError.PROPAGATE
    private var parserOptions: ParserOptions = ParserOptions.Default

    fun schema(schema: GQLDocument): Builder = apply {
      this.schema = schema
    }

    fun schema(schema: String): Builder = apply {
      schema(schema.toGQLDocument())
    }

    fun addCoercing(type: String, coercing: Coercing<*>): Builder = apply {
      this.coercings.put(type, coercing)
    }

    fun queryRoot(queryRoot: RootResolver) = apply {
      this.queryRoot = queryRoot
    }

    fun mutationRoot(mutationRoot: RootResolver) = apply {
      this.mutationRoot = mutationRoot
    }

    fun subscriptionRoot(subscriptionRoot: RootResolver) = apply {
      this.subscriptionRoot = subscriptionRoot
    }

    fun resolver(resolver: Resolver): Builder = apply {
      this.resolver = resolver
    }

    fun typeResolver(typeResolver: TypeResolver): Builder = apply {
      this.typeResolver = typeResolver
    }

    fun addInstrumentation(instrumentation: Instrumentation): Builder = apply {
      this.instrumentations.add(instrumentation)
    }

    fun persistedDocumentCache(persistedDocumentCache: PersistedDocumentCache?): Builder = apply {
      this.persistedDocumentCache = persistedDocumentCache
    }

    fun onError(onError: OnError) = apply {
      check(onError != OnError.HALT) {
        "OnError.HALT is not supported"
      }
      this.onError = onError
    }

    /**
     * Configures the parsing options for this schema.
     */
    fun parserOptions(parserOptions: ParserOptions): Builder = apply {
      this.parserOptions = parserOptions
    }

    fun build(): ExecutableSchema {
      check(schema != null) {
        "A schema is required to build an ExecutableSchema"
      }

      return ExecutableSchema(
          buildSchema(schema!!, onError),
          coercings,
          resolver ?: ThrowingResolver,
          typeResolver ?: ThrowingTypeResolver,
          instrumentations,
          onError,
          queryRoot,
          mutationRoot,
          subscriptionRoot,
          persistedDocumentCache,
          parserOptions,
      )
    }
  }
}

/**
 * Merges [document] with the definitions required to serve introspection and the `service` capabilities query,
 * and turns the result into a [Schema].
 */
internal fun buildSchema(document: GQLDocument, onError: OnError): Schema {
  val ourDefinitions = builtinDefinitions() + serviceCapabilitiesDefinitions() + serviceDefinition(onError)
  val reservedNames = ourDefinitions.mapNotNull { it.definitionName() }.toSet()
  val sourceDefinitions = document.definitions
  sourceDefinitions.forEach {
    val definitionName = it.definitionName()
    if (definitionName in reservedNames) {
      error("Source schema cannot contain definition '$definitionName'. It is provided by the implementation")
    }
  }
  return GQLDocument(ourDefinitions + document.definitions, null).toSchema()
}

private fun serviceDefinition(onError: OnError): GQLDefinition {
  return GQLServiceDefinition(
      sourceLocation = null,
      description = null,
      directives = emptyList(),
      capabilities = listOf(
          GQLCapability(description = null, name = "graphql.onError", value = null),
          GQLCapability(description = null, name = "graphql.defaultErrorBehavior", value = onError.name)
      ),
  )
}

private fun GQLDefinition.definitionName(): String? {
  return when (this) {
    is GQLTypeDefinition -> name
    is GQLDirectiveDefinition -> "@$name"
    is GQLSchemaDefinition -> "schema"
    is GQLServiceDefinition -> "service"
    else -> null
  }
}
