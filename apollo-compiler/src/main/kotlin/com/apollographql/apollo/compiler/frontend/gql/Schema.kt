package com.apollographql.apollo.compiler.frontend.gql

/**
 * a very thin wrapper around a schema GQLDocument
 *
 * It serves as a common ground between GQLDocument and IntrospectionSchema
 *
 * Schema should always contain all types, including the builtin ones
 */
class Schema(
    val typeDefinitions: Map<String, GQLTypeDefinition>,
    val queryTypeDefinition: GQLTypeDefinition,
    val mutationTypeDefinition: GQLTypeDefinition?,
    val subscriptionTypeDefinition: GQLTypeDefinition?,
) {
  fun toDocument(): GQLDocument = GQLDocument(
      definitions = typeDefinitions.values.toList() + GQLSchemaDefinition(
          description = null,
          directives = emptyList(),
          rootOperationTypeDefinitions = rootOperationTypeDefinition()
      ),
      filePath = null
  ).withoutBuiltinTypes()

  private fun rootOperationTypeDefinition(): List<GQLOperationTypeDefinition> {
    val list = mutableListOf<GQLOperationTypeDefinition>()
    list.add(
        GQLOperationTypeDefinition(
            operationType = "query",
            namedType = queryTypeDefinition.name
        )
    )
    if (mutationTypeDefinition != null) {
      list.add(
          GQLOperationTypeDefinition(
              operationType = "mutation",
              namedType = mutationTypeDefinition.name
          )
      )
    }
    if (subscriptionTypeDefinition != null) {
      list.add(
          GQLOperationTypeDefinition(
              operationType = "subscription",
              namedType = subscriptionTypeDefinition.name
          )
      )
    }

    return list
  }

  fun typeDefinition(name: String): GQLTypeDefinition {
    return typeDefinitions[name] ?: throw SchemaValidationException("Cannot find type `$name`")
  }
}