package com.apollographql.apollo3.execution

import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.json.MapJsonWriter
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo3.ast.GQLListType
import com.apollographql.apollo3.ast.GQLNamedType
import com.apollographql.apollo3.ast.GQLNonNullType
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.GQLSelection
import com.apollographql.apollo3.ast.GQLType
import com.apollographql.apollo3.ast.GQLUnionTypeDefinition
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.definitionFromScope
import com.apollographql.apollo3.ast.responseName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

private object UnusedRoot

internal class OperationExecutor(
    val operation: GQLOperationDefinition,
    val fragments: Map<String, GQLFragmentDefinition>,
    val executionContext: ExecutionContext,
    val variables: Map<String, Any?>,
    val schema: Schema,
    val mainResolver: MainResolver,
    val adapters: CustomScalarAdapters,
    val instrumentations: List<Instrumentation>,
    val roots: Roots,
) {

  private var errors = mutableListOf<Error>()
  private val introspectionResolvers = IntrospectionResolvers(schema)

  fun execute(): GraphQLResponse {
    val operationDefinition = operation
    val rootTypename = schema.rootTypeNameOrNullFor(operationDefinition.operationType)
    if (rootTypename == null) {
      return errorResponse("'${operationDefinition.operationType}' is not supported")
    }
    val rootObject = when (operationDefinition.operationType) {
      "query" -> roots.query()
      "mutation" -> roots.mutation()
      "subscription" -> error("Use executeSubscription() to execute subscriptions")
      else -> error("Unknown operation type '${operationDefinition.operationType}")
    }
    val data = adaptToJson(emptyList(), rootObject ?: UnusedRoot, GQLNamedType(null, rootTypename), operationDefinition.selections)

    return GraphQLResponse(data, errors, null)
  }

  private fun Resolver.tryResolve(path: List<Any>, resolveInfo: ResolveInfo): Any? {
    return try {
      resolve(resolveInfo)
    } catch (e: Exception) {
      errors.add(
          Error.Builder("Cannot resolve '${resolveInfo.fieldName}': ${e.message}")
              .path(path)
              .build()
      )
      null
    }
  }

  private fun errorFlow(message: String): Flow<SubscriptionItem> {
    return flowOf(SubscriptionItemError(Error.Builder(message).build()))
  }

  fun executeSubscription(): Flow<SubscriptionItem> {
    val operationDefinition = operation
    val rootTypename = schema.rootTypeNameOrNullFor(operationDefinition.operationType)
    if (rootTypename == null) {
      return errorFlow("'${operationDefinition.operationType}' is not supported")
    }
    val rootObject = when (operationDefinition.operationType) {
      "subscription" -> roots.subscription()
      else -> return errorFlow("Unknown operation type '${operationDefinition.operationType}.")
    }

    val field = operationDefinition.selections.singleOrNull() as? GQLField
    if (field == null) {
      return errorFlow("Subscriptions must have a single root field")
    }

    val resolveInfo = ResolveInfo(
        rootObject ?: UnusedRoot,
        executionContext,
        MergedField(field, selections = field.selections),
        schema = schema,
        variables = variables,
        adapters = adapters,
        parentType = rootTypename
    )

    instrumentations.forEach { it.beforeResolve(resolveInfo) }
    val flow = try {
      mainResolver.resolve(resolveInfo)
    } catch (e: Exception) {
      return errorFlow(e.message ?: "Cannot resolve root field")
    }

    if (flow == null) {
      return errorFlow("root subscription field returned null")
    }

    if (flow !is Flow<*>) {
      error("Subscription resolvers must return a Flow<> for root fields")
    }

    val definition = field.definitionFromScope(schema, rootTypename)!!

    return flow.map {
      errors.clear()
      val path = listOf(field.responseName())

      val data = try {
        mapOf(field.responseName() to adaptToJson(path, it, definition.type, field.selections))
      } catch (e: UnexpectedNull) {
        null
      }
      GraphQLResponse(data = data, errors = errors, extensions = null)
    }.map {
      @Suppress("USELESS_CAST")
      SubscriptionItemResponse(it) as SubscriptionItem
    }.catch {
      emit(SubscriptionItemError(Error.Builder(it.message ?: "Error executing subscription").build()))
    }
  }

  private fun resolveObject(
      path: List<Any>,
      selections: List<GQLSelection>,
      parentObject: Any,
      knownTypename: String?,
  ): Map<String, Any?>? {
    val typename = knownTypename ?: introspectionResolvers.typename(parentObject) ?: mainResolver.typename(parentObject)
    if (typename == null) {
      errors.add(
          Error.Builder("Cannot determine typename of $parentObject")
              .build()
      )
      return null
    }
    val mergedFields = collectAndMergeFields(selections, typename)
    return mergedFields.associate { mergedField ->
      val responseName = mergedField.first.responseName()
      val fieldPath = path + responseName


      val resolveInfo = ResolveInfo(
          parentObject = parentObject,
          executionContext = executionContext,
          field = mergedField,
          schema = schema,
          variables = variables,
          adapters = adapters,
          parentType = typename
      )

      instrumentations.forEach { it.beforeResolve(resolveInfo) }


      val ret = when {
        resolveInfo.fieldName == "__typename" -> typename
        else -> {
          val resolver = introspectionResolvers.resolver(typename, mergedField.first.name) ?: mainResolver
          resolver.tryResolve(fieldPath, resolveInfo)
        }
      }

      val type = mergedField.first.definitionFromScope(schema, typename)
          ?: error("No field definition for $responseName")
      responseName to adaptToJson(fieldPath, ret, type.type, mergedField.selections)
    }
  }

  object UnexpectedNull : Exception("A resolver returned null in a non-null position")

  private fun adaptToJson(path: List<Any>, value: Any?, type: GQLType, selections: List<GQLSelection>): Any? {
    return when (type) {
      is GQLNonNullType -> {
        if (value == null) {
          throw UnexpectedNull
        } else {
          adaptToJson(path, value, type.type, selections)
        }
      }

      is GQLListType -> {
        if (value == null) {
          value
        } else {
          check(value is List<*>)
          try {
            value.mapIndexed { index, any ->
              adaptToJson(path + index, any, type.type, selections)
            }
          } catch (e: UnexpectedNull) {
            null
          }
        }
      }

      is GQLNamedType -> {
        if (value == null) {
          null
        } else {
          when (val typeDefinition = schema.typeDefinition(type.name)) {
            is GQLObjectTypeDefinition,
            is GQLInterfaceTypeDefinition,
            is GQLUnionTypeDefinition,
            -> {
              val knownTypename = if (typeDefinition is GQLObjectTypeDefinition) {
                typeDefinition.name
              } else {
                null
              }
              try {
                resolveObject(path, selections, value, knownTypename)
              } catch (e: UnexpectedNull) {
                null
              }
            }

            else -> {
              val adapter = adapters.adapterFor<Any>(type.name)
              if (adapter != null) {
                val writer = MapJsonWriter()
                adapter.toJson(writer, CustomScalarAdapters.Empty, value)
                writer.root()
              } else {
                value
              }
            }
          }
        }
      }
    }
  }

  private fun collectAndMergeFields(selections: List<GQLSelection>, typename: String): Collection<MergedField> {
    return collectFields(selections, typename).groupBy {
      it.responseName()
    }.mapValues {
      val fieldsWithSameResponseName = it.value
      MergedField(
          fieldsWithSameResponseName.first(),
          fieldsWithSameResponseName.flatMap {
            it.selections
          }
      )
    }.values
  }

  private fun collectFields(
      selections: List<GQLSelection>,
      typeName: String,
  ): List<GQLField> {
    return selections.flatMap {
      when (it) {
        is GQLField -> listOf(it)
        is GQLInlineFragment -> {
          if (it.typeCondition?.name?.let { schema.possibleTypes(it).contains(typeName) } ?: true) {
            collectFields(it.selections, typeName)
          } else {
            emptyList()
          }
        }

        is GQLFragmentSpread -> {
          val fragmentDefinition = fragments.get(it.name)!!
          if (fragmentDefinition.typeCondition.name.let { schema.possibleTypes(it).contains(typeName) }) {
            collectFields(fragmentDefinition.selections, typeName)
          } else {
            emptyList()
          }
        }
      }
    }
  }
}
