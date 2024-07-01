package com.apollographql.apollo.ast


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

fun GQLTypeDefinition.fieldDefinitions(schema: Schema): List<GQLFieldDefinition> {
  return when (this) {
    is GQLObjectTypeDefinition -> {
      if (name == schema.queryTypeDefinition.name) {
        fields + typeMetaFieldDefinition + schemaMetaFieldDefinition + typenameMetaFieldDefinition
      } else {
        fields + typenameMetaFieldDefinition
      }
    }
    is GQLInterfaceTypeDefinition -> {
      fields + typenameMetaFieldDefinition
    }
    is GQLUnionTypeDefinition -> {
      listOf(typenameMetaFieldDefinition)
    }
    else -> emptyList()
  }
}

fun GQLField.definitionFromScope(schema: Schema, parentTypeDefinition: GQLTypeDefinition): GQLFieldDefinition? {
  return parentTypeDefinition.fieldDefinitions(schema).firstOrNull { it.name == name }
}

fun GQLField.responseName() = alias ?: name
