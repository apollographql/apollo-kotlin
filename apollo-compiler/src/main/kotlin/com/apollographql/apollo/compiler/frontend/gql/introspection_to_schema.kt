package com.apollographql.apollo.compiler.frontend.gql

import com.apollographql.apollo.compiler.introspection.IntrospectionSchema

private class GQLDocumentBuilder(private val introspectionSchema: IntrospectionSchema) {

  fun toGQLDocument(): GQLDocument {
    return with(introspectionSchema) {
      GQLDocument(
          definitions = types.values.map {
            when (it) {
              is IntrospectionSchema.Type.Union -> it.toGQLUnionTypeDefinition()
              is IntrospectionSchema.Type.Interface -> it.toGQLInterfaceTypeDefinition()
              is IntrospectionSchema.Type.Enum -> it.toGQLEnumTypeDefinition()
              is IntrospectionSchema.Type.Object -> it.toGQLObjectTypeDefinition()
              is IntrospectionSchema.Type.InputObject -> it.toGQLInputObjectTypeDefinition()
              is IntrospectionSchema.Type.Scalar -> it.toGQLScalarTypeDefinition()
            }
          } + schemaDefinition(),
          filePath = null
      )
    }
  }

  private fun IntrospectionSchema.Type.Object.toGQLObjectTypeDefinition(): GQLObjectTypeDefinition {
    return GQLObjectTypeDefinition(
        description = description,
        name = name,
        directives = emptyList(),
        fields = fields?.map { it.toGQLFieldDefinition() } ?: throw ConversionException("Object '$name' did not define any field"),
        implementsInterfaces = findInterfacesImplementedBy(name)
    )
  }

  private fun findInterfacesImplementedBy(name: String): List<String> {
    return introspectionSchema.types.values.filterIsInstance<IntrospectionSchema.Type.Interface>()
        .filter {
          it.possibleTypes?.map { it.name }?.contains(name) == true
        }
        .map {
          it.name
        }
  }

  private fun IntrospectionSchema.Type.Enum.toGQLEnumTypeDefinition(): GQLEnumTypeDefinition {
    return GQLEnumTypeDefinition(
        description = description,
        name = name,
        enumValues = enumValues.map { it.toGQLEnumValueDefinition() },
        directives = emptyList()
    )
  }

  private fun IntrospectionSchema.Type.Enum.Value.toGQLEnumValueDefinition(): GQLEnumValueDefinition {
    return GQLEnumValueDefinition(
        description = description,
        name = name,
        directives = makeDirectives(deprecationReason)
    )
  }

  private fun IntrospectionSchema.Type.Interface.toGQLInterfaceTypeDefinition(): GQLInterfaceTypeDefinition {
    return GQLInterfaceTypeDefinition(
        name = name,
        description = description,
        fields = fields?.map { it.toGQLFieldDefinition() } ?: throw ConversionException("interface '$name' did not define any field"),
        implementsInterfaces = emptyList(), // TODO
        directives = emptyList()
    )
  }

  private fun IntrospectionSchema.Field.toGQLFieldDefinition(): GQLFieldDefinition {
    return GQLFieldDefinition(
        name = name,
        description = description,
        arguments = this.args.map { it.toGQLInputValueDefinition() },
        directives = makeDirectives(deprecationReason),
        type = type.toGQLType()
    )
  }

  private fun IntrospectionSchema.Field.Argument.toGQLInputValueDefinition(): GQLInputValueDefinition {
    return GQLInputValueDefinition(
        name = name,
        description = description,
        directives = makeDirectives(deprecationReason),
        defaultValue = defaultValue.toGQLValue(),
        type = type.toGQLType(),
    )
  }

  private fun Any?.toGQLValue(): GQLValue? {
    if (this == null) {
      // no default value
      return null
    }
    try {
      if (this is String) {
        return GraphQLParser.parseValue(this).orThrow()
      }
    } catch (e: Exception) {
      println("Wrongly encoded default value: $this: ${e.message}")
    }

    // All the below should theorically not happen because the spec
    return when {
      this is String -> GQLStringValue(value = this)
      this is Int -> GQLIntValue(value = this)
      this is Long -> GQLIntValue(value = this.toInt())
      this is Double -> GQLFloatValue(value = this)
      this is Boolean -> GQLBooleanValue(value = this)
      this is Map<*, *> -> GQLObjectValue(fields = this.map {
        GQLObjectField(name = it.key as String, value = it.value.toGQLValue()!!)
      })
      this is List<*> -> GQLListValue(values = map { it.toGQLValue()!! })
      else -> throw ConversionException("cannot convert $this to a GQLValue")
    }
  }

  fun makeDirectives(deprecationReason: String?): List<GQLDirective> {
    if (deprecationReason == null) {
      return emptyList()
    }
    return listOf(
        GQLDirective(
            name = "deprecated",
            arguments = GQLArguments(listOf(
                GQLArgument(name = "reason", value = GQLStringValue(value = deprecationReason))
            ))
        )
    )
  }

  private fun IntrospectionSchema.Type.Union.toGQLUnionTypeDefinition(): GQLUnionTypeDefinition {
    return GQLUnionTypeDefinition(
        name = name,
        description = "",
        memberTypes = possibleTypes?.map { it.toGQLNamedType() } ?: throw ConversionException("Union '$name' must have members"),
        directives = emptyList(),
    )
  }

  private fun IntrospectionSchema.TypeRef.toGQLNamedType(): GQLNamedType {
    return toGQLType() as? GQLNamedType ?: throw ConversionException("expected a NamedType")
  }

  private fun IntrospectionSchema.TypeRef.toGQLType(): GQLType {
    return when (this.kind) {
      IntrospectionSchema.Kind.NON_NULL -> GQLNonNullType(
          type = ofType?.toGQLType() ?: throw ConversionException("ofType must not be null for non null types")
      )
      IntrospectionSchema.Kind.LIST -> GQLListType(
          type = ofType?.toGQLType() ?: throw ConversionException("ofType must not be null for list types")
      )
      else -> GQLNamedType(
          name = name!!
      )
    }
  }

  private fun IntrospectionSchema.schemaDefinition(): GQLSchemaDefinition {
    val rootOperationTypeDefinitions = mutableListOf<GQLOperationTypeDefinition>()
    rootOperationTypeDefinitions.add(
        GQLOperationTypeDefinition(
            operationType = "query",
            namedType = queryType
        )
    )
    if (mutationType != null) {
      rootOperationTypeDefinitions.add(
          GQLOperationTypeDefinition(
              operationType = "mutation",
              namedType = mutationType
          )
      )
    }
    if (subscriptionType != null) {
      rootOperationTypeDefinitions.add(
          GQLOperationTypeDefinition(
              operationType = "subscription",
              namedType = subscriptionType
          )
      )
    }

    return GQLSchemaDefinition(
        description = "",
        directives = emptyList(),
        rootOperationTypeDefinitions = rootOperationTypeDefinitions
    )
  }

  private fun IntrospectionSchema.Type.InputObject.toGQLInputObjectTypeDefinition(): GQLInputObjectTypeDefinition {
    return GQLInputObjectTypeDefinition(
        description = description,
        name = name,
        inputFields = inputFields.map { it.toGQLInputValueDefinition() },
        directives = emptyList()
    )
  }

  private fun IntrospectionSchema.InputField.toGQLInputValueDefinition(): GQLInputValueDefinition {
    return GQLInputValueDefinition(
        description = description,
        name = name,
        directives = makeDirectives(deprecationReason),
        type = type.toGQLType(),
        defaultValue = defaultValue.toGQLValue()
    )
  }

  private fun IntrospectionSchema.Type.Scalar.toGQLScalarTypeDefinition(): GQLScalarTypeDefinition {
    return GQLScalarTypeDefinition(
        description = description,
        name = name,
        directives = emptyList()
    )
  }
}

private fun IntrospectionSchema.toGQLDocument(): GQLDocument = GQLDocumentBuilder(this).toGQLDocument()

fun IntrospectionSchema.toSchema(): Schema = toGQLDocument()
    .withBuiltinTypes()
    .toSchema()


