package com.apollographql.apollo3.ast


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

fun GQLField.definitionFromScope(schema: Schema, rawTypename: String): GQLFieldDefinition? {
  return definitionFromScope(schema, schema.typeDefinition(rawTypename))
}

fun GQLField.definitionFromScope(schema: Schema, parentTypeDefinition: GQLTypeDefinition): GQLFieldDefinition? {
  return when {
    name == "__typename" -> listOf(typenameMetaFieldDefinition)
    name == "__schema" && parentTypeDefinition.name == schema.queryTypeDefinition.name -> listOf(schemaMetaFieldDefinition)
    name == "__type" && parentTypeDefinition.name == schema.queryTypeDefinition.name -> listOf(typeMetaFieldDefinition)
    parentTypeDefinition is GQLObjectTypeDefinition -> parentTypeDefinition.fields
    parentTypeDefinition is GQLInterfaceTypeDefinition -> parentTypeDefinition.fields
    else -> emptyList()
  }.firstOrNull { it.name == name }
}

fun GQLField.responseName() = alias ?: name
