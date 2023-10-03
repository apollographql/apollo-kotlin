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

private object UnusedRoot

internal class OperationExecutor(
    val fragments: Map<String, GQLFragmentDefinition>,
    val executionContext: ExecutionContext,
    val variables: Map<String, Any?>,
    val schema: Schema,
    val mainResolver: MainResolver,
    val adapters: CustomScalarAdapters,
    val instrumentations: List<Instrumentation>,
) {

  private var errors = mutableListOf<Error>()
  private val introspectionResolvers = IntrospectionResolvers(schema)

  fun execute(operationDefinition: GQLOperationDefinition): GraphQLResponse {

    // XXX: we should throw if it's an unsupported operation
    val rootTypename = schema.rootTypeNameFor(operationDefinition.operationType)
    val rootObject = when (operationDefinition.operationType) {
      "query" -> mainResolver.rootQueryObject()
      "mutation" -> mainResolver.rootMutationObject()
      "subscription" -> mainResolver.rootSubscriptionObject()
      else -> error("Unsupported operation type '$mainResolver.rootQueryObject()'")
    }
    val data = resolveObject(operationDefinition.selections, rootObject ?: UnusedRoot, rootTypename)

    return GraphQLResponse(data, errors, null)
  }

  private fun resolveObject(selections: List<GQLSelection>, parentObject: Any, knownTypename: String?): Map<String, Any?>? {
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
          try {
            resolver.resolve(resolveInfo)
          } catch (e: Exception) {
            errors.add(
                Error.Builder("No resolver for $typename.${mergedField.first.name}")
                    .build()
            )
            null
          }
        }
      }

      val type = mergedField.first.definitionFromScope(schema, typename)
          ?: error("No field definition for $responseName")
      responseName to adaptToJson(ret, type.type, mergedField.selections)
    }
  }

  private fun adaptToJson(value: Any?, type: GQLType, selections: List<GQLSelection>): Any? {
    return when (type) {
      is GQLNonNullType -> {
        check(value != null)
        adaptToJson(value, type.type, selections)
      }

      is GQLListType -> {
        if (value == null) {
          value
        } else {
          check(value is List<*>)
          value.map { adaptToJson(it, type.type, selections) }
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
              resolveObject(selections, value, knownTypename)
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
