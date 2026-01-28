package com.apollographql.apollo.execution

import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.ast.*
import com.apollographql.apollo.execution.internal.OperationContext
import com.apollographql.apollo.execution.internal.PreparedRequest
import com.apollographql.apollo.execution.internal.introspectionCoercings
import com.apollographql.apollo.execution.internal.introspectionResolver
import com.apollographql.apollo.execution.internal.prepareRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * A GraphQL schema with execution information:
 * - root values
 * - coercings
 * - resolvers
 *
 * [ExecutableSchema] also includes handling for persisted documents. This part is not technically part of the main GraphQL spec but is popular that we add first party support to it.
 */
class ExecutableSchema internal constructor(
    private val schema: Schema,
    private val coercings: Map<String, Coercing<*>>,
    private val queryRoot: RootResolver?,
    private val mutationRoot: RootResolver?,
    private val subscriptionRoot: RootResolver?,
    private val resolver: Resolver,
    private val typeResolver: TypeResolver,
    private val instrumentations: List<Instrumentation>,
    private val persistedDocumentCache: PersistedDocumentCache?,
    private val onError: OnError,
) {
  private val introspectionResolver: Resolver = introspectionResolver(schema)

  suspend fun execute(
      request: GraphQLRequest,
      executionContext: ExecutionContext = ExecutionContext.Empty,
  ): GraphQLResponse {
    return prepareRequest(schema, coercings, persistedDocumentCache, request).fold(
        ifLeft = {
          GraphQLResponse.Builder().errors(it).build()
        },
        ifRight = {
          operationContext(it, executionContext).execute()
        }
    )
  }

  fun subscribe(
      request: GraphQLRequest,
      executionContext: ExecutionContext = ExecutionContext.Empty,
  ): Flow<SubscriptionEvent> {
    return prepareRequest(schema, coercings, persistedDocumentCache, request).fold(
        ifLeft = {
          flowOf(SubscriptionResponse(GraphQLResponse.Builder().errors(it).build()))
        },
        ifRight = {
          operationContext(it, executionContext).subscribe()
        }
    )
  }

  private fun operationContext(preparedRequest: PreparedRequest, executionContext: ExecutionContext): OperationContext {
    return OperationContext(
        schema,
        coercings + introspectionCoercings,
        introspectionResolver,
        queryRoot,
        mutationRoot,
        subscriptionRoot,
        resolver,
        typeResolver,
        instrumentations,
        preparedRequest.operation,
        preparedRequest.fragments,
        preparedRequest.variables,
        executionContext,
        preparedRequest.onError ?: onError
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

    fun build(): ExecutableSchema {
      check(schema != null) {
        "A schema is required to build an ExecutableSchema"
      }
      /**
       * TODO: scalar definitions are added back when calling `toSchema()` but I'm unclear why we have to filter them out here.
       */
      val ourDefinitions = builtinDefinitions().filter { it !is GQLScalarTypeDefinition } + serviceDefinition(onError)
      val reservedNames = ourDefinitions.mapNotNull { it.definitionName() }.toSet()
      val sourceDefinitions = schema!!.definitions
      sourceDefinitions.forEach {
        val definitionName = it.definitionName()
        if (definitionName in reservedNames) {
          error("Source schema cannot contain definition '$definitionName'. It is provided by the implementation")
        }
      }
      val schema = GQLDocument(ourDefinitions + schema!!.definitions, null).toSchema()

      return ExecutableSchema(
          schema,
          coercings,
          queryRoot,
          mutationRoot,
          subscriptionRoot,
          resolver ?: ThrowingResolver,
          typeResolver ?: ThrowingTypeResolver,
          instrumentations,
          persistedDocumentCache,
          onError
      )
    }
  }
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