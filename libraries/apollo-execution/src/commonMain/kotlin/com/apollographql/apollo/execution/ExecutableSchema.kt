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
 * - resolver
 *
 * [ExecutableSchema] also includes handling for persisted documents. This part is not technically part of the main GraphQL spec but is popular that we add first party support to it.
 */
class ExecutableSchema(
  private val schema: Schema,
  private val coercings: Map<String, Coercing<*>>,
  private val queryRoot: RootResolver?,
  private val mutationRoot: RootResolver?,
  private val subscriptionRoot: RootResolver?,
  private val resolver: Resolver,
  private val typeResolver: TypeResolver,
  private val instrumentations: List<Instrumentation>,
  private val persistedDocumentCache: PersistedDocumentCache?,
) {
  private val introspectionResolver: Resolver = introspectionResolver(schema)

  suspend fun execute(
    request: GraphQLRequest,
    executionContext: ExecutionContext = ExecutionContext.Empty
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
    executionContext: ExecutionContext = ExecutionContext.Empty
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

    fun build(): ExecutableSchema {
      val definitions = builtinDefinitions().filter { it !is GQLScalarTypeDefinition } + (schema?.definitions
        ?: error("A schema is required to build an ExecutableSchema"))
      val schema = GQLDocument(definitions, null).toSchema()

      return ExecutableSchema(
        schema,
        coercings,
        queryRoot,
        mutationRoot,
        subscriptionRoot,
        resolver ?: ThrowingResolver,
        typeResolver ?: ThrowingTypeResolver,
        instrumentations,
        persistedDocumentCache
      )
    }
  }
}

