package com.apollographql.apollo3.ast

internal class TypeExtensionsMergeScope {
  private val issues = mutableListOf<Issue>()

  fun mergeDocumentTypeExtensions(definitions: List<GQLDefinition>): List<GQLDefinition> {
    val (extensions, otherDefinitions) = definitions.partition { it is GQLTypeSystemExtension }

    return extensions.fold(otherDefinitions) { acc, extension ->
      when (extension) {
        is GQLSchemaExtension -> acc.mergeSchemaExtension(schemaExtension = extension)
        is GQLScalarTypeExtension -> acc.merge<GQLScalarTypeDefinition, GQLScalarTypeExtension>(extension) { it.merge(extension) }
        is GQLInterfaceTypeExtension -> acc.merge<GQLInterfaceTypeDefinition, GQLInterfaceTypeExtension>(extension) { it.merge(extension) }
        is GQLObjectTypeExtension -> acc.merge<GQLObjectTypeDefinition, GQLObjectTypeExtension>(extension) { it.merge(extension) }
        is GQLInputObjectTypeExtension -> acc.merge<GQLInputObjectTypeDefinition, GQLInputObjectTypeExtension>(extension) { it.merge(extension) }
        is GQLEnumTypeExtension -> acc.merge<GQLEnumTypeDefinition, GQLEnumTypeExtension>(extension) { it.merge(extension) }
        is GQLUnionTypeExtension -> acc.merge<GQLUnionTypeDefinition, GQLUnionTypeExtension>(extension) { it.merge(extension) }
        else -> throw UnrecognizedAntlrRule("Unrecognized type system extension", extension.sourceLocation)
      }
    }
  }

  private fun GQLUnionTypeDefinition.merge(extension: GQLUnionTypeExtension): GQLUnionTypeDefinition {
    return copy(
        directives = directives.mergeUniquesOrThrow(extension.directives),
        memberTypes = memberTypes.mergeUniquesOrThrow(extension.memberTypes)
    )
  }

  private fun GQLEnumTypeDefinition.merge(extension: GQLEnumTypeExtension): GQLEnumTypeDefinition {
    return copy(
        directives = directives.mergeUniquesOrThrow(extension.directives),
        enumValues = enumValues.mergeUniquesOrThrow(extension.enumValues),
    )
  }

  private fun GQLInputObjectTypeDefinition.merge(extension: GQLInputObjectTypeExtension): GQLInputObjectTypeDefinition {
    return copy(
        directives = directives.mergeUniquesOrThrow(extension.directives),
        inputFields = inputFields.mergeUniquesOrThrow(extension.inputFields)
    )
  }

  private fun GQLObjectTypeDefinition.merge(extension: GQLObjectTypeExtension): GQLObjectTypeDefinition {
    return copy(
        directives = directives.mergeUniquesOrThrow(extension.directives),
        fields = fields.mergeUniquesOrThrow(extension.fields),
    )
  }

  private fun GQLInterfaceTypeDefinition.merge(extension: GQLInterfaceTypeExtension): GQLInterfaceTypeDefinition {
    return copy(
        fields = fields.mergeUniquesOrThrow(extension.fields)
    )
  }

  private fun List<GQLDefinition>.mergeSchemaExtension(schemaExtension: GQLSchemaExtension): List<GQLDefinition> {
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
      issues.add(Issue.ValidationError("Cannot apply schema extension on non existing schema definition", schemaExtension.sourceLocation))
    }
    return definitions
  }

  private fun GQLScalarTypeDefinition.merge(scalarTypeExtension: GQLScalarTypeExtension): GQLScalarTypeDefinition {
    return copy(
        directives = directives.mergeUniquesOrThrow(scalarTypeExtension.directives)
    )
  }

  private inline fun <reified T, E> List<GQLDefinition>.merge(
      extension: E,
      merge: (T) -> T,
  ): List<GQLDefinition> where T : GQLDefinition, T : GQLNamed, E : GQLNamed, E : GQLNode {
    var found = false
    val definitions = mutableListOf<GQLDefinition>()
    forEach {
      if (it is T && it.name == extension.name) {
        if (found) {
          issues.add(Issue.ValidationError("Multiple '${extension.name}' types found while merging extensions. This is a bug, check validation code", extension.sourceLocation))
        }
        definitions.add(merge(it))
        found = true
      } else {
        definitions.add(it)
      }
    }
    if (!found) {
      issues.add(Issue.ValidationError("Cannot find type `${extension.name}` to apply extension", extension.sourceLocation))
    }
    return definitions
  }

  private fun GQLSchemaDefinition.merge(extension: GQLSchemaExtension): GQLSchemaDefinition {
    return copy(
        directives = directives.mergeUniquesOrThrow(extension.directives),
        rootOperationTypeDefinitions = rootOperationTypeDefinitions.mergeUniquesOrThrow(extension.operationTypesDefinition) { it.operationType }
    )
  }

  private inline fun <reified T> List<T>.mergeUniquesOrThrow(others: List<T>): List<T> where T : GQLNamed, T : GQLNode {
    return (this + others).apply {
      groupBy { it.name }.entries.firstOrNull { it.value.size > 1 }?.let {
        issues.add(Issue.ValidationError("Cannot merge already existing node ${T::class.java.simpleName} `${it.key}`", it.value.first().sourceLocation))
      }
    }
  }


  private inline fun <reified T : GQLNode> List<T>.mergeUniquesOrThrow(others: List<T>, name: (T) -> String): List<T> {
    return (this + others).apply {
      groupBy { name(it) }.entries.firstOrNull { it.value.size > 1 }?.let {
        issues.add(Issue.ValidationError("Cannot merge already existing node ${T::class.java.simpleName} `${it.key}`", it.value.first().sourceLocation))
      }
    }
  }
}
