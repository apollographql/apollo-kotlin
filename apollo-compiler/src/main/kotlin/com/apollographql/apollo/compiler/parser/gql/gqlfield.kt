package com.apollographql.apollo.compiler.parser.gql


private val typenameMetaFieldDefinition = GQLFieldDefinition(
    name = "__typename",
    type = GQLNonNullType(type = GQLNamedType(name = "String")),
    directives = emptyList(),
    arguments = emptyList(),
    description = ""
)

private val schemaMetaFieldDefinition = GQLFieldDefinition(
    name = "__schema",
    type = GQLNonNullType(type = GQLNamedType(name = "__Schema")),
    directives = emptyList(),
    arguments = emptyList(),
    description = ""
)

private val typeMetaFieldDefinition = GQLFieldDefinition(
    name = "__type",
    type = GQLNonNullType(type = GQLNamedType(name = "__Type")),
    directives = emptyList(),
    arguments = listOf(
        GQLInputValueDefinition(
            name = "name",
            type = GQLNonNullType(type = GQLNamedType(name = "String")),
            defaultValue = null,
            description = null,
            directives = emptyList()
        )
    ),
    description = ""
)

internal fun GQLField.definitionFromScope(schema: Schema, typeDefinitionInScope: GQLTypeDefinition): GQLFieldDefinition? {
  return when {
    name == "__typename" -> listOf(typenameMetaFieldDefinition)
    name == "__schema" && typeDefinitionInScope.name == schema.queryTypeDefinition.name -> listOf(schemaMetaFieldDefinition)
    name == "__type" && typeDefinitionInScope.name == schema.queryTypeDefinition.name -> listOf(typeMetaFieldDefinition)
    typeDefinitionInScope is GQLObjectTypeDefinition -> typeDefinitionInScope.fields
    typeDefinitionInScope is GQLInterfaceTypeDefinition -> typeDefinitionInScope.fields
    else -> emptyList()
  }.firstOrNull { it.name == name }
}

internal fun GQLField.responseName() = alias ?: name
