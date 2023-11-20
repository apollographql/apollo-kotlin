package com.apollographql.apollo3.execution

import com.apollographql.apollo3.api.getOrElse
import com.apollographql.apollo3.ast.GQLDirectiveDefinition
import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.GQLEnumValueDefinition
import com.apollographql.apollo3.ast.GQLFieldDefinition
import com.apollographql.apollo3.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLInputValueDefinition
import com.apollographql.apollo3.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo3.ast.GQLListType
import com.apollographql.apollo3.ast.GQLNamedType
import com.apollographql.apollo3.ast.GQLNonNullType
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLScalarTypeDefinition
import com.apollographql.apollo3.ast.GQLType
import com.apollographql.apollo3.ast.GQLTypeDefinition
import com.apollographql.apollo3.ast.GQLUnionTypeDefinition
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.findDeprecationReason
import com.apollographql.apollo3.ast.findSpecifiedBy
import com.apollographql.apollo3.ast.toUtf8

private inline fun <reified T> Any.cast() = this as T

private object SchemaObject

internal class IntrospectionResolvers(val schema: Schema) {

    private val typenames = mapOf(
        SchemaObject::class to "__Schema",
        IntrospectionType::class to "__Type",
        GQLFieldDefinition::class to "__Field",
        GQLEnumValueDefinition::class to "__EnumValue",
        GQLInputValueDefinition::class to "__InputValue",
        GQLDirectiveDefinition::class to "__Directive"
    )

    private val resolvers = mapOf(
        schema.queryTypeDefinition.name to mapOf(
            "__schema" to Resolver { SchemaObject },
            "__type" to Resolver {
                val name = it.getArgument<String>("name").getOrThrow()
                IntrospectionType(GQLNamedType(name = name), schema)
            }
        ),
        "__Schema" to mapOf(
            "description" to Resolver { null },
            "types" to Resolver {
                schema.typeDefinitions.keys.map {
                    IntrospectionType(GQLNamedType(name = it), schema)
                }
            },
            "queryType" to Resolver { IntrospectionType(GQLNamedType(name = schema.queryTypeDefinition.name), schema) },
            "mutationType" to Resolver {
                schema.mutationTypeDefinition?.let {
                    IntrospectionType(
                        GQLNamedType(name = it.name),
                        schema
                    )
                }
            },
            "subscriptionType" to Resolver {
                schema.subscriptionTypeDefinition?.let {
                    IntrospectionType(
                        GQLNamedType(name = it.name),
                        schema
                    )
                }
            },
            "directives" to Resolver { schema.directiveDefinitions.values.toList() },
        ),
        "__Type" to mapOf(
            "kind" to Resolver {
                val type = it.parentObject.cast<IntrospectionType>()
                when (type.typeDefinition) {
                    is GQLEnumTypeDefinition -> __TypeKind.ENUM.name
                    is GQLInputObjectTypeDefinition -> __TypeKind.INPUT_OBJECT.name
                    is GQLInterfaceTypeDefinition -> __TypeKind.INTERFACE.name
                    is GQLObjectTypeDefinition -> __TypeKind.OBJECT.name
                    is GQLScalarTypeDefinition -> __TypeKind.SCALAR.name
                    is GQLUnionTypeDefinition -> __TypeKind.UNION.name
                    null -> {
                        when (type.type) {
                            is GQLNonNullType -> __TypeKind.NON_NULL.name
                            is GQLListType -> __TypeKind.LIST.name
                            else -> null
                        }
                    }
                }
            },
            "name" to Resolver {
                val typeDefinition = it.parentObject.cast<IntrospectionType>().typeDefinition
                typeDefinition?.name
            },
            "description" to Resolver {
                val typeDefinition = it.parentObject.cast<IntrospectionType>().typeDefinition
                typeDefinition?.description
            },
            "specifiedByURL" to Resolver {
                val typeDefinition = it.parentObject.cast<IntrospectionType>().typeDefinition
                typeDefinition?.directives?.findSpecifiedBy()
            },
            "fields" to Resolver {
                val typeDefinition = it.parentObject.cast<IntrospectionType>().typeDefinition
                val definitions = when (typeDefinition) {
                    is GQLObjectTypeDefinition -> typeDefinition.fields
                    is GQLInterfaceTypeDefinition -> typeDefinition.fields
                    else -> null
                }

                if (definitions == null) {
                    return@Resolver null
                }

                val includeDeprecated = it.getArgument<Boolean>("includeDeprecated").getOrElse(false)
                definitions.filter {
                    includeDeprecated || it.directives.findDeprecationReason() == null
                }
            },
            "interfaces" to Resolver {
                val typeDefinition = it.parentObject.cast<IntrospectionType>().typeDefinition
                val interfaces = when (typeDefinition) {
                    is GQLObjectTypeDefinition -> typeDefinition.implementsInterfaces
                    is GQLInterfaceTypeDefinition -> typeDefinition.implementsInterfaces
                    else -> null
                }

                if (interfaces == null) {
                    return@Resolver null
                }

                interfaces
            },
            "possibleTypes" to Resolver {
                val typeDefinition = it.parentObject.cast<IntrospectionType>().typeDefinition
                val possibleTypes = when (typeDefinition) {
                    is GQLInterfaceTypeDefinition -> schema.possibleTypes(typeDefinition.name)
                    is GQLUnionTypeDefinition -> schema.possibleTypes(typeDefinition.name)
                    else -> null
                }

                if (possibleTypes == null) {
                    return@Resolver null
                }

                possibleTypes
            },
            "enumValues" to Resolver {
                val typeDefinition = it.parentObject.cast<IntrospectionType>().typeDefinition
                if (typeDefinition !is GQLEnumTypeDefinition) {
                    return@Resolver null
                }

                val includeDeprecated = it.getArgument<Boolean>("includeDeprecated").getOrElse(false)

                typeDefinition.enumValues.filter {
                    includeDeprecated || it.directives.findDeprecationReason() == null
                }
            },
            "inputFields" to Resolver {
                val typeDefinition = it.parentObject.cast<IntrospectionType>().typeDefinition
                if (typeDefinition !is GQLInputObjectTypeDefinition) {
                    return@Resolver null
                }

                val includeDeprecated = it.getArgument<Boolean>("includeDeprecated").getOrElse(false)

                typeDefinition.inputFields.filter {
                    includeDeprecated || it.directives.findDeprecationReason() == null
                }
            },
            "ofType" to Resolver {
                val type = it.parentObject.cast<IntrospectionType>()
                when (type.type) {
                    is GQLNamedType -> null
                    is GQLListType -> IntrospectionType(type.type.type, schema)
                    is GQLNonNullType -> IntrospectionType(type.type.type, schema)
                }
            }
        ),
        "__Field" to mapOf(
            "name" to Resolver {
                val fieldDefinition = it.parentObject.cast<GQLFieldDefinition>()
                fieldDefinition.name
            },
            "description" to Resolver {
                val fieldDefinition = it.parentObject.cast<GQLFieldDefinition>()
                fieldDefinition.description
            },
            "args" to Resolver {
                val includeDeprecated = it.getArgument<Boolean>("includeDeprecated").getOrElse(false)

                val fieldDefinition = it.parentObject.cast<GQLFieldDefinition>()
                fieldDefinition.arguments.filter {
                    includeDeprecated || it.directives.findDeprecationReason() == null
                }
            },
            "type" to Resolver {
                val fieldDefinition = it.parentObject.cast<GQLFieldDefinition>()
                IntrospectionType(fieldDefinition.type, schema)
            },
            "isDeprecated" to Resolver {
                val fieldDefinition = it.parentObject.cast<GQLFieldDefinition>()
                fieldDefinition.directives.findDeprecationReason() != null
            },
            "deprecationReason" to Resolver {
                val fieldDefinition = it.parentObject.cast<GQLFieldDefinition>()
                fieldDefinition.directives.findDeprecationReason()
            }
        ),
        "__InputValue" to mapOf(
            "name" to Resolver {
                val inputValueDefinition = it.parentObject.cast<GQLInputValueDefinition>()
                inputValueDefinition.name
            },
            "description" to Resolver {
                val inputValueDefinition = it.parentObject.cast<GQLInputValueDefinition>()
                inputValueDefinition.description
            },
            "type" to Resolver {
                val inputValueDefinition = it.parentObject.cast<GQLInputValueDefinition>()
                IntrospectionType(inputValueDefinition.type, schema)
            },
            "defaultValue" to Resolver {
                val inputValueDefinition = it.parentObject.cast<GQLInputValueDefinition>()
                inputValueDefinition.defaultValue?.toUtf8()
            },
            "isDeprecated" to Resolver {
                val inputValueDefinition = it.parentObject.cast<GQLInputValueDefinition>()
                inputValueDefinition.directives.findDeprecationReason() != null
            },
            "deprecationReason" to Resolver {
                val inputValueDefinition = it.parentObject.cast<GQLInputValueDefinition>()
                inputValueDefinition.directives.findDeprecationReason()
            },
        ),
        "__EnumValue" to mapOf(
            "name" to Resolver {
                it.parentObject.cast<GQLEnumValueDefinition>().name
            },
            "description" to Resolver {
                it.parentObject.cast<GQLEnumValueDefinition>().description
            },
            "isDeprecated" to Resolver {
                it.parentObject.cast<GQLEnumValueDefinition>().directives.findDeprecationReason() != null
            },
            "deprecationReason" to Resolver {
                it.parentObject.cast<GQLEnumValueDefinition>().directives.findDeprecationReason()
            },
        ),
        "__Directive" to mapOf(
            "name" to Resolver {
                it.parentObject.cast<GQLDirectiveDefinition>().name
            },
            "description" to Resolver {
                it.parentObject.cast<GQLDirectiveDefinition>().description
            },
            "isRepeatable" to Resolver {
                it.parentObject.cast<GQLDirectiveDefinition>().repeatable
            },
            "locations" to Resolver {
                it.parentObject.cast<GQLDirectiveDefinition>().locations.map {
                    it.name
                }
            },
            "args" to Resolver {
                val includeDeprecated = it.getArgument<Boolean>("includeDeprecated").getOrElse(false)

                it.parentObject.cast<GQLDirectiveDefinition>().arguments.filter {
                    includeDeprecated || it.directives.findDeprecationReason() == null
                }
            }
        )
    )

    fun typename(obj: Any): String? {
        return typenames[obj::class]
    }

    fun resolver(typename: String, field: String): Resolver? {
        return resolvers.get(typename)?.get(field)
    }
}


private fun IntrospectionType(type: GQLType, schema: Schema): IntrospectionType {
    return IntrospectionType(
        type,
        (type as? GQLNamedType)?.name?.let { schema.typeDefinition(it) }
    )
}

private class IntrospectionType(
    val type: GQLType,
    val typeDefinition: GQLTypeDefinition?
)

internal enum class __TypeKind {
    SCALAR,
    OBJECT,
    INTERFACE,
    UNION,
    ENUM,
    INPUT_OBJECT,
    LIST,
    NON_NULL,
}
