package com.apollographql.ijplugin.util

import com.apollographql.ijplugin.gradle.ApolloKotlinService
import com.apollographql.ijplugin.gradle.gradleToolingModelService
import com.apollographql.ijplugin.graphql.ApolloGraphQLConfigContributor
import com.intellij.lang.jsgraphql.ide.config.GraphQLConfigProvider
import com.intellij.lang.jsgraphql.psi.GraphQLDirective
import com.intellij.lang.jsgraphql.psi.GraphQLElement
import com.intellij.lang.jsgraphql.psi.GraphQLFieldDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLFile
import com.intellij.lang.jsgraphql.psi.GraphQLIdentifier
import com.intellij.lang.jsgraphql.psi.GraphQLInterfaceTypeDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLListType
import com.intellij.lang.jsgraphql.psi.GraphQLNamedElement
import com.intellij.lang.jsgraphql.psi.GraphQLNamedTypeDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLNonNullType
import com.intellij.lang.jsgraphql.psi.GraphQLObjectTypeDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLType
import com.intellij.lang.jsgraphql.psi.GraphQLTypeName
import com.intellij.lang.jsgraphql.psi.GraphQLValue
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parentOfTypes

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
  val implementedInterfaceTypeDefinitions =
    implementedInterfaces.typeNameList.mapNotNull { it.nameIdentifier.reference?.resolve()?.parentOfType<GraphQLInterfaceTypeDefinition>() }
  if (implementedInterfaceTypeDefinitions.isEmpty()) return fieldCoordinates
  return fieldCoordinates + implementedInterfaceTypeDefinitions.flatMap { matchingFieldCoordinates(fieldDefinition, it) }
}

/**
 * Return the schema files associated with the given GraphQL element.
 */
fun GraphQLElement.schemaFiles(): List<GraphQLFile> {
  val containingFile = containingFile ?: return emptyList()
  val projectConfig = GraphQLConfigProvider.getInstance(project).resolveProjectConfig(containingFile) ?: return emptyList()
  return projectConfig.schema.mapNotNull { schema ->
    schema.filePath?.let { path -> project.findPsiFileByUrl(schema.dir.url + "/" + path) } as? GraphQLFile
  }
}

/**
 * Return the [ApolloKotlinService] associated with the given GraphQL element.
 */
fun GraphQLElement.apolloKotlinService(): ApolloKotlinService? {
  val containingFile = containingFile ?: return null
  val projectConfig = GraphQLConfigProvider.getInstance(project).resolveProjectConfig(containingFile) ?: return null
  val apolloKotlinServiceId =
    projectConfig.extensions[ApolloGraphQLConfigContributor.EXTENSION_APOLLO_KOTLIN_SERVICE_ID] as? String ?: return null
  return project.gradleToolingModelService.apolloKotlinServices.firstOrNull { it.id.toString() == apolloKotlinServiceId }
}

fun GraphQLDirective.argumentValue(argumentName: String): GraphQLValue? =
  arguments?.argumentList.orEmpty().firstOrNull { it.name == argumentName }?.value

inline fun <reified T : PsiElement> GraphQLNamedElement.resolve(): T? =
  nameIdentifier?.reference?.resolve()?.parentOfTypes(T::class)

val GraphQLType.rawType: GraphQLTypeName?
  get() {
    @Suppress("RecursivePropertyAccessor")
    return when (this) {
      is GraphQLTypeName -> return this
      is GraphQLNonNullType -> return this.type.rawType
      is GraphQLListType -> return this.type.rawType
      else -> null
    }
  }
