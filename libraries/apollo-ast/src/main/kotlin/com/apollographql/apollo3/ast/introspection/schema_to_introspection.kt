package com.apollographql.apollo3.ast.introspection

import com.apollographql.apollo3.ast.GQLBooleanValue
import com.apollographql.apollo3.ast.GQLDirectiveDefinition
import com.apollographql.apollo3.ast.GQLDirectiveLocation
import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.GQLEnumValue
import com.apollographql.apollo3.ast.GQLEnumValueDefinition
import com.apollographql.apollo3.ast.GQLFieldDefinition
import com.apollographql.apollo3.ast.GQLFloatValue
import com.apollographql.apollo3.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLInputValueDefinition
import com.apollographql.apollo3.ast.GQLIntValue
import com.apollographql.apollo3.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo3.ast.GQLListType
import com.apollographql.apollo3.ast.GQLListValue
import com.apollographql.apollo3.ast.GQLNamedType
import com.apollographql.apollo3.ast.GQLNonNullType
import com.apollographql.apollo3.ast.GQLNullValue
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLObjectValue
import com.apollographql.apollo3.ast.GQLScalarTypeDefinition
import com.apollographql.apollo3.ast.GQLStringValue
import com.apollographql.apollo3.ast.GQLType
import com.apollographql.apollo3.ast.GQLTypeDefinition
import com.apollographql.apollo3.ast.GQLUnionTypeDefinition
import com.apollographql.apollo3.ast.GQLValue
import com.apollographql.apollo3.ast.GQLVariableValue
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.findDeprecationReason
import com.apollographql.apollo3.ast.toUtf8

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
            },
            directives = schema.directiveDefinitions.values.map { it.toSchemaDirective() },
        )
    )
  }

  private fun GQLObjectTypeDefinition.toSchemaType(): IntrospectionSchema.Schema.Type.Object {
    return IntrospectionSchema.Schema.Type.Object(
        name = name,
        description = description,
        fields = fields.map { it.toSchemaField() },
        interfaces = implementsInterfaces.map { IntrospectionSchema.Schema.Type.Interface(name = it, description = null, fields = null, possibleTypes = null, interfaces = null) }.ifEmpty { null },
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

  private fun GQLInputValueDefinition.toSchemaArgument(): IntrospectionSchema.Schema.Argument {
    val deprecationReason = directives.findDeprecationReason()

    return IntrospectionSchema.Schema.Argument(
        name = name,
        description = description,
        isDeprecated = deprecationReason != null,
        deprecationReason = deprecationReason,
        type = type.toSchemaType(schema),
        defaultValue = defaultValue?.toUtf8(indent = "")
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
        defaultValue = defaultValue?.toUtf8(indent = ""),
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
            },
        interfaces = implementsInterfaces.map { interfaceName ->
          IntrospectionSchema.Schema.TypeRef(
              kind = IntrospectionSchema.Schema.Kind.INTERFACE,
              name = interfaceName
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

  private fun GQLDirectiveDefinition.toSchemaDirective() = IntrospectionSchema.Schema.Directive(
      name = name,
      description = description,
      locations = locations.map { it.toSchemaDirectiveLocation() },
      args = arguments.map { it.toSchemaArgument() },
      isRepeatable = repeatable,
  )

  private fun GQLDirectiveLocation.toSchemaDirectiveLocation() = when (this) {
    GQLDirectiveLocation.QUERY -> IntrospectionSchema.Schema.Directive.DirectiveLocation.QUERY
    GQLDirectiveLocation.MUTATION -> IntrospectionSchema.Schema.Directive.DirectiveLocation.MUTATION
    GQLDirectiveLocation.SUBSCRIPTION -> IntrospectionSchema.Schema.Directive.DirectiveLocation.SUBSCRIPTION
    GQLDirectiveLocation.FIELD -> IntrospectionSchema.Schema.Directive.DirectiveLocation.FIELD
    GQLDirectiveLocation.FRAGMENT_DEFINITION -> IntrospectionSchema.Schema.Directive.DirectiveLocation.FRAGMENT_DEFINITION
    GQLDirectiveLocation.FRAGMENT_SPREAD -> IntrospectionSchema.Schema.Directive.DirectiveLocation.FRAGMENT_SPREAD
    GQLDirectiveLocation.INLINE_FRAGMENT -> IntrospectionSchema.Schema.Directive.DirectiveLocation.INLINE_FRAGMENT
    GQLDirectiveLocation.VARIABLE_DEFINITION -> IntrospectionSchema.Schema.Directive.DirectiveLocation.VARIABLE_DEFINITION
    GQLDirectiveLocation.SCHEMA -> IntrospectionSchema.Schema.Directive.DirectiveLocation.SCHEMA
    GQLDirectiveLocation.SCALAR -> IntrospectionSchema.Schema.Directive.DirectiveLocation.SCALAR
    GQLDirectiveLocation.OBJECT -> IntrospectionSchema.Schema.Directive.DirectiveLocation.OBJECT
    GQLDirectiveLocation.FIELD_DEFINITION -> IntrospectionSchema.Schema.Directive.DirectiveLocation.FIELD_DEFINITION
    GQLDirectiveLocation.ARGUMENT_DEFINITION -> IntrospectionSchema.Schema.Directive.DirectiveLocation.ARGUMENT_DEFINITION
    GQLDirectiveLocation.INTERFACE -> IntrospectionSchema.Schema.Directive.DirectiveLocation.INTERFACE
    GQLDirectiveLocation.UNION -> IntrospectionSchema.Schema.Directive.DirectiveLocation.UNION
    GQLDirectiveLocation.ENUM -> IntrospectionSchema.Schema.Directive.DirectiveLocation.ENUM
    GQLDirectiveLocation.ENUM_VALUE -> IntrospectionSchema.Schema.Directive.DirectiveLocation.ENUM_VALUE
    GQLDirectiveLocation.INPUT_OBJECT -> IntrospectionSchema.Schema.Directive.DirectiveLocation.INPUT_OBJECT
    GQLDirectiveLocation.INPUT_FIELD_DEFINITION -> IntrospectionSchema.Schema.Directive.DirectiveLocation.INPUT_FIELD_DEFINITION
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

private fun GQLValue.toKotlinValue(constContext: Boolean): Any? {
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
      error("Default values cannot contain variables")
    }
  }
}
