package com.apollographql.apollo3.compiler.introspection

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.compiler.introspection.IntrospectionSchema
import com.apollographql.apollo3.ast.*

@OptIn(ApolloExperimental::class)
private class IntrospectionSchemaBuilder(private val schema: Schema) {
  private val typeDefinitions = schema.typeDefinitions

  fun toIntrospectionSchema(): IntrospectionSchema {
    return IntrospectionSchema(
        __schema = IntrospectionSchema.Schema(
            queryType = IntrospectionSchema.Schema.QueryType(schema.queryTypeDefinition.name),
            mutationType = schema.mutationTypeDefinition?.name?.let { IntrospectionSchema.Schema.MutationType(it) },
            subscriptionType = schema.subscriptionTypeDefinition?.name?.let { IntrospectionSchema.Schema.SubscriptionType(it) },
            types = typeDefinitions.values.map {
              when (it) {
                is GQLObjectTypeDefinition -> it.toSchemaType()
                is GQLInputObjectTypeDefinition -> it.toSchemaType()
                is GQLInterfaceTypeDefinition -> it.toSchemaType()
                is GQLScalarTypeDefinition -> it.toSchemaType()
                is GQLEnumTypeDefinition -> it.toSchemaType()
                is GQLUnionTypeDefinition -> it.toSchemaType()
              }
            }
        )
    )
  }

  private fun GQLObjectTypeDefinition.toSchemaType(): IntrospectionSchema.Schema.Type.Object {
    return IntrospectionSchema.Schema.Type.Object(
        name = name,
        description = description,
        fields = fields.map { it.toSchemaField() }
    )
  }

  private fun GQLFieldDefinition.toSchemaField(): IntrospectionSchema.Schema.Field {
    val deprecationReason = directives.findDeprecationReason()
    return IntrospectionSchema.Schema.Field(
        name = name,
        description = description,
        isDeprecated = deprecationReason != null,
        deprecationReason = deprecationReason,
        type = type.toSchemaType(schema),
        args = arguments.map { it.toSchemaArgument() }
    )
  }

  private fun GQLInputValueDefinition.toSchemaArgument(): IntrospectionSchema.Schema.Field.Argument {
    val deprecationReason = directives.findDeprecationReason()

    return IntrospectionSchema.Schema.Field.Argument(
        name = name,
        description = description,
        isDeprecated = deprecationReason != null,
        deprecationReason = deprecationReason,
        type = type.toSchemaType(schema),
        defaultValue = defaultValue?.toUtf8()
    )
  }

  private fun GQLInputObjectTypeDefinition.toSchemaType(): IntrospectionSchema.Schema.Type.InputObject {
    return IntrospectionSchema.Schema.Type.InputObject(
        name = name,
        description = description,
        inputFields = inputFields.map { it.toSchemaInputField() }
    )
  }

  private fun GQLInputValueDefinition.toSchemaInputField(): IntrospectionSchema.Schema.InputField {
    val deprecationReason = directives.findDeprecationReason()
    return IntrospectionSchema.Schema.InputField(
        name = name,
        description = description,
        isDeprecated = deprecationReason != null,
        deprecationReason = deprecationReason,
        type = type.toSchemaType(schema),
        defaultValue = defaultValue?.toKotlinValue(true),
    )
  }

  private fun GQLInterfaceTypeDefinition.toSchemaType(): IntrospectionSchema.Schema.Type.Interface {
    return IntrospectionSchema.Schema.Type.Interface(
        name = name,
        description = description,
        fields = fields.map { it.toSchemaField() },
        possibleTypes = typeDefinitions.values
            .filter { typeDefinition ->
              typeDefinition is GQLObjectTypeDefinition && typeDefinition.implementsInterfaces.contains(name)
            }
            .map { typeDefinition ->
              IntrospectionSchema.Schema.TypeRef(
                  kind = IntrospectionSchema.Schema.Kind.OBJECT,
                  name = typeDefinition.name
              )
            }
    )
  }

  private fun GQLEnumTypeDefinition.toSchemaType(): IntrospectionSchema.Schema.Type.Enum {
    return IntrospectionSchema.Schema.Type.Enum(
        name = name,
        description = description,
        enumValues = enumValues.map { it.toSchemaEnumValue() }
    )
  }

  private fun GQLEnumValueDefinition.toSchemaEnumValue(): IntrospectionSchema.Schema.Type.Enum.Value {
    val deprecationReason = directives.findDeprecationReason()
    return IntrospectionSchema.Schema.Type.Enum.Value(
        name = name,
        description = description,
        isDeprecated = deprecationReason != null,
        deprecationReason = deprecationReason
    )
  }

  private fun GQLScalarTypeDefinition.toSchemaType(): IntrospectionSchema.Schema.Type.Scalar {
    return IntrospectionSchema.Schema.Type.Scalar(
        name = this.name,
        description = this.description
    )
  }

  private fun GQLUnionTypeDefinition.toSchemaType(): IntrospectionSchema.Schema.Type.Union {
    return IntrospectionSchema.Schema.Type.Union(
        name = name,
        description = description,
        fields = null,
        possibleTypes = memberTypes.map { it.toSchemaType(schema) }
    )
  }
}

internal fun GQLType.toSchemaType(schema: Schema): IntrospectionSchema.Schema.TypeRef {
  return when (this) {
    is GQLNonNullType -> {
      IntrospectionSchema.Schema.TypeRef(
          kind = IntrospectionSchema.Schema.Kind.NON_NULL,
          name = "", // why "" and not null ?
          ofType = type.toSchemaType(schema)
      )
    }
    is GQLListType -> {
      IntrospectionSchema.Schema.TypeRef(
          kind = IntrospectionSchema.Schema.Kind.LIST,
          name = "", // why "" and not null ?
          ofType = type.toSchemaType(schema))
    }
    is GQLNamedType -> {
      IntrospectionSchema.Schema.TypeRef(
          kind = schema.typeDefinition(name).schemaKind(),
          name = name,
          ofType = null
      )
    }
  }
}

internal fun GQLTypeDefinition.schemaKind() = when (this) {
  is GQLEnumTypeDefinition -> IntrospectionSchema.Schema.Kind.ENUM
  is GQLUnionTypeDefinition -> IntrospectionSchema.Schema.Kind.UNION
  is GQLObjectTypeDefinition -> IntrospectionSchema.Schema.Kind.OBJECT
  is GQLInputObjectTypeDefinition -> IntrospectionSchema.Schema.Kind.INPUT_OBJECT
  is GQLScalarTypeDefinition -> IntrospectionSchema.Schema.Kind.SCALAR
  is GQLInterfaceTypeDefinition -> IntrospectionSchema.Schema.Kind.INTERFACE
}

fun Schema.toIntrospectionSchema() = IntrospectionSchemaBuilder(this).toIntrospectionSchema()


fun GQLValue.toKotlinValue(constContext: Boolean): Any? {
  return when (this) {
    is GQLIntValue -> value
    is GQLFloatValue -> value
    is GQLStringValue -> value
    is GQLNullValue -> null
    is GQLListValue -> values.map { it.toKotlinValue(constContext) }
    is GQLObjectValue -> fields.map { it.name to it.value.toKotlinValue(constContext) }.toMap()
    is GQLBooleanValue -> value
    is GQLEnumValue -> value // Could we use something else in Kotlin?
    is GQLVariableValue -> {
      if (constContext) {
        throw ConversionException("Value cannot be a variable in a const context", sourceLocation)
      } else {
        BooleanExpression.Element(name)
      }
    }
  }
}
