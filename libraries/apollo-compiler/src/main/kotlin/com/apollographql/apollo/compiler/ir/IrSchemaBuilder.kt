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
      alreadyVisitedTypes: Set<String>,
  ): IrSchema {

    val irEnums = mutableListOf<IrEnum>()
    val irScalars = mutableListOf<IrScalar>()
    val irInputObjects = mutableListOf<IrInputObject>()
    val irUnions = mutableListOf<IrUnion>()
    val irInterfaces = mutableListOf<IrInterface>()
    val irObjects = mutableListOf<IrObject>()

    val visitedTypes = alreadyVisitedTypes.toMutableSet()
    val typesStack = usedCoordinates.getTypes().toMutableList()
    while (typesStack.isNotEmpty()) {
      val name = typesStack.removeFirst()
      if (visitedTypes.contains(name)) {
        continue
      }

      visitedTypes.add(name)
      val typeDefinition = schema.typeDefinition(name)

      when {
        typeDefinition is GQLScalarTypeDefinition -> {
          irScalars.add(typeDefinition.toIr())
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
          irInterfaces.add(typeDefinition.toIr(schema, usedCoordinates))
        }
        typeDefinition is GQLObjectTypeDefinition -> {
          irObjects.add(typeDefinition.toIr(schema, usedCoordinates))
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
