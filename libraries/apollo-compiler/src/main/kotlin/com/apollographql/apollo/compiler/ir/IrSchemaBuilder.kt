package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.ast.GQLEnumTypeDefinition
import com.apollographql.apollo.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo.ast.GQLObjectTypeDefinition
import com.apollographql.apollo.ast.GQLScalarTypeDefinition
import com.apollographql.apollo.ast.GQLUnionTypeDefinition
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.compiler.UsedCoordinates

internal object IrSchemaBuilder {
  fun build(
      schema: Schema,
      usedCoordinates: UsedCoordinates,
  ): IrSchema {

    val irEnums = mutableListOf<IrEnum>()
    val irScalars = mutableListOf<IrScalar>()
    val irInputObjects = mutableListOf<IrInputObject>()
    val irUnions = mutableListOf<IrUnion>()
    val irInterfaces = mutableListOf<IrInterface>()
    val irObjects = mutableListOf<IrObject>()

    /*
     * Add scalar types with runtime adapters.
     * This is so that the user can register them in a typesafe way.
     * Note that in most of the cases, the scalar type should be added already as it is most likely used in an operation.
     */
    val scalarUsedCoordinates = UsedCoordinates()
    schema.typeDefinitions.values.forEach {
      if (it !is GQLScalarTypeDefinition) return@forEach

      val mapTo = it.findMapTo(schema)
      if (mapTo != null && mapTo.adapter == null) {
        scalarUsedCoordinates.putType(it.name)
      }
    }

    val mergedUsedCoordinates = usedCoordinates.mergeWith(scalarUsedCoordinates)

    val visitedTypes = mutableSetOf<String>()
    val typesStack = mergedUsedCoordinates.getTypes().toMutableList()

    while (typesStack.isNotEmpty()) {
      val name = typesStack.removeFirst()
      if (visitedTypes.contains(name)) {
        continue
      }

      visitedTypes.add(name)
      val typeDefinition = schema.typeDefinition(name)

      when {
        typeDefinition is GQLScalarTypeDefinition -> {
          irScalars.add(typeDefinition.toIr(schema, mergedUsedCoordinates))
        }
        typeDefinition is GQLEnumTypeDefinition -> {
          irEnums.add(typeDefinition.toIr(schema))
        }
        typeDefinition is GQLInputObjectTypeDefinition -> {
          irInputObjects.add(typeDefinition.toIr(schema))
        }
        typeDefinition is GQLUnionTypeDefinition -> {
          irUnions.add(typeDefinition.toIr())
        }
        typeDefinition is GQLInterfaceTypeDefinition -> {
          irInterfaces.add(typeDefinition.toIr(schema, mergedUsedCoordinates))
        }
        typeDefinition is GQLObjectTypeDefinition -> {
          irObjects.add(typeDefinition.toIr(schema, mergedUsedCoordinates))
        }
      }
    }

    return DefaultIrSchema(
        irScalars = irScalars,
        irEnums = irEnums,
        irInputObjects = irInputObjects,
        irUnions = irUnions,
        irInterfaces = irInterfaces,
        irObjects = irObjects,
        connectionTypes = schema.connectionTypes.toList(),
    )
  }
}
