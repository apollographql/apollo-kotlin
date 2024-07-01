@file:JvmName("PossibleTypes")

package com.apollographql.apollo.api

import kotlin.jvm.JvmName

private fun possibleTypesInternal(allTypes: List<CompiledType>, type: CompiledNamedType): List<ObjectType> {
  return when (type) {
    is ObjectType -> listOf(type)
    is UnionType -> type.members.toList()
    is InterfaceType -> {
      allTypes.flatMap { possibleImplementation ->
        when (possibleImplementation) {
          is ObjectType -> {
            if (possibleImplementation.implements.any { it.name == type.name }) {
              possibleTypesInternal(allTypes, possibleImplementation)
            } else {
              emptyList()
            }
          }
          is InterfaceType -> {
            if (possibleImplementation.implements.any { it.name == type.name }) {
              possibleTypesInternal(allTypes, possibleImplementation)
            } else {
              emptyList()
            }
          }
          else -> emptyList()
        }
      }
    }
    else -> error("Type '$type' can only have one possible type")
  }
}

/**
 * Returns all objects that implement [type]
 */
fun possibleTypes(allTypes: List<CompiledType>, type: CompiledNamedType): List<ObjectType> {
  return possibleTypesInternal(allTypes, type).distinctBy { it.name }.sortedBy { it.name }
}
