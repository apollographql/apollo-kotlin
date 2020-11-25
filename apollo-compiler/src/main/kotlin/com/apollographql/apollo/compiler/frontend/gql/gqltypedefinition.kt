package com.apollographql.apollo.compiler.frontend.gql

// 5.5.2.3 Fragment spread is possible
internal fun GQLTypeDefinition.sharesPossibleTypesWith(other: GQLTypeDefinition, typeDefinitions: Map<String, GQLTypeDefinition>): Boolean {
  return possibleTypes(typeDefinitions).intersect(other.possibleTypes(typeDefinitions)).isNotEmpty()
}

// 5.5.2.3 Fragment spread is possible
internal fun GQLTypeDefinition.possibleTypes(typeDefinitions: Map<String, GQLTypeDefinition>): Set<String> {
  return when (this) {
    is GQLUnionTypeDefinition -> memberTypes.map { it.name }.toSet()
    is GQLInterfaceTypeDefinition -> setOf(name) + typeDefinitions.values.filter {
      it is GQLObjectTypeDefinition && it.implementsInterfaces.contains(this.name)
          || it is GQLInterfaceTypeDefinition && it.implementsInterfaces.contains(this.name)
    }.flatMap {
      // Recurse until we reach the concrete types
      // This could certainly be improved
      it.possibleTypes(typeDefinitions).toList()
    }.toSet()
    is GQLObjectTypeDefinition -> setOf(name)
    else -> throw SchemaValidationException("Cannot determine possibleTypes of $name")
  }
}

internal fun GQLTypeDefinition.isBuiltIn() = setOf(
    "Int",
    "Float",
    "String",
    "Boolean",
    "ID",
    "__Schema",
    "__Type",
    "__Field",
    "__InputValue",
    "__EnumValue",
    "__TypeKind",
    "__Directive",
    "__DirectiveLocation").contains(name)