package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.ast.GQLEnumTypeDefinition
import com.apollographql.apollo.ast.GQLFieldDefinition
import com.apollographql.apollo.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo.ast.GQLListType
import com.apollographql.apollo.ast.GQLNamedType
import com.apollographql.apollo.ast.GQLNonNullType
import com.apollographql.apollo.ast.GQLObjectTypeDefinition
import com.apollographql.apollo.ast.GQLScalarTypeDefinition
import com.apollographql.apollo.ast.GQLType
import com.apollographql.apollo.ast.GQLTypeDefinition
import com.apollographql.apollo.ast.GQLUnionTypeDefinition
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.isAbstract
import com.apollographql.apollo.ast.isComposite
import com.apollographql.apollo.compiler.CodegenSchema
import com.apollographql.apollo.compiler.UsedCoordinates
import kotlinx.serialization.Serializable

@Serializable
internal class DefaultIrDataBuilders(
    val dataBuilders: List<IrDataBuilder>,
    val possibleTypes: Map<String, List<String>>,
    val scalars: List<String>
) : IrDataBuilders

interface IrDataBuilders

@Serializable
internal data class IrMapProperty(
    val name: String,
    val type: IrType2,
)

/**
 * This is a separate type from [IrType] because [IrType] is quite big already. We might
 * want to refactor our type handling at some point
 *
 * [IrType2] ultimately resolves to *Map types for composite types instead of the regular model types for [IrType].
 */
@Serializable
internal sealed interface IrType2

@Serializable
internal class IrNonNullType2(val ofType: IrType2) : IrType2

@Serializable
internal class IrListType2(val ofType: IrType2) : IrType2

@Serializable
internal class IrScalarType2(val name: String) : IrType2

@Serializable
internal class IrEnumType2(val name: String) : IrType2

@Serializable
internal class IrCompositeType2(val name: String) : IrType2

@Serializable
internal class IrDataBuilder(
    /**
     * The GraphQL name of the object it can build
     */
    val name: String,

    /**
     * Whether this DataBuilder is for abstract types (unions and interfaces)
     */
    val isAbstract: Boolean,

    /**
     * [superTypes] includes unions in addition to implemented interfaces
     */
    val superTypes: List<String>,

    /**
     * If this is a root type, the operation type ("query", "mutation", "subscription").
     * null if this data builder does not represent a root type.
     */
    val operationType: String?,

    val properties: List<IrMapProperty>,
)

/**
 * Recurse into this type and all its super types to collect all the fields that are queried.
 */
internal fun collectFields(
    schema: Schema,
    usedCoordinatesMap: Map<String, Map<String, Set<String>>>,
    typeDefinition: GQLTypeDefinition,
): Set<String> {
  val superTypes = when (typeDefinition) {
    is GQLObjectTypeDefinition -> typeDefinition.implementsInterfaces
    is GQLInterfaceTypeDefinition -> typeDefinition.implementsInterfaces
    else -> emptyList()
  }
  return usedCoordinatesMap.get(typeDefinition.name)?.keys.orEmpty() + superTypes.flatMap {
    collectFields(schema, usedCoordinatesMap, schema.typeDefinition(it))
  }
}

fun buildIrDataBuilders(
    codegenSchema: CodegenSchema,
    usedCoordinates: UsedCoordinates,
): IrDataBuilders {

  val schema = codegenSchema.schema
  val typeDefinitions = schema.typeDefinitions.values

  val usedCoordinatesMap: Map<String, Map<String, Set<String>>> = usedCoordinates.asMap()
  val typesToVisit = usedCoordinatesMap.keys.toMutableList()
  val visitedTypes = mutableSetOf<String>()
  val dataBuilders = mutableListOf<IrDataBuilder>()

  val schemaDefinition = schema.schemaDefinition!!
  var queryType: String? = null
  var mutationType: String? = null
  var subscriptionType: String? = null
  schemaDefinition.rootOperationTypeDefinitions.forEach {
    when (it.operationType) {
      "query" -> queryType = it.namedType
      "mutation" -> mutationType = it.namedType
      "subscription" -> subscriptionType = it.namedType
    }
  }
  while (typesToVisit.isNotEmpty()) {
    val type = typesToVisit.removeFirst()
    if (visitedTypes.contains(type)) {
      continue
    }
    visitedTypes.add(type)
    val typeDefinition = schema.typeDefinition(type)
    if (!typeDefinition.isComposite()) {
      continue
    }

    /**
     * We generate a DataBuilder that contains all the fields used in the super types.
     */
    val usedFields = collectFields(schema, usedCoordinatesMap, typeDefinition)

    val fieldDefinitions = when (typeDefinition) {
      is GQLObjectTypeDefinition -> typeDefinition.fields
      is GQLInterfaceTypeDefinition -> typeDefinition.fields
      else -> emptyList()
    }

    val irFields = fieldDefinitions.mapNotNull {
      if (usedFields.contains(it.name)) {
        it.toIrMapProperty(schema)
      } else {
        null
      }
    }

    val superTypes = schema.superTypes(typeDefinition).toList()

    /**
     * Visit super types: They will be needed for code generation
     */
    typesToVisit.addAll(superTypes)

    /**
     * Visit sub-types: add all the possible types of this abstract type
     * because the user might want to use any of them:
     *
     * ```kotlin
     * GetHeroQuery.Data {
     *   hero = buildHuman {}  // or buildDroid {}
     * }
     * ```
     */
    if (typeDefinition is GQLUnionTypeDefinition) {
      typesToVisit.addAll(typeDefinition.memberTypes.map { it.name })
    } else if(typeDefinition is GQLInterfaceTypeDefinition) {
      typeDefinitions.forEach {
        when (it) {
          is GQLObjectTypeDefinition -> {
            if (it.implementsInterfaces.contains(type)) {
              typesToVisit.add(it.name)
            }
          }
          is GQLInterfaceTypeDefinition -> {
            if (it.implementsInterfaces.contains(type)) {
              typesToVisit.add(it.name)
            }
          }
          else -> Unit
        }
      }
    }

    val operationType = when (typeDefinition.name) {
      queryType -> "query"
      mutationType -> "mutation"
      subscriptionType -> "subscription"
      else -> null
    }
    /**
     * Add the DataBuilder
     */
    dataBuilders.add(
        IrDataBuilder(
            name = typeDefinition.name,
            isAbstract = typeDefinition.isAbstract(),
            superTypes = superTypes,
            properties = irFields,
            operationType = operationType
        )
    )
  }

  val possibleTypes = codegenSchema.schema.computePossibleTypes()
  val scalars = codegenSchema.schema.typeDefinitions.values.filterIsInstance<GQLScalarTypeDefinition>().map { it.name }
  return DefaultIrDataBuilders(dataBuilders, possibleTypes, scalars)
}

private fun Schema.computePossibleTypes(): Map<String, List<String>> {
  return typeDefinitions.values.mapNotNull {
    if (it !is GQLInterfaceTypeDefinition && it !is GQLUnionTypeDefinition) {
      return@mapNotNull null
    }
    it.name to possibleTypes(it).toList()
  }.toMap()
}

private fun GQLType.toIrType2(schema: Schema): IrType2 {
  return when (this) {
    is GQLNonNullType -> IrNonNullType2(type.toIrType2(schema))
    is GQLListType -> IrListType2(type.toIrType2(schema))
    is GQLNamedType -> {
      when (schema.typeDefinition(name)) {
        is GQLScalarTypeDefinition -> return IrScalarType2(name)
        is GQLEnumTypeDefinition -> return IrEnumType2(name)
        is GQLInputObjectTypeDefinition -> error("Input objects are not supported in data builders")
        is GQLInterfaceTypeDefinition,
        is GQLUnionTypeDefinition,
          -> IrCompositeType2(name)

        is GQLObjectTypeDefinition -> IrCompositeType2(name)
      }
    }
  }
}

private fun GQLFieldDefinition.toIrMapProperty(schema: Schema): IrMapProperty {
  return IrMapProperty(
      name,
      type.toIrType2(schema)
  )
}
