package com.apollographql.apollo.compiler.parser.graphql.ast

import com.apollographql.apollo.compiler.parser.error.ParseException

fun GQLDocument.validateAndNormalizeSchema(): GQLDocument {
  validateNotExecutable()
  validateUniqueSchemaDefinition()
  validateTypeNames()
  validateDirectiveNames()

  val (extensions, otherDefinitions) = definitions.partition { it is GQLTypeSystemExtension }
  extensions as List<GQLTypeSystemExtension>

  return copy(
      definitions = extensions.fold(otherDefinitions) { acc, typeExtension ->
        acc.merge(typeExtension)
      }
  )
}


private fun List<GQLDefinition>.merge(typeExtension: GQLTypeSystemExtension): List<GQLDefinition> {
  return when (typeExtension) {
    is GQLSchemaExtension -> merge(schemaExtension = typeExtension)
    else -> TODO()
  }
}

private fun List<GQLDefinition>.merge(schemaExtension: GQLSchemaExtension): List<GQLDefinition> {
  var found = false
  val definitions = mutableListOf<GQLDefinition>()
  forEach {
    if (it is GQLSchemaDefinition) {
      definitions.add(it.merge(schemaExtension))
      found = true
    } else {
      definitions.add(it)
    }
  }
  if (!found) {
    throw ParseException("Cannot apply schema extension on non existing schema definition", schemaExtension.sourceLocation)
  }
  return definitions
}

private fun GQLSchemaDefinition.merge(extension: GQLSchemaExtension): GQLSchemaDefinition {
  return copy(
      directives = directives.merge(extension.directives) { it.name },
      rootOperationTypeDefinitions = rootOperationTypeDefinitions.merge(extension.operationTypesDefinition) { it.operationType }
  )
}

private inline fun <reified T : GQLNode> List<T>.merge(directives: List<T>, name: (T) -> String): List<T> {
  return (this + directives).apply {
    groupBy { name(it) }.entries.firstOrNull { it.value.size > 1 }?.let {
      throw ParseException("Cannot merge already existing node ${T::class.java.simpleName} `${it.key}`", it.value.first().sourceLocation)
    }
  }
}

private fun GQLDocument.validateUniqueSchemaDefinition() {
  val schemaDefinitions = definitions.filter { it is GQLSchemaDefinition }
  if (schemaDefinitions.count() > 1) {
    throw ParseException("multiple schema definitions found", schemaDefinitions.last().sourceLocation)
  }
}


private fun GQLDefinition.typeDefinitionName() = when (this) {
  is GQLEnumTypeDefinition -> name
  is GQLScalarTypeDefinition -> name
  is GQLObjectTypeDefinition -> name
  is GQLInterfaceTypeDefinition -> name
  is GQLUnionTypeDefinition -> name
  is GQLInputObjectTypeDefinition -> name
  else -> null
}

private fun GQLDocument.validateTypeNames() {
  val typeDefinitions = mutableMapOf<String, GQLDefinition>()
  val conflicts = mutableListOf<GQLDefinition>()
  definitions.forEach {
    val name = it.typeDefinitionName()
    if (name == null) {
      return@forEach
    }
    if (!typeDefinitions.containsKey(name)) {
      typeDefinitions.put(name, it)
    } else {
      conflicts.add(it)
    }
  }

  // 3.3 All types within a GraphQL schema must have unique names
  if (conflicts.size > 0) {
    val conflict = conflicts.first()
    throw ParseException("type '${conflict.typeDefinitionName()}' is defined multiple times", conflict.sourceLocation)
  }

  // 3.3 All types and directives defined within a schema must not have a name which begins with "__"
  typeDefinitions.forEach { name, definition ->
    if (name.startsWith("__")) {
      throw ParseException("names starting with '__' are reserved for introspection", definition.sourceLocation)
    }

  }
}

private fun GQLDefinition.directiveDefinitionName() = when (this) {
  is GQLDirectiveDefinition -> name
  else -> null
}

private fun GQLDocument.validateDirectiveNames() {
  val directiveDefinitions = mutableMapOf<String, GQLDefinition>()
  val conflicts = mutableListOf<GQLDefinition>()
  definitions.forEach {
    val name = it.directiveDefinitionName()
    if (name == null) {
      return@forEach
    }
    if (!directiveDefinitions.containsKey(name)) {
      directiveDefinitions.put(name, it)
    } else {
      conflicts.add(it)
    }
  }

  // 3.3 All directives within a GraphQL schema must have unique names.
  if (conflicts.size > 0) {
    val conflict = conflicts.first()
    throw ParseException("directive '${conflict.typeDefinitionName()}' is defined multiple times", conflict.sourceLocation)
  }

  // 3.3 All types and directives defined within a schema must not have a name which begins with "__"
  directiveDefinitions.forEach { name, definition ->
    if (name.startsWith("__")) {
      throw ParseException("names starting with '__' are reserved for introspection", definition.sourceLocation)
    }

  }
}

/**
 * This is not in the specification per-se but in our use case, that will help catch some cases when users mistake
 * graphql operations for schemas
 */
private fun GQLDocument.validateNotExecutable() {
  definitions.firstOrNull { it is GQLOperationDefinition || it is GQLFragmentDefinition }
      ?.let {
        throw ParseException("Found executable definition while parsing schema", it.sourceLocation)
      }
}

fun GQLDocument.toIntrospectionSchema() {

}