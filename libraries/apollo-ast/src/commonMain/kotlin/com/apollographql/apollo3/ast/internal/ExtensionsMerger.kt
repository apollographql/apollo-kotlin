package com.apollographql.apollo3.ast.internal

import com.apollographql.apollo3.ast.GQLDefinition
import com.apollographql.apollo3.ast.GQLDirective
import com.apollographql.apollo3.ast.GQLDirectiveDefinition
import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.GQLEnumTypeExtension
import com.apollographql.apollo3.ast.GQLEnumValueDefinition
import com.apollographql.apollo3.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLInputObjectTypeExtension
import com.apollographql.apollo3.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo3.ast.GQLInterfaceTypeExtension
import com.apollographql.apollo3.ast.GQLNamed
import com.apollographql.apollo3.ast.GQLNode
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLObjectTypeExtension
import com.apollographql.apollo3.ast.GQLResult
import com.apollographql.apollo3.ast.GQLScalarTypeDefinition
import com.apollographql.apollo3.ast.GQLScalarTypeExtension
import com.apollographql.apollo3.ast.GQLSchemaDefinition
import com.apollographql.apollo3.ast.GQLSchemaExtension
import com.apollographql.apollo3.ast.GQLTypeSystemExtension
import com.apollographql.apollo3.ast.GQLUnionTypeDefinition
import com.apollographql.apollo3.ast.GQLUnionTypeExtension
import com.apollographql.apollo3.ast.Issue
import com.apollographql.apollo3.ast.SourceLocation
import kotlin.reflect.KClass

/**
 * Because directive order is important, the order of the definitions is also important here
 *
 * Typically, extensions come after type definitions
 */
internal class ExtensionsMerger(private val definitions: List<GQLDefinition>) {
  val issues = mutableListOf<Issue>()
  val directiveDefinitions: Map<String, GQLDirectiveDefinition>

  init {
    directiveDefinitions = definitions.filterIsInstance<GQLDirectiveDefinition>().associateBy { it.name }
  }

  fun merge(): GQLResult<List<GQLDefinition>> {
    val newDefinitions = mutableListOf<GQLDefinition>()
    definitions.forEach { definition ->

      when (definition) {
        is GQLTypeSystemExtension -> {
          when(definition) {
            is GQLSchemaExtension -> mergeTyped(GQLSchemaDefinition::class, newDefinitions, definition, "schema") { mergeSchema(it, definition) }
            is GQLScalarTypeExtension -> mergeNamed(GQLScalarTypeDefinition::class, newDefinitions, definition, "scalar") { mergeScalar(it, definition) }
            is GQLInterfaceTypeExtension -> mergeNamed(GQLInterfaceTypeDefinition::class, newDefinitions, definition, "interface") { mergeInterface(it, definition) }
            is GQLObjectTypeExtension -> mergeNamed(GQLObjectTypeDefinition::class, newDefinitions, definition, "object") { mergeObject(it, definition) }
            is GQLInputObjectTypeExtension -> mergeNamed(GQLInputObjectTypeDefinition::class, newDefinitions, definition, "input") { mergeInputObject(it, definition) }
            is GQLEnumTypeExtension -> mergeNamed(GQLEnumTypeDefinition::class, newDefinitions, definition, "enum") { mergeEnum(it, definition) }
            is GQLUnionTypeExtension -> mergeNamed(GQLUnionTypeDefinition::class, newDefinitions, definition, "union") { mergeUnion(it, definition) }
          }
        }
        else -> {
          newDefinitions.add(definition)
        }
      }
    }

    return GQLResult(newDefinitions, issues)
  }
}

private fun ExtensionsMerger.mergeUnion(
    unionTypeDefinition: GQLUnionTypeDefinition,
    extension: GQLUnionTypeExtension,
): GQLUnionTypeDefinition = with(unionTypeDefinition) {
  return copy(
      directives = mergeDirectives(directives, extension.directives),
      memberTypes = mergeUniquesOrThrow(memberTypes, extension.memberTypes)
  )
}

private fun ExtensionsMerger.mergeEnum(
    enumTypeDefinition: GQLEnumTypeDefinition,
    extension: GQLEnumTypeExtension,
): GQLEnumTypeDefinition = with(enumTypeDefinition) {
  return copy(
      directives = mergeDirectives(directives, extension.directives),
      enumValues = mergeEnumValues(enumValues, extension.enumValues),
  )
}

/**
 * Technically not allowed by the current spec, but useful to be able to add directives on enum values.
 * See https://github.com/graphql/graphql-spec/issues/952
 */
private fun ExtensionsMerger.mergeEnumValues(
    existingList: List<GQLEnumValueDefinition>,
    otherList: List<GQLEnumValueDefinition>,
): List<GQLEnumValueDefinition> {
  val result = mutableListOf<GQLEnumValueDefinition>()
  result.addAll(existingList)
  for (other in otherList) {
    val existing = result.firstOrNull { it.name == other.name }
    result += if (existing != null) {
      result.remove(existing)
      existing.copy(directives = mergeDirectives(existing.directives, other.directives))
    } else {
      other
    }
  }
  return result
}

private fun ExtensionsMerger.mergeInputObject(
    inputObjectTypeDefinition: GQLInputObjectTypeDefinition,
    extension: GQLInputObjectTypeExtension,
): GQLInputObjectTypeDefinition = with(inputObjectTypeDefinition) {
  return copy(
      directives = mergeDirectives(directives, extension.directives),
      inputFields = mergeUniquesOrThrow(inputFields, extension.inputFields)
  )
}

private fun ExtensionsMerger.mergeObject(
    objectTypeDefinition: GQLObjectTypeDefinition,
    extension: GQLObjectTypeExtension,
): GQLObjectTypeDefinition = with(objectTypeDefinition) {
  return copy(
      directives = mergeDirectives(directives, extension.directives),
      fields = mergeUniquesOrThrow(fields, extension.fields),
      implementsInterfaces = mergeUniqueInterfacesOrThrow(implementsInterfaces, extension.implementsInterfaces, extension.sourceLocation)
  )
}

private fun ExtensionsMerger.mergeInterface(
    interfaceTypeDefinition: GQLInterfaceTypeDefinition,
    extension: GQLInterfaceTypeExtension,
): GQLInterfaceTypeDefinition = with(interfaceTypeDefinition) {
  return copy(
      fields = mergeUniquesOrThrow(fields, extension.fields),
      directives = mergeDirectives(directives, extension.directives),
      implementsInterfaces = mergeUniqueInterfacesOrThrow(implementsInterfaces, extension.implementsInterfaces, extension.sourceLocation)
  )
}

private inline fun <reified T, E> ExtensionsMerger.mergeTyped(
    @Suppress("UNUSED_PARAMETER") clazz: KClass<T>,
    newDefinitions: MutableList<GQLDefinition>,
    extension: E,
    extra: String,
    merge: (T) -> T,
) where T: GQLDefinition, E: GQLTypeSystemExtension{
  val index = newDefinitions.indexOfFirst { it is T }
  if (index == -1) {
    issues.add(Issue.ValidationError("Cannot find $extra definition to apply extension", extension.sourceLocation))
  } else {
    newDefinitions.set(index, merge(newDefinitions[index]as T))
  }
}

private fun ExtensionsMerger.mergeScalar(
    scalarTypeDefinition: GQLScalarTypeDefinition,
    scalarTypeExtension: GQLScalarTypeExtension,
): GQLScalarTypeDefinition = with(scalarTypeDefinition) {
  return copy(
      directives = mergeDirectives(directives, scalarTypeExtension.directives)
  )
}

private inline fun <reified T, E> ExtensionsMerger.mergeNamed(
    @Suppress("UNUSED_PARAMETER") clazz: KClass<T>,
    definitions: MutableList<GQLDefinition>,
    extension: E,
    extra: String,
    merge: (T) -> T,
) where T : GQLDefinition, T : GQLNamed, E : GQLNamed, E : GQLTypeSystemExtension  {
  val indexedValues = definitions.withIndex().filter { (_, value) ->
    value is T && value.name == extension.name
  }

  when (indexedValues.size) {
    0 -> {
      issues.add(Issue.ValidationError("Cannot find $extra type `${extension.name}` to apply extension", extension.sourceLocation))
    }
    1 -> {
      val indexedValue = indexedValues.first()
      definitions.set(indexedValue.index, merge(indexedValue.value as T))
    }
    else -> {
      issues.add(Issue.ValidationError("Multiple '${extension.name}' types found while merging extensions.", extension.sourceLocation))
    }
  }
}

private fun ExtensionsMerger.mergeSchema(
    schemaDefinition: GQLSchemaDefinition,
    extension: GQLSchemaExtension,
): GQLSchemaDefinition = with(schemaDefinition) {
  return copy(
      directives = mergeDirectives(directives, extension.directives),
      rootOperationTypeDefinitions = mergeUniquesOrThrow(rootOperationTypeDefinitions, extension.operationTypeDefinitions) { it.operationType }
  )
}

/**
 * Merge both list of directive
 */
private fun ExtensionsMerger.mergeDirectives(
    list: List<GQLDirective>,
    other: List<GQLDirective>,
): List<GQLDirective> {
  val result = mutableListOf<GQLDirective>()

  result.addAll(list)
  for (directiveToAdd in other) {
    if (result.any { it.name == directiveToAdd.name }) {
      // duplicated directive, get the definition to see if we can
      val definition = directiveDefinitions[directiveToAdd.name]

      if (definition == null) {
        issues.add(Issue.ValidationError("Cannot find directive definition `${directiveToAdd.name}`", directiveToAdd.sourceLocation))
        continue
      } else if (!definition.repeatable) {
        issues.add(Issue.ValidationError("Cannot add non-repeatable directive `${directiveToAdd.name}`", directiveToAdd.sourceLocation))
        continue
      }
    }
    result.add(directiveToAdd)
  }

  return result
}

private inline fun <reified T> ExtensionsMerger.mergeUniquesOrThrow(
    list: List<T>,
    others: List<T>,
): List<T> where T : GQLNamed, T : GQLNode = with(list) {
  return (this + others).apply {
    groupBy { it.name }.entries.firstOrNull { it.value.size > 1 }?.let {
      issues.add(Issue.ValidationError("Cannot merge already existing node `${it.key}`", it.value.first().sourceLocation))
    }
  }
}

private fun ExtensionsMerger.mergeUniqueInterfacesOrThrow(
    list: List<String>,
    others: List<String>,
    sourceLocation: SourceLocation?,
): List<String> = with(list) {
  return (this + others).apply {
    groupBy { it }.entries.firstOrNull { it.value.size > 1 }?.let {
      issues.add(Issue.ValidationError("Cannot merge interface ${it.value.first()} as it's already defined", sourceLocation))
    }
  }
}


private inline fun <reified T : GQLNode> ExtensionsMerger.mergeUniquesOrThrow(
    list: List<T>,
    others: List<T>,
    name: (T) -> String,
): List<T> = with(list) {
  return (this + others).apply {
    groupBy { name(it) }.entries.firstOrNull { it.value.size > 1 }?.let {
      issues.add(Issue.ValidationError("Cannot merge already existing node `${it.key}`", it.value.first().sourceLocation))
    }
  }
}

