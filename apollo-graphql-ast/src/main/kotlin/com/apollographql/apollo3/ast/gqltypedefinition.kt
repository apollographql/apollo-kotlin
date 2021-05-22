package com.apollographql.apollo3.ast

// 5.5.2.3 Fragment spread is possible
internal fun GQLTypeDefinition.sharesPossibleTypesWith(other: GQLTypeDefinition, typeDefinitions: Map<String, GQLTypeDefinition>): Boolean {
  return possibleTypes(typeDefinitions).intersect(other.possibleTypes(typeDefinitions)).isNotEmpty()
}

fun GQLTypeDefinition.possibleTypes(schema: Schema): Set<String> {
  return possibleTypes(schema.typeDefinitions)
}

// 5.5.2.3 Fragment spread is possible
fun GQLTypeDefinition.possibleTypes(typeDefinitions: Map<String, GQLTypeDefinition>): Set<String> {
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

fun GQLTypeDefinition.isFieldNonNull(fieldName: String): Boolean {
  val directive = when (this) {
    is GQLObjectTypeDefinition -> directives
    is GQLInterfaceTypeDefinition -> directives
    else -> return false
  }.firstOrNull { it.name == "nonnull" }

  if (directive == null) {
    return false
  }

  return (directive.arguments!!.arguments.first().value as GQLListValue)
      .values
      .map { (it as GQLStringValue).value }
      .contains(fieldName)
}