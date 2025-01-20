package com.apollographql.apollo.execution.internal

import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.ast.*
import com.apollographql.apollo.execution.Coercing
import com.apollographql.apollo.execution.ExternalValue
import com.apollographql.apollo.execution.ExternalValueOrDeferred
import com.apollographql.apollo.execution.FieldCallback
import com.apollographql.apollo.execution.GraphQLResponse
import com.apollographql.apollo.execution.Instrumentation
import com.apollographql.apollo.execution.InternalValue
import com.apollographql.apollo.execution.OperationCallback
import com.apollographql.apollo.execution.OperationInfo
import com.apollographql.apollo.execution.ResolveInfo
import com.apollographql.apollo.execution.ResolveTypeInfo
import com.apollographql.apollo.execution.Resolver
import com.apollographql.apollo.execution.ResolverValue
import com.apollographql.apollo.execution.ResolverValueOrError
import com.apollographql.apollo.execution.RootResolver
import com.apollographql.apollo.execution.SubscriptionError
import com.apollographql.apollo.execution.SubscriptionEvent
import com.apollographql.apollo.execution.SubscriptionResponse
import com.apollographql.apollo.execution.TypeResolver
import com.apollographql.apollo.execution.finalize
import com.apollographql.apollo.execution.leafCoercingSerialize
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Bags of constant properties used while resolving the operation.
 *
 * 2 important methods are [resolveFieldValueOrThrow] and [completeValueOrThrow]. They are the core of field resolution,
 * are suspend and may throw.
 * Exceptions may happen from resolvers, coercing, and type resolvers which are typically outside our code.
 *
 * Their counterparts [resolveFieldValue] and [completeValue] return an [Error] in case something goes wrong.
 *
 * [executeField] returns a [Deferred] that must be awaited using [finalize]
 *
 */
internal class OperationContext(
  private val schema: Schema,
  private val coercings: Map<String, Coercing<*>>,
  private val introspectionResolver: Resolver,
  private val queryRoot: RootResolver?,
  private val mutationRoot: RootResolver?,
  private val subscriptionRoot: RootResolver?,
  private val resolver: Resolver,
  private val typeResolver: TypeResolver,
  private val instrumentations: List<Instrumentation>,
  private val operation: GQLOperationDefinition,
  private val fragments: Map<String, GQLFragmentDefinition>,
  private val variableValues: Map<String, InternalValue>,
  private val executionContext: ExecutionContext,
) {
  private val bubbles: Boolean = operation.bubbles()

  /**
   * executes the given operation and awaits its result.
   *
   * Note: a future version may add an "executeAsync" function so we can start sending some data before the whole
   * map is computed.
   */
  suspend fun execute(): GraphQLResponse {
    var instrumentationException: Exception? = null
    val operationCallbacks = mutableListOf<OperationCallback>()
    val operationInfo = OperationInfo(
      operation,
      fragments,
      schema,
      executionContext
    )
    instrumentations.forEach {
      val callback = try {
        it.onOperation(operationInfo)
      } catch (e: Exception) {
        if (e is CancellationException) {
          throw e
        }
        instrumentationException = e
        return@forEach
      }
      if (callback != null) {
        operationCallbacks.add(callback)
      }
    }
    if (instrumentationException != null) {
      return graphqlErrorResponse("An error happened while instrumenting '${operation.name}': ${instrumentationException.message}")
    }
    val rootTypename = schema.rootTypeNameOrNullFor(operation.operationType)
    if (rootTypename == null) {
      return graphqlErrorResponse("'${operation.operationType}' is not supported")
    }
    val rootObject = when (operation.operationType) {
      "query" -> queryRoot?.resolveRoot()
      "mutation" -> mutationRoot?.resolveRoot()
      "subscription" -> {
        return graphqlErrorResponse("Use subscribe() to execute subscriptions")
      }

      else -> {
        return graphqlErrorResponse("Unknown operation type '${operation.operationType}")
      }
    }
    val typeDefinition = schema.typeDefinition(schema.rootTypeNameFor(operation.operationType))

    val groupedFieldSet = collectFields(typeDefinition.name, operation.selections, variableValues)

    return coroutineScope {
      async(start = CoroutineStart.UNDISPATCHED) {
        executeGroupedFieldSet(
          this,
          groupedFieldSet,
          typeDefinition as GQLObjectTypeDefinition,
          rootObject,
          variableValues,
          emptyList(),
          operation.operationType == "mutation"
        )
      }.toGraphQLResponse(callbacks = operationCallbacks)
    }
  }

  fun subscribe(): Flow<SubscriptionEvent> {
    val rootObject = when (operation.operationType) {
      "subscription" -> subscriptionRoot?.resolveRoot()
      else -> return subscriptionError("Unknown operation type '${operation.operationType}.")
    }

    val eventStream = try {
      createSourceEventStream(operation, rootObject)
    } catch (e: Exception) {
      return subscriptionError("Cannot create source event stream: ${e.message}")
    }
    return mapSourceToResponseEvent(eventStream, variableValues).catch {
      emit(SubscriptionError(listOf(Error.Builder("Error collecting the source event stream: ${it.message}").build())))
    }
  }

  internal suspend fun ExternalValueOrDeferred.toGraphQLResponse(callbacks: List<OperationCallback>): GraphQLResponse {
    val errors = mutableListOf<Error>()

    val data = this.finalize(errors)

    val response = GraphQLResponse(data, errors.ifEmpty { null }, null)

    return callbacks.fold(response) { acc, instrumentation ->
      instrumentation.onOperationCompleted(acc)
    }
  }

  private fun subscriptionError(message: String): Flow<SubscriptionError> {
    return flowOf(SubscriptionError(listOf(Error.Builder(message).build())))
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun resolveFieldEventStream(
    subscriptionType: GQLObjectTypeDefinition,
    rootValue: ResolverValue,
    fields: List<GQLField>,
    argumentValues: Map<String, InternalValue>,
    responseName: String
  ): Flow<FieldEvent> {
    return flow {
      val resolveInfo = ResolveInfo(
        parentObject = rootValue,
        executionContext = executionContext,
        fields = fields,
        schema = schema,
        arguments = argumentValues,
        parentType = subscriptionType.name,
        path = emptyList()
      )

      emit(resolveFieldValue(resolveInfo))
    }.flatMapConcat {
      if (it !is Flow<*>) {
        flowOf(FieldEventError("Subscription resolvers must return a Flow<> (got '$it')"))
      } else {
        it.map { objectValue ->
          FieldEventItem(
            parentType = subscriptionType.name,
            objectValue = objectValue,
            fields = fields,
            responseName = responseName
          )
        }
      }
    }
  }

  sealed interface FieldEvent
  private class FieldEventItem(
    val parentType: String,
    val objectValue: InternalValue,
    val fields: List<GQLField>,
    val responseName: String,
  ) : FieldEvent

  private class FieldEventError(
    val message: String,
  ) : FieldEvent

  private fun createSourceEventStream(
    subscription: GQLOperationDefinition,
    rootValue: ResolverValue
  ): Flow<FieldEvent> {
    val rootTypename = schema.rootTypeNameOrNullFor(subscription.operationType)
    if (rootTypename == null) {
      return flowOf(FieldEventError("'${subscription.operationType}' is not supported"))
    }

    val typeDefinition = schema.typeDefinition(rootTypename)
    check(typeDefinition is GQLObjectTypeDefinition) {
      "Root typename '${typeDefinition.name} must be of object type"
    }
    val selections = subscription.selections
    val groupedFieldsSet = collectFields(typeDefinition.name, selections, variableValues)
    check(groupedFieldsSet.size == 1) {
      return flowOf(FieldEventError("Subscriptions must have a single root field"))
    }
    val fields = groupedFieldsSet.entries.single().value
    val field = fields.first()
    val argumentValues = coerceArgumentValues(schema, typeDefinition.name, field, coercings, variableValues)
    return resolveFieldEventStream(
      subscriptionType = typeDefinition,
      rootValue = rootValue,
      fields = fields,
      argumentValues = argumentValues,
      responseName = fields.first().responseName()
    )
  }

  private fun mapSourceToResponseEvent(
    sourceStream: Flow<FieldEvent>,
    variableValues: Map<String, ExternalValue>
  ): Flow<SubscriptionEvent> {
    return sourceStream.map {
      // TODO: allow implementers to terminate the stream with an exception
      SubscriptionResponse(executeSubscriptionEvent(it))
    }
  }

  private suspend fun executeSubscriptionEvent(
    event: FieldEvent,
  ): GraphQLResponse {
    return when (event) {
      is FieldEventError -> GraphQLResponse.Builder().errors(listOf(Error.Builder(event.message).build())).build()
      is FieldEventItem -> {
        coroutineScope {
          val fieldData = completeValue(
            scope = this,
            fieldType = event.fields.first().definitionFromScope(schema, event.parentType)!!.type,
            fields = event.fields,
            result = event.objectValue,
            path = listOf(event.responseName)
          )

          mapOf(event.responseName to fieldData).toGraphQLResponse(emptyList())
        }
      }
    }
  }

  /**
   * executes the given field.
   *
   * @param scope a scope where to execute asynchronous work.
   * @param objectType the parent type of the field. Always a concrete object type.
   * @param objectValue the parent object as returned from a resolver.
   * @param fieldType the field type as in the schema field definition.
   * @param fields the merged fields
   * @param variableValues the coerced variable values.
   */
  private fun executeField(
    scope: CoroutineScope,
    objectType: GQLObjectTypeDefinition,
    objectValue: ResolverValue,
    fieldType: GQLType,
    fields: List<GQLField>,
    variableValues: Map<String, InternalValue>,
    path: List<Any>,
  ): Deferred<ExternalValue> {
    val field = fields.first()
    val argumentValues = coerceArgumentValues(schema, objectType.name, field, coercings, variableValues)

    return scope.async(start = CoroutineStart.UNDISPATCHED) {
      val resolveInfo = ResolveInfo(
        parentObject = objectValue,
        executionContext = executionContext,
        fields = fields,
        schema = schema,
        arguments = argumentValues,
        parentType = objectType.name,
        path = path,
      )

      val fieldCallbacks = mutableListOf<FieldCallback>()
      var instrumentationError: Error? = null
      instrumentations.map {
        try {
          val callback = it.onField(resolveInfo)
          if (callback != null) {
            fieldCallbacks.add(callback)
          }
        } catch (e: Exception) {
          if (e is CancellationException) {
            throw e
          }
          instrumentationError = Error.Builder("Cannot instrument '${path.lastOrNull()}': ${e.message}")
            .path(path)
            .build()
        }
      }

      val completedValue = if (instrumentationError == null) {
        val resolvedValue = resolveFieldValue(resolveInfo)
        completeValue(
          scope = scope,
          fieldType = fieldType,
          fields = fields,
          result = resolvedValue,
          path = path
        )
      } else {
        instrumentationError
      }
      fieldCallbacks.forEach {
        it.onFieldCompleted(completedValue)
      }
      completedValue
    }
  }

  private suspend fun completeValue(
    scope: CoroutineScope,
    fieldType: GQLType,
    fields: List<GQLField>,
    result: ResolverValue,
    path: List<Any>
  ): ExternalValue {
    return runFieldOrError(path) {
      completeValueOrThrow(
        scope,
        fieldType,
        fields,
        result,
        path
      )
    }
  }

  /**
   * @throws IllegalStateException if coercing fails.
   * @throws Exception if [typeResolver] fails.
   */
  private suspend fun completeValueOrThrow(
    scope: CoroutineScope,
    fieldType: GQLType,
    fields: List<GQLField>,
    result: ResolverValue,
    path: List<Any>
  ): ExternalValue {
    if (result is Error) {
      // fast path if the resolver failed
      return result
    }

    if (fieldType is GQLNonNullType) {
      val completedResult = completeValue(scope, fieldType.type, fields, result, path)
      if (completedResult == null) {
        return Error.Builder("A resolver returned null in a non-nullable position")
          .path(path)
          .build()
      }
      return completedResult
    }

    if (result == null) {
      return result
    }

    if (fieldType is GQLListType) {
      if (result !is List<*>) {
        return Error.Builder("A resolver returned non-list in a list position")
          .path(path)
          .build()
      }

      val deferred = result.mapIndexed { index, item ->
        scope.async(start = CoroutineStart.UNDISPATCHED) {
          completeValue(scope, fieldType.type, fields, item, path + index)
        }
      }
      val list = deferred.map {
        val completed = it.await()
        if (bubbles && completed is Error && fieldType.type is GQLNonNullType) {
          /**
           * We got an error in non-null position, return early
           * TODO: cancel other deferred items
           */
          return completed
        }
        completed
      }
      return list
    }

    fieldType as GQLNamedType
    val typeDefinition = schema.typeDefinition(fieldType.name)
    return when (typeDefinition) {
      is GQLEnumTypeDefinition,
      is GQLScalarTypeDefinition,
        -> {
        // leaf type
        leafCoercingSerialize(result, coercings, typeDefinition)
      }

      is GQLInterfaceTypeDefinition,
      is GQLObjectTypeDefinition,
      is GQLUnionTypeDefinition,
        -> {
        val typename = if (typeDefinition is GQLObjectTypeDefinition) {
          typeDefinition.name
        } else {
          typeResolver.resolveType(result, ResolveTypeInfo(typeDefinition.name, schema))
        }

        val selections = fields.flatMap { it.selections }
        val groupedFieldSet = collectFields(typename, selections, variableValues)
        return executeGroupedFieldSet(
          scope = scope,
          groupedFieldSet = groupedFieldSet,
          typeDefinition = schema.typeDefinition(typename) as GQLObjectTypeDefinition,
          objectValue = result,
          variableValues = variableValues,
          path = path,
          serial = false,
        )
      }

      is GQLInputObjectTypeDefinition -> {
        return Error.Builder("Input type used in output position")
          .path(path)
          .build()
      }
    }
  }

  private suspend fun runFieldOrError(
    path: List<Any>,
    block: suspend () -> Any?
  ): Any? {
    return try {
      block()
    } catch (e: Exception) {
      if (e is CancellationException) {
        throw e
      }
      Error.Builder("Cannot resolve '${path.lastOrNull()}': ${e.message}")
        .path(path)
        .build()
    }
  }

  private suspend fun resolveFieldValue(
    resolveInfo: ResolveInfo,
  ): ResolverValueOrError {
    return runFieldOrError(resolveInfo.path) {
      resolveFieldValueOrThrow(resolveInfo)
    }
  }

  /**
   * Calls the resolver.
   *
   * @throws [Exception] when the resolver throws.
   */
  private suspend fun resolveFieldValueOrThrow(
    resolveInfo: ResolveInfo
  ): ResolverValue {
    val resolver = when {
      resolveInfo.fieldName.startsWith("__") -> introspectionResolver
      resolveInfo.parentType.startsWith("__") -> introspectionResolver
      else -> resolver
    }
    return resolver.resolve(resolveInfo)
  }

  private class Entry(
    val key: String,
    val value: Deferred<ExternalValue>,
    val nullable: Boolean
  )

  private suspend fun executeGroupedFieldSet(
    scope: CoroutineScope,
    groupedFieldSet: Map<String, List<GQLField>>,
    typeDefinition: GQLObjectTypeDefinition,
    objectValue: ResolverValue,
    variableValues: Map<String, InternalValue>,
    path: List<Any>,
    serial: Boolean
  ): ExternalValue {
    val typename = typeDefinition.name
    val entries = groupedFieldSet.entries.map { entry ->
      val field = entry.value.first()
      val fieldDefinition = field.definitionFromScope(schema, typename)!!
      val fieldPath = path + field.responseName()

      val deferred =
        executeField(scope, typeDefinition, objectValue, fieldDefinition.type, entry.value, variableValues, fieldPath)
      if (serial) {
        deferred.await()
      }
      Entry(entry.key, deferred, fieldDefinition.type !is GQLNonNullType)
    }

    val result = mutableMapOf<String, ExternalValue>()
    entries.forEach {
      val value = it.value.await()
      if (bubbles && value is Error && !it.nullable) {
        return value
      }

      result.put(it.key, it.value)
    }

    return result
  }

  /**
   * Assumes validation and or variable coercion caught errors, crashes else.
   */
  private fun GQLDirective.singleRequiredBooleanArgumentValue(coercedVariables: Map<String, InternalValue>): Boolean {
    val value = arguments.single().value
    return when (value) {
      is GQLBooleanValue -> value.value
      is GQLVariableValue -> {
        // If the variable is absent or not a boolean, it should have failed during coercion
        coercedVariables.get(value.name) as Boolean
      }

      else -> error("Cannot get argument value for directive '$name'")
    }
  }

  private fun List<GQLDirective>.shouldSkip(coercedVariables: Map<String, InternalValue>): Boolean {
    forEach {
      if (it.name == "skip") {
        if (it.singleRequiredBooleanArgumentValue(coercedVariables)) {
          return true
        }
      }
    }
    forEach {
      if (it.name == "include") {
        if (!it.singleRequiredBooleanArgumentValue(coercedVariables)) {
          return true
        }
      }
    }
    return false
  }

  private fun <K, V> MutableMap<K, V>.update(key: K, update: (prev: V?) -> V) {
    val newValue = if (this.containsKey(key)) {
      update(this.get(key))
    } else {
      update(null)
    }
    put(key, newValue)
  }

  private fun collectFields(
    objectType: String,
    selections: List<GQLSelection>,
    coercedVariables: Map<String, InternalValue>,
  ): Map<String, List<GQLField>> {
    val groupedFields = mutableMapOf<String, List<GQLField>>()
    collectFields(objectType, selections, coercedVariables, mutableSetOf(), groupedFields)
    return groupedFields
  }

  private fun collectFields(
    objectType: String,
    selections: List<GQLSelection>,
    coercedVariables: Map<String, InternalValue>,
    visitedFragments: MutableSet<String>,
    groupedFields: MutableMap<String, List<GQLField>>,
  ) {
    selections.forEach { selection ->
      if (selection.directives.shouldSkip(coercedVariables)) {
        return@forEach
      }
      when (selection) {
        is GQLField -> {
          groupedFields.update(selection.responseName()) {
            (it.orEmpty()).plus(selection)
          }
        }

        is GQLFragmentSpread -> {
          if (visitedFragments.contains(selection.name)) {
            return@forEach
          }
          visitedFragments.add(selection.name)

          val fragmentDefinition = fragments.get(selection.name)!!

          if (schema.possibleTypes(fragmentDefinition.typeCondition.name).contains(objectType)) {
            collectFields(objectType, fragmentDefinition.selections, coercedVariables, visitedFragments, groupedFields)
          }
        }

        is GQLInlineFragment -> {
          val typeCondition = selection.typeCondition?.name
          if (typeCondition == null || schema.possibleTypes(typeCondition).contains(objectType)) {
            collectFields(objectType, selection.selections, coercedVariables, visitedFragments, groupedFields)
          }
        }
      }
    }
  }
}

private fun <E> List<E>.orNullIfEmpty(): List<E>? {
  return this.ifEmpty {
    null
  }
}

private val GQLSelection.directives: List<GQLDirective>
  get() = when (this) {
    is GQLField -> directives
    is GQLFragmentSpread -> directives
    is GQLInlineFragment -> directives
  }

private fun GQLOperationDefinition.bubbles(): Boolean {
  return !directives.any { it.name == "noBubblesPlz" }
}
