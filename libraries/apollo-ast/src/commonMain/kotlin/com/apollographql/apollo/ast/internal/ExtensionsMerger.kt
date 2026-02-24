package com.apollographql.apollo.ast.internal

import com.apollographql.apollo.ast.GQLBooleanValue
import com.apollographql.apollo.ast.GQLDefinition
import com.apollographql.apollo.ast.GQLDirective
import com.apollographql.apollo.ast.GQLDirectiveDefinition
import com.apollographql.apollo.ast.GQLDirectiveExtension
import com.apollographql.apollo.ast.GQLEnumTypeDefinition
import com.apollographql.apollo.ast.GQLEnumTypeExtension
import com.apollographql.apollo.ast.GQLEnumValue
import com.apollographql.apollo.ast.GQLEnumValueDefinition
import com.apollographql.apollo.ast.GQLFieldDefinition
import com.apollographql.apollo.ast.GQLFloatValue
import com.apollographql.apollo.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo.ast.GQLInputObjectTypeExtension
import com.apollographql.apollo.ast.GQLInputValueDefinition
import com.apollographql.apollo.ast.GQLIntValue
import com.apollographql.apollo.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo.ast.GQLInterfaceTypeExtension
import com.apollographql.apollo.ast.GQLListType
import com.apollographql.apollo.ast.GQLListValue
import com.apollographql.apollo.ast.GQLNamed
import com.apollographql.apollo.ast.GQLNamedType
import com.apollographql.apollo.ast.GQLNode
import com.apollographql.apollo.ast.GQLNonNullType
import com.apollographql.apollo.ast.GQLNullValue
import com.apollographql.apollo.ast.GQLObjectTypeDefinition
import com.apollographql.apollo.ast.GQLObjectTypeExtension
import com.apollographql.apollo.ast.GQLObjectValue
import com.apollographql.apollo.ast.GQLResult
import com.apollographql.apollo.ast.GQLScalarTypeDefinition
import com.apollographql.apollo.ast.GQLScalarTypeExtension
import com.apollographql.apollo.ast.GQLSchemaDefinition
import com.apollographql.apollo.ast.GQLSchemaExtension
import com.apollographql.apollo.ast.GQLServiceDefinition
import com.apollographql.apollo.ast.GQLServiceExtension
import com.apollographql.apollo.ast.GQLStringValue
import com.apollographql.apollo.ast.GQLType
import com.apollographql.apollo.ast.GQLTypeSystemExtension
import com.apollographql.apollo.ast.GQLUnionTypeDefinition
import com.apollographql.apollo.ast.GQLUnionTypeExtension
import com.apollographql.apollo.ast.GQLValue
import com.apollographql.apollo.ast.GQLVariableValue
import com.apollographql.apollo.ast.Issue
import com.apollographql.apollo.ast.MergeOptions
import com.apollographql.apollo.ast.OtherValidationIssue
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.SourceLocation
import com.apollographql.apollo.ast.toUtf8
import kotlin.reflect.KClass

/**
 * Because directive order is important, the order of the definitions is also important here
 *
 * Typically, extensions come after type definitions
 */
internal class ExtensionsMerger(private val definitions: List<GQLDefinition>, internal val mergeOptions: MergeOptions) {
  val issues = mutableListOf<Issue>()
  val directiveDefinitions: Map<String, GQLDirectiveDefinition> = definitions
      .filterIsInstance<GQLDirectiveDefinition>()
      .associateBy { it.name }

  fun merge(): GQLResult<List<GQLDefinition>> {
    val newDefinitions = mutableListOf<GQLDefinition>()
    definitions.forEach { definition ->

      when (definition) {
        is GQLTypeSystemExtension -> {
          when (definition) {
            is GQLSchemaExtension -> mergeTypedDefinition(GQLSchemaDefinition::class, newDefinitions, definition, "schema") { mergeSchema(it, definition) }
            is GQLScalarTypeExtension -> mergeNamedDefinition(GQLScalarTypeDefinition::class, newDefinitions, definition, "scalar") { mergeScalar(it, definition) }
            is GQLInterfaceTypeExtension -> mergeNamedDefinition(GQLInterfaceTypeDefinition::class, newDefinitions, definition, "interface") { mergeInterface(it, definition) }
            is GQLObjectTypeExtension -> mergeNamedDefinition(GQLObjectTypeDefinition::class, newDefinitions, definition, "object") { mergeObject(it, definition) }
            is GQLInputObjectTypeExtension -> mergeNamedDefinition(GQLInputObjectTypeDefinition::class, newDefinitions, definition, "input") { mergeInputObject(it, definition) }
            is GQLEnumTypeExtension -> mergeNamedDefinition(GQLEnumTypeDefinition::class, newDefinitions, definition, "enum") { mergeEnum(it, definition) }
            is GQLUnionTypeExtension -> mergeNamedDefinition(GQLUnionTypeDefinition::class, newDefinitions, definition, "union") { mergeUnion(it, definition) }
            is GQLDirectiveExtension -> mergeNamedDefinition(GQLDirectiveDefinition::class, newDefinitions, definition, "directive") { mergeDirective(it, definition) }
            is GQLServiceExtension -> mergeTypedDefinition(GQLServiceDefinition::class, newDefinitions, definition, "service") { mergeService(it, definition) }
          }
        }

        else -> {
          newDefinitions.add(definition)
        }
      }
    }

    return GQLResult(newDefinitions, issues)
  }

  fun mergeFieldDefinition(
      issues: MutableList<Issue>,
      existing: GQLFieldDefinition,
      incoming: GQLFieldDefinition,
  ): GQLFieldDefinition? {
    // Cannot change the type
    if (!areEqual(existing.type, incoming.type)) {
      issues.add(OtherValidationIssue("Cannot merge field '${incoming.name}': wrong type '${incoming.type.toUtf8()}' (expected: '${existing.type.toUtf8()}')", incoming.sourceLocation))
      return null
    }

    if (incoming.description != null && incoming.description != existing.description) {
      issues.add(OtherValidationIssue("Cannot merge field '${incoming.name}': descriptions are different", incoming.sourceLocation))
    }

    return existing.copy(
        directives = mergeDirectives(existing.directives, incoming.directives),
        arguments = mergeArguments(existing.arguments, incoming.arguments)
    )
  }
}

private fun ExtensionsMerger.mergeArguments(
    existingDefinitions: List<GQLInputValueDefinition>,
    incomingDefinitions: List<GQLInputValueDefinition>,
): List<GQLInputValueDefinition> {
  val newArguments = mutableListOf<GQLInputValueDefinition>()
  newArguments.addAll(existingDefinitions)
  incomingDefinitions.forEach { incoming ->
    val index = newArguments.indexOfFirst { arg -> arg.name == incoming.name }
    if (index != -1) {
      val existing = newArguments[index]
      if (!areEqual(existing.type, incoming.type)) {
        issues.add(OtherValidationIssue("Cannot merge argument '${incoming.name}': wrong type '${incoming.type.toUtf8()}' (expected: '${existing.type.toUtf8()}')", incoming.sourceLocation))
      }
      if (incoming.description != null && incoming.description != existing.description) {
        issues.add(OtherValidationIssue("Cannot merge argument '${incoming.name}': descriptions are different", incoming.sourceLocation))
      }
      if (incoming.defaultValue != null && !areEqual(incoming.defaultValue, existing.defaultValue)) {
        issues.add(OtherValidationIssue("Cannot merge argument '${incoming.name}': default values are different", incoming.sourceLocation))
      }
      newArguments[index] = existing.copy(
          directives = mergeDirectives(existing.directives, incoming.directives)
      )
    } else {
      newArguments.add(incoming)
    }
  }

  return newArguments
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
  val mergedDirectives = mergeObjectOrInterfaceDirectives(directives, extension.directives)
  return copy(
      fields = mergeFields(fields, extension.fields, mergedDirectives.fieldDirectives),
      directives = mergedDirectives.directives,
      implementsInterfaces = mergeUniqueInterfacesOrThrow(implementsInterfaces, extension.implementsInterfaces, extension.sourceLocation)
  )
}

private fun ExtensionsMerger.mergeInterface(
    interfaceTypeDefinition: GQLInterfaceTypeDefinition,
    extension: GQLInterfaceTypeExtension,
): GQLInterfaceTypeDefinition = with(interfaceTypeDefinition) {
  val mergedDirectives = mergeObjectOrInterfaceDirectives(directives, extension.directives)
  return copy(
      fields = mergeFields(fields, extension.fields, mergedDirectives.fieldDirectives),
      directives = mergedDirectives.directives,
      implementsInterfaces = mergeUniqueInterfacesOrThrow(implementsInterfaces, extension.implementsInterfaces, extension.sourceLocation)
  )
}

private inline fun <reified T, E> ExtensionsMerger.mergeTypedDefinition(
    @Suppress("UNUSED_PARAMETER") clazz: KClass<T>,
    newDefinitions: MutableList<GQLDefinition>,
    extension: E,
    extra: String,
    merge: (T) -> T,
) where T : GQLDefinition, E : GQLTypeSystemExtension {
  val index = newDefinitions.indexOfFirst { it is T }
  if (index == -1) {
    issues.add(OtherValidationIssue("Cannot find $extra definition to apply the following extension:\n${extension.toUtf8()}", extension.sourceLocation))
  } else {
    newDefinitions.set(index, merge(newDefinitions[index] as T))
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

private fun ExtensionsMerger.mergeDirective(
    directiveDefinition: GQLDirectiveDefinition,
    extension: GQLDirectiveExtension,
): GQLDirectiveDefinition = with(directiveDefinition) {
  return copy(
      directives = mergeDirectives(directives, extension.directives)
  )
}

private inline fun <reified T, E> ExtensionsMerger.mergeNamedDefinition(
    @Suppress("UNUSED_PARAMETER") clazz: KClass<T>,
    definitions: MutableList<GQLDefinition>,
    extension: E,
    extra: String,
    merge: (T) -> T,
) where T : GQLDefinition, T : GQLNamed, E : GQLNamed, E : GQLTypeSystemExtension {
  val indexedValues = definitions.withIndex().filter { (_, value) ->
    value is T && value.name == extension.name
  }

  when (indexedValues.size) {
    0 -> {
      issues.add(OtherValidationIssue("Cannot find $extra type `${extension.name}` to apply the following extension:\n${extension.toUtf8()}", extension.sourceLocation))
    }

    1 -> {
      val indexedValue = indexedValues.first()
      definitions.set(indexedValue.index, merge(indexedValue.value as T))
    }

    else -> {
      issues.add(OtherValidationIssue("Multiple '${extension.name}' types found while merging extensions.", extension.sourceLocation))
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

private fun ExtensionsMerger.mergeService(
    serviceDefinition: GQLServiceDefinition,
    extension: GQLServiceExtension,
): GQLServiceDefinition = with(serviceDefinition) {
  return copy(
      directives = mergeDirectives(directives, extension.directives),
      capabilities = mergeUniquesOrThrow(capabilities, extension.capabilities) { it.name }
  )
}

private class MergedDirectives(
    val directives: List<GQLDirective>,
    val fieldDirectives: Map<String, List<GQLDirective>>,
)

private fun ExtensionsMerger.mergeObjectOrInterfaceDirectives(
    list: List<GQLDirective>,
    other: List<GQLDirective>,
): MergedDirectives {
  val fieldDirectives = mutableListOf<Pair<String, GQLDirective>>()

  val directives = mergeDirectives(list, other) {
    if (it.name == Schema.SEMANTIC_NON_NULL_FIELD) {
      fieldDirectives.add((it.arguments.first { it.name == "name" }.value as GQLStringValue).value to GQLDirective(
          name = Schema.SEMANTIC_NON_NULL,
          arguments = it.arguments.filter { it.name != "name" }
      ))
      false
    } else {
      true
    }
  }

  return MergedDirectives(
      directives,
      fieldDirectives.groupBy(
          keySelector = { it.first },
          valueTransform = { it.second }
      )
  )
}

/**
 * Merge both lists of directives
 */
private fun ExtensionsMerger.mergeDirectives(
    list: List<GQLDirective>,
    other: List<GQLDirective>,
    filter: (GQLDirective) -> Boolean = { true },
): List<GQLDirective> {
  val result = mutableListOf<GQLDirective>()

  result.addAll(list)
  for (directiveToAdd in other) {
    if (!filter(directiveToAdd)) {
      continue
    }

    if (result.any { it.name == directiveToAdd.name }) {
      // duplicated directive, get the definition to see if we can
      val definition = directiveDefinitions[directiveToAdd.name]

      if (definition == null) {
        issues.add(OtherValidationIssue("Cannot find directive definition `${directiveToAdd.name}`", directiveToAdd.sourceLocation))
        continue
      } else if (!definition.repeatable) {
        issues.add(OtherValidationIssue("Cannot add non-repeatable directive `${directiveToAdd.name}`", directiveToAdd.sourceLocation))
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
      issues.add(OtherValidationIssue("Cannot merge already existing node `${it.key}`", it.value.first().sourceLocation))
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
      issues.add(OtherValidationIssue("Cannot merge interface ${it.value.first()} as it's already defined", sourceLocation))
    }
  }
}

private fun ExtensionsMerger.mergeFields(
    list: List<GQLFieldDefinition>,
    others: List<GQLFieldDefinition>,
    extraDirectives: Map<String, List<GQLDirective>> = emptyMap(),
): List<GQLFieldDefinition> {

  val result = list.toMutableList()

  others.forEach { newFieldDefinition ->
    val index = result.indexOfFirst { it.name == newFieldDefinition.name }
    if (index == -1) {
      // field doesn't exist, add it
      result.add(newFieldDefinition)
    } else {
      if (!mergeOptions.allowMergingFieldDefinitions) {
        issues.add(OtherValidationIssue("There is already a field definition named `${newFieldDefinition.name}` for this type", newFieldDefinition.sourceLocation))
        return@forEach
      }

      val mergedFieldDefinition = mergeFieldDefinition(issues, result[index], newFieldDefinition)
      if (mergedFieldDefinition != null) {
        result[index] = mergedFieldDefinition
      }
    }
  }

  return result.map {
    it.copy(directives = mergeDirectives(it.directives, extraDirectives.get(it.name).orEmpty()))
  }
}

private fun GQLType.isCompatibleWith(other: GQLType): Boolean {
  var a = this
  var b = other

  if (a is GQLNonNullType) {
    a = a.type
  }
  if (b is GQLNonNullType) {
    b = b.type
  }

  return when (a) {
    is GQLListType -> {
      if (b is GQLListType) {
        a.type.isCompatibleWith(b.type)
      } else {
        false
      }
    }

    is GQLNamedType -> {
      if (b !is GQLNamedType) {
        false
      } else {
        b.name == a.name
      }
    }

    is GQLNonNullType -> {
      error("")
    }
  }
}

private fun areEqual(a: GQLType, b: GQLType): Boolean {
  when (a) {
    is GQLListType -> {
      if (b !is GQLNonNullType) {
        return false
      }
      return areEqual(a.type, b.type)
    }

    is GQLNamedType -> {
      if (b !is GQLNamedType) {
        return false
      }
      return a.name == b.name
    }

    is GQLNonNullType -> {
      if (b !is GQLNonNullType) {
        return false
      }
      return areEqual(a.type, b.type)
    }
  }
}

internal fun areEqual(a: GQLValue?, b: GQLValue?): Boolean {
  return when (a) {
    null -> {
      b == null
    }
    is GQLBooleanValue -> {
      if (b !is GQLBooleanValue){
         false
      } else {
        a.value == b.value
      }
    }
    is GQLEnumValue -> {
      if (b !is GQLEnumValue) {
        false
      } else {
        a.value == b.value
      }
    }
    is GQLFloatValue -> {
      if (b !is GQLFloatValue) {
        false
      } else {
        a.value == b.value
      }
    }
    is GQLIntValue -> {
      if (b !is GQLIntValue) {
        false
      } else {
        a.value == b.value
      }
    }
    is GQLListValue -> {
      if (b !is GQLListValue) {
        false
      } else {
        if (a.values.size != b.values.size) {
          false
        } else {
          for (i in a.values.indices) {
            if (areEqual(a.values.get(i), b.values.get(i))) {
              return false
            }
          }
          true
        }
      }
    }
    is GQLNullValue -> b is GQLNullValue
    is GQLObjectValue -> {
      if (b !is GQLObjectValue) {
        false
      } else {
        // TODO: Can object literals have duplicate fields?
        if (a.fields.size != b.fields.size) {
          false
        } else {
          for (i in a.fields.indices) {
            val aField = a.fields.get(i)
            val bField = b.fields.firstOrNull { it.name == aField.name }
            if (bField == null) {
              return false
            }
            return areEqual(aField.value, bField.value)
          }
          true
        }
      }
    }
    is GQLStringValue -> {
      if (b !is GQLStringValue) {
        false
      } else {
        a.value == b.value
      }
    }
    is GQLVariableValue -> {
      if (b !is GQLVariableValue) {
        false
      } else {
        a.name == b.name
      }
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
      issues.add(OtherValidationIssue("Cannot merge already existing node `${it.key}`", it.value.first().sourceLocation))
    }
  }
}

