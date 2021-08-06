package com.apollographql.apollo3.ast.internal

import com.apollographql.apollo3.ast.GQLDefinition
import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.GQLEnumTypeExtension
import com.apollographql.apollo3.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLInputObjectTypeExtension
import com.apollographql.apollo3.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo3.ast.GQLInterfaceTypeExtension
import com.apollographql.apollo3.ast.GQLNamed
import com.apollographql.apollo3.ast.GQLNode
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLObjectTypeExtension
import com.apollographql.apollo3.ast.GQLScalarTypeDefinition
import com.apollographql.apollo3.ast.GQLScalarTypeExtension
import com.apollographql.apollo3.ast.GQLSchemaDefinition
import com.apollographql.apollo3.ast.GQLSchemaExtension
import com.apollographql.apollo3.ast.GQLTypeSystemExtension
import com.apollographql.apollo3.ast.GQLUnionTypeDefinition
import com.apollographql.apollo3.ast.GQLUnionTypeExtension
import com.apollographql.apollo3.ast.Issue
import com.apollographql.apollo3.ast.SourceLocation
import com.apollographql.apollo3.ast.UnrecognizedAntlrRule


internal fun IssuesScope.mergeExtensions(definitions: List<GQLDefinition>): List<GQLDefinition> {
  val (extensions, otherDefinitions) = definitions.partition { it is GQLTypeSystemExtension }

  return extensions.fold(otherDefinitions) { acc, extension ->
    when (extension) {
      is GQLSchemaExtension -> mergeSchemaExtension(acc, schemaExtension = extension)
      is GQLScalarTypeExtension -> merge<GQLScalarTypeDefinition, GQLScalarTypeExtension>(acc, extension) { merge(it, extension) }
      is GQLInterfaceTypeExtension -> merge<GQLInterfaceTypeDefinition, GQLInterfaceTypeExtension>(acc, extension) { merge(it, extension) }
      is GQLObjectTypeExtension -> merge<GQLObjectTypeDefinition, GQLObjectTypeExtension>(acc, extension) { merge(it, extension) }
      is GQLInputObjectTypeExtension -> merge<GQLInputObjectTypeDefinition, GQLInputObjectTypeExtension>(acc, extension) { merge(it, extension) }
      is GQLEnumTypeExtension -> merge<GQLEnumTypeDefinition, GQLEnumTypeExtension>(acc, extension) { merge(it, extension) }
      is GQLUnionTypeExtension -> merge<GQLUnionTypeDefinition, GQLUnionTypeExtension>(acc, extension) { merge(it, extension) }
      else -> throw UnrecognizedAntlrRule("Unrecognized type system extension", extension.sourceLocation)
    }
  }
}

private fun IssuesScope.merge(
    unionTypeDefinition: GQLUnionTypeDefinition,
    extension: GQLUnionTypeExtension,
): GQLUnionTypeDefinition = with(unionTypeDefinition) {
  return copy(
      directives = mergeUniquesOrThrow(directives, extension.directives),
      memberTypes = mergeUniquesOrThrow(memberTypes, extension.memberTypes)
  )
}

private fun IssuesScope.merge(
    enumTypeDefinition: GQLEnumTypeDefinition,
    extension: GQLEnumTypeExtension,
): GQLEnumTypeDefinition = with(enumTypeDefinition) {
  return copy(
      directives = mergeUniquesOrThrow(directives, extension.directives),
      enumValues = mergeUniquesOrThrow(enumValues, extension.enumValues),
  )
}

private fun IssuesScope.merge(
    inputObjectTypeDefinition: GQLInputObjectTypeDefinition,
    extension: GQLInputObjectTypeExtension,
): GQLInputObjectTypeDefinition = with(inputObjectTypeDefinition) {
  return copy(
      directives = mergeUniquesOrThrow(directives, extension.directives),
      inputFields = mergeUniquesOrThrow(inputFields, extension.inputFields)
  )
}

private fun IssuesScope.merge(
    objectTypeDefinition: GQLObjectTypeDefinition,
    extension: GQLObjectTypeExtension,
): GQLObjectTypeDefinition = with(objectTypeDefinition) {
  return copy(
      directives = mergeUniquesOrThrow(directives, extension.directives),
      fields = mergeUniquesOrThrow(fields, extension.fields),
      implementsInterfaces = mergeUniqueInterfacesOrThrow(implementsInterfaces, extension.implementsInterfaces, extension.sourceLocation)
  )
}

private fun IssuesScope.merge(
    interfaceTypeDefinition: GQLInterfaceTypeDefinition,
    extension: GQLInterfaceTypeExtension,
): GQLInterfaceTypeDefinition = with(interfaceTypeDefinition) {
  return copy(
      fields = mergeUniquesOrThrow(fields, extension.fields),
      implementsInterfaces = mergeUniqueInterfacesOrThrow(implementsInterfaces, extension.implementsInterfaces, extension.sourceLocation)
  )
}

private fun IssuesScope.mergeSchemaExtension(
    definitions: List<GQLDefinition>,
    schemaExtension: GQLSchemaExtension,
): List<GQLDefinition> = with(definitions) {
  var found = false
  val newDefinitions = mutableListOf<GQLDefinition>()
  forEach {
    if (it is GQLSchemaDefinition) {
      newDefinitions.add(merge(it, schemaExtension))
      found = true
    } else {
      newDefinitions.add(it)
    }
  }
  if (!found) {
    issues.add(Issue.ValidationError("Cannot apply schema extension on non existing schema definition", schemaExtension.sourceLocation))
  }
  return newDefinitions
}

private fun IssuesScope.merge(
    scalarTypeDefinition: GQLScalarTypeDefinition,
    scalarTypeExtension: GQLScalarTypeExtension,
): GQLScalarTypeDefinition = with(scalarTypeDefinition) {
  return copy(
      directives = mergeUniquesOrThrow(directives, scalarTypeExtension.directives)
  )
}

private inline fun <reified T, E> IssuesScope.merge(
    definitions: List<GQLDefinition>,
    extension: E,
    merge: (T) -> T,
): List<GQLDefinition> where T : GQLDefinition, T : GQLNamed, E : GQLNamed, E : GQLNode = with(definitions) {
  var found = false
  val newDefinitions = mutableListOf<GQLDefinition>()
  forEach {
    if (it is T && it.name == extension.name) {
      if (found) {
        issues.add(Issue.ValidationError("Multiple '${extension.name}' types found while merging extensions. This is a bug, check validation code", extension.sourceLocation))
      }
      newDefinitions.add(merge(it))
      found = true
    } else {
      newDefinitions.add(it)
    }
  }
  if (!found) {
    issues.add(Issue.ValidationError("Cannot find type `${extension.name}` to apply extension", extension.sourceLocation))
  }
  return newDefinitions
}

private fun IssuesScope.merge(
    schemaDefinition: GQLSchemaDefinition,
    extension: GQLSchemaExtension,
): GQLSchemaDefinition = with(schemaDefinition) {
  return copy(
      directives = mergeUniquesOrThrow(directives, extension.directives),
      rootOperationTypeDefinitions = mergeUniquesOrThrow(rootOperationTypeDefinitions, extension.operationTypesDefinition) { it.operationType }
  )
}

private inline fun <reified T> IssuesScope.mergeUniquesOrThrow(
    list: List<T>,
    others: List<T>,
): List<T> where T : GQLNamed, T : GQLNode = with(list) {
  return (this + others).apply {
    groupBy { it.name }.entries.firstOrNull { it.value.size > 1 }?.let {
      issues.add(Issue.ValidationError("Cannot merge already existing node ${T::class.java.simpleName} `${it.key}`", it.value.first().sourceLocation))
    }
  }
}

private fun IssuesScope.mergeUniqueInterfacesOrThrow(
    list: List<String>,
    others: List<String>,
    sourceLocation: SourceLocation,
): List<String> = with(list) {
  return (this + others).apply {
    groupBy { it }.entries.firstOrNull { it.value.size > 1 }?.let {
      issues.add(Issue.ValidationError("Cannot merge interface ${it.value.first()} as it's already defined", sourceLocation))
    }
  }
}


private inline fun <reified T : GQLNode> IssuesScope.mergeUniquesOrThrow(
    list: List<T>,
    others: List<T>,
    name: (T) -> String,
): List<T> = with(list) {
  return (this + others).apply {
    groupBy { name(it) }.entries.firstOrNull { it.value.size > 1 }?.let {
      issues.add(Issue.ValidationError("Cannot merge already existing node ${T::class.java.simpleName} `${it.key}`", it.value.first().sourceLocation))
    }
  }
}

