package com.apollographql.apollo.compiler.parser.gql

import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema

private class IntrospectionSchemaBuilder(private val schema: Schema) {
  private val typeDefinitions = schema.typeDefinitions

  fun toIntrospectionSchema(): IntrospectionSchema {
    return IntrospectionSchema(
        queryType = schema.queryTypeDefinition.name,
        mutationType = schema.mutationTypeDefinition?.name,
        subscriptionType = schema.subscriptionTypeDefinition?.name,
        types = typeDefinitions.values.map {
          it.name to when (it) {
            is GQLObjectTypeDefinition -> it.toSchemaType()
            is GQLInputObjectTypeDefinition -> it.toSchemaType()
            is GQLInterfaceTypeDefinition -> it.toSchemaType()
            is GQLScalarTypeDefinition -> it.toSchemaType()
            is GQLEnumTypeDefinition -> it.toSchemaType()
            is GQLUnionTypeDefinition -> it.toSchemaType()
          }
        }.toMap()
    )
  }

  private fun GQLObjectTypeDefinition.toSchemaType(): IntrospectionSchema.Type.Object {
    return IntrospectionSchema.Type.Object(
        name = name,
        description = description,
        fields = fields.map { it.toSchemaField() }
    )
  }

  private fun GQLFieldDefinition.toSchemaField(): IntrospectionSchema.Field {
    val deprecationReason = directives.findDeprecationReason()
    return IntrospectionSchema.Field(
        name = name,
        description = description,
        isDeprecated = deprecationReason != null,
        deprecationReason = deprecationReason,
        type = type.toSchemaType(),
        args = arguments.map { it.toSchemaArgument() }
    )
  }

  private fun GQLDocument.typeDefinition(name: String): GQLTypeDefinition? {
    return definitions.filterIsInstance<GQLTypeDefinition>().firstOrNull { it.name == name }
  }

  private fun GQLType.toSchemaType(): IntrospectionSchema.TypeRef {
    return when (this) {
      is GQLNonNullType -> {
        IntrospectionSchema.TypeRef(
            kind = IntrospectionSchema.Kind.NON_NULL,
            name = "", // why "" and not null ?
            ofType = type.toSchemaType()
        )
      }
      is GQLListType -> {
        IntrospectionSchema.TypeRef(
            kind = IntrospectionSchema.Kind.LIST,
            name = "", // why "" and not null ?
            ofType = type.toSchemaType())
      }
      is GQLNamedType -> {
        val typeDefinition = typeDefinitions[name] ?: throw ConversionException(
            message = "Undefined GraphQL schema type `$name`",
            sourceLocation = sourceLocation
        )
        IntrospectionSchema.TypeRef(
            kind = typeDefinition.schemaKind(),
            name = name,
            ofType = null
        )
      }
    }
  }

  private fun GQLDocument.rootOperationTypeName(operationType: String): String? {
    val schemaDefinition = definitions.filterIsInstance<GQLSchemaDefinition>()
        .firstOrNull()
    if (schemaDefinition == null) {
      // 3.3.1
      // No schema definition, look for an object type named after the operationType
      // i.e. Query, Mutation, ...
      return definitions.filterIsInstance<GQLObjectTypeDefinition>()
          .firstOrNull { it.name == operationType.capitalize() }
          ?.name
    }
    val namedType = schemaDefinition.rootOperationTypeDefinitions.firstOrNull {
      it.operationType == operationType
    }?.namedType

    return namedType
  }

  private fun GQLTypeDefinition.schemaKind() = when (this) {
    is GQLEnumTypeDefinition -> IntrospectionSchema.Kind.ENUM
    is GQLUnionTypeDefinition -> IntrospectionSchema.Kind.UNION
    is GQLObjectTypeDefinition -> IntrospectionSchema.Kind.OBJECT
    is GQLInputObjectTypeDefinition -> IntrospectionSchema.Kind.INPUT_OBJECT
    is GQLScalarTypeDefinition -> IntrospectionSchema.Kind.SCALAR
    is GQLInterfaceTypeDefinition -> IntrospectionSchema.Kind.INTERFACE
  }

  private fun GQLInputValueDefinition.toSchemaArgument(): IntrospectionSchema.Field.Argument {
    val deprecationReason = directives.findDeprecationReason()

    return IntrospectionSchema.Field.Argument(
        name = name,
        description = description,
        isDeprecated = deprecationReason != null,
        deprecationReason = deprecationReason,
        type = type.toSchemaType(),
        defaultValue = defaultValue?.toKotlinValue(true) // TODO: difference between null and absent
    )
  }

  private fun GQLInputObjectTypeDefinition.toSchemaType(): IntrospectionSchema.Type.InputObject {
    return IntrospectionSchema.Type.InputObject(
        name = name,
        description = description,
        inputFields = inputFields.map { it.toSchemaInputField() }
    )
  }

  private fun GQLInputValueDefinition.toSchemaInputField(): IntrospectionSchema.InputField {
    val deprecationReason = directives.findDeprecationReason()
    return IntrospectionSchema.InputField(
        name = name,
        description = description,
        isDeprecated = deprecationReason != null,
        deprecationReason = deprecationReason,
        type = type.toSchemaType(),
        defaultValue = defaultValue?.toKotlinValue(true),
    )
  }

  private fun GQLInterfaceTypeDefinition.toSchemaType(): IntrospectionSchema.Type.Interface {
    return IntrospectionSchema.Type.Interface(
        name = name,
        description = description,
        fields = fields.map { it.toSchemaField() },
        possibleTypes = typeDefinitions.values
            .filter { typeDefinition ->
              typeDefinition is GQLObjectTypeDefinition && typeDefinition.implementsInterfaces.contains(name)
            }
            .map { typeDefinition ->
              IntrospectionSchema.TypeRef(
                  kind = IntrospectionSchema.Kind.OBJECT,
                  name = typeDefinition.name
              )
            }
    )
  }

  private fun GQLEnumTypeDefinition.toSchemaType(): IntrospectionSchema.Type.Enum {
    return IntrospectionSchema.Type.Enum(
        name = name,
        description = description,
        enumValues = enumValues.map { it.toSchemaEnumValue() }
    )
  }

  private fun GQLEnumValueDefinition.toSchemaEnumValue(): IntrospectionSchema.Type.Enum.Value {
    val deprecationReason = directives.findDeprecationReason()
    return IntrospectionSchema.Type.Enum.Value(
        name = name,
        description = description,
        isDeprecated = deprecationReason != null,
        deprecationReason = deprecationReason
    )
  }

  private fun GQLScalarTypeDefinition.toSchemaType(): IntrospectionSchema.Type.Scalar {
    return IntrospectionSchema.Type.Scalar(
        name = name,
        description = description,
    )
  }

  private fun GQLUnionTypeDefinition.toSchemaType(): IntrospectionSchema.Type.Union {
    return IntrospectionSchema.Type.Union(
        name = name,
        description = description,
        fields = null,
        possibleTypes = memberTypes.map { it.toSchemaType() }
    )
  }
}

fun Schema.toIntrospectionSchema() = IntrospectionSchemaBuilder(this).toIntrospectionSchema()


