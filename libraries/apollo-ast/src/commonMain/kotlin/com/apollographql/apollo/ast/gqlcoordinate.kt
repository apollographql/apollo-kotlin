package com.apollographql.apollo.ast

import com.apollographql.apollo.annotations.ApolloExperimental

fun resolveSchemaCoordinate(schema: Schema, coordinate: String): ResolvedSchemaElement? {
  return resolveSchemaCoordinate(schema, coordinate.parseAsGQLSchemaCoordinate().getOrThrow())
}

sealed interface ResolvedSchemaElement

class ResolvedType(val typeDefinition: GQLTypeDefinition) : ResolvedSchemaElement
class ResolvedField(val fieldDefinition: GQLFieldDefinition) : ResolvedSchemaElement
class ResolvedInputField(val inputField: GQLInputValueDefinition) : ResolvedSchemaElement
class ResolvedEnumValue(val enumValue: GQLEnumValueDefinition) : ResolvedSchemaElement
class ResolvedFieldArgument(val argument: GQLInputValueDefinition) : ResolvedSchemaElement
class ResolvedDirective(val directiveDefinition: GQLDirectiveDefinition) : ResolvedSchemaElement
class ResolvedDirectiveArgument(val argument: GQLInputValueDefinition) : ResolvedSchemaElement

/**
 * Resolves the given schema coordinate according to [schema].
 *
 * @return the [ResolvedSchemaElement] or `null` if not found.
 *
 * @throws IllegalArgumentException if any of the containing elements is not found or is of an unexpected type.
 */
@ApolloExperimental
fun resolveSchemaCoordinate(schema: Schema, coordinate: GQLSchemaCoordinate): ResolvedSchemaElement? {
  return when (coordinate) {
    is GQLArgumentCoordinate -> {
      val typeDefinition =
        schema.typeDefinitions.get(coordinate.type) ?: throw IllegalArgumentException("Unknow type '${coordinate.type}'")

      val fieldDefinition = when (typeDefinition) {
        is GQLObjectTypeDefinition, is GQLInterfaceTypeDefinition -> {
          typeDefinition.fieldDefinitions(schema).firstOrNull { it.name == coordinate.field }
              ?: throw IllegalArgumentException("Unknow field '${coordinate.field}' in type '${typeDefinition.name}'")
        }

        else -> throw IllegalArgumentException("Expected '${typeDefinition.name}' to be an object or interface type")
      }
      fieldDefinition.arguments.firstOrNull { it.name == coordinate.argument }?.let { ResolvedFieldArgument(it) }
    }

    is GQLMemberCoordinate -> {
      val typeDefinition =
        schema.typeDefinitions.get(coordinate.type) ?: throw IllegalArgumentException("Unknow type '${coordinate.type}'")

      when (typeDefinition) {
        is GQLObjectTypeDefinition, is GQLInterfaceTypeDefinition -> {
          typeDefinition.fieldDefinitions(schema).firstOrNull { it.name == coordinate.member }?.let { ResolvedField(it) }
        }

        is GQLInputObjectTypeDefinition -> {
          typeDefinition.inputFields.firstOrNull { it.name == coordinate.member }?.let { ResolvedInputField(it) }
        }

        is GQLEnumTypeDefinition -> {
          typeDefinition.enumValues.firstOrNull { it.name == coordinate.member }?.let { ResolvedEnumValue(it) }
        }

        else -> throw IllegalArgumentException("Expected '${typeDefinition.name}' to be an object, input object, interface or enum type")
      }
    }

    is GQLTypeCoordinate -> {
      schema.typeDefinitions.get(coordinate.name)?.let { ResolvedType(it) }
    }

    is GQLDirectiveArgumentCoordinate -> {
      val directive =
        schema.directiveDefinitions.get(coordinate.name) ?: throw IllegalArgumentException("Unknow directive '@${coordinate.name}'")

      directive.arguments.firstOrNull { it.name == coordinate.argument }?.let { ResolvedDirectiveArgument(it) }
    }

    is GQLDirectiveCoordinate -> {
      schema.directiveDefinitions.get(coordinate.name)?.let { ResolvedDirective(it) }
    }
  }
}
