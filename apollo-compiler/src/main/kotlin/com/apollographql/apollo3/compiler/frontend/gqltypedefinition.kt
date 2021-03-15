package com.apollographql.apollo3.compiler.frontend

// 5.5.2.3 Fragment spread is possible
internal fun GQLTypeDefinition.sharesPossibleTypesWith(other: GQLTypeDefinition, typeDefinitions: Map<String, GQLTypeDefinition>): Boolean {
  return possibleTypes(typeDefinitions).intersect(other.possibleTypes(typeDefinitions)).isNotEmpty()
}

internal fun GQLTypeDefinition.possibleTypes(schema: Schema): Set<String> {
  return possibleTypes(schema.typeDefinitions)
}

// 5.5.2.3 Fragment spread is possible
internal fun GQLTypeDefinition.possibleTypes(typeDefinitions: Map<String, GQLTypeDefinition>): Set<String> {
  return when (this) {
    is GQLUnionTypeDefinition -> memberTypes.map { it.name }.toSet()
    is GQLInterfaceTypeDefinition -> typeDefinitions.values.filter {
      it is GQLObjectTypeDefinition && it.implementsInterfaces.contains(this.name)
          || it is GQLInterfaceTypeDefinition && it.implementsInterfaces.contains(this.name)
    }.flatMap {
      // Recurse until we reach the concrete types
      // This could certainly be improved
      it.possibleTypes(typeDefinitions).toList()
    }.toSet()
    is GQLObjectTypeDefinition -> setOf(name)
    is GQLScalarTypeDefinition -> setOf(name)
    is GQLEnumTypeDefinition -> enumValues.map { it.name }.toSet()
    else -> {
      throw SchemaValidationException("Cannot determine possibleTypes of $name")
    }
  }
}

