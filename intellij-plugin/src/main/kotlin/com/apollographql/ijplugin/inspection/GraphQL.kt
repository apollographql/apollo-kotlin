package com.apollographql.ijplugin.inspection

import com.intellij.lang.jsgraphql.psi.GraphQLFieldDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLIdentifier
import com.intellij.lang.jsgraphql.psi.GraphQLInterfaceTypeDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLNamedTypeDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLObjectTypeDefinition
import com.intellij.psi.util.parentOfType

/**
 * Given a field identifier, returns the set of field coordinates that match it.
 * A field coordinate is a string of the form "TypeName.fieldName".
 * Interfaces are taken into account which is why multiple coordinates can be returned.
 *
 * For example, given the following schema:
 * ```graphql
 * interface Node {
 *   id: ID!
 * }
 *
 * interface HasId {
 *   id: ID!
 * }
 *
 * type User implements Node & HasId {
 *   id: ID!
 *   name: String!
 * }
 * ```
 *
 * And the following query:
 * ```graphql
 * query {
 *   user {
 *     id
 *     name
 *   }
 * }
 * ```
 *
 * The following coordinates will be returned for `id`:
 * - `User.id`
 * - `Node.id`
 * - `HasId.id`
 */
fun matchingFieldCoordinates(fieldIdentifier: GraphQLIdentifier): Set<String> {
  val fieldDefinition = fieldIdentifier.reference?.resolve()?.parent as? GraphQLFieldDefinition ?: return emptySet()
  val namedTypeDefinition = fieldDefinition.parentOfType<GraphQLNamedTypeDefinition>() ?: return emptySet()
  return matchingFieldCoordinates(fieldDefinition, namedTypeDefinition)
}

private fun matchingFieldCoordinates(
    fieldDefinition: GraphQLFieldDefinition,
    namedTypeDefinition: GraphQLNamedTypeDefinition,
): Set<String> {
  val typeName = namedTypeDefinition.typeNameDefinition?.name ?: return emptySet()

  val fieldsDefinitions = (namedTypeDefinition as? GraphQLObjectTypeDefinition)?.fieldsDefinition
      ?: (namedTypeDefinition as? GraphQLInterfaceTypeDefinition)?.fieldsDefinition ?: return emptySet()
  if (fieldsDefinitions.fieldDefinitionList.none { it.name == fieldDefinition.name }) return emptySet()

  val fieldCoordinates = mutableSetOf(typeName + "." + fieldDefinition.name)
  val implementedInterfaces = (namedTypeDefinition as? GraphQLObjectTypeDefinition)?.implementsInterfaces
      ?: (namedTypeDefinition as? GraphQLInterfaceTypeDefinition)?.implementsInterfaces ?: return fieldCoordinates
  val implementedInterfaceTypeDefinitions = implementedInterfaces.typeNameList.mapNotNull { it.nameIdentifier.reference?.resolve()?.parentOfType<GraphQLInterfaceTypeDefinition>() }
  if (implementedInterfaceTypeDefinitions.isEmpty()) return fieldCoordinates
  return fieldCoordinates + implementedInterfaceTypeDefinitions.flatMap { matchingFieldCoordinates(fieldDefinition, it) }
}
