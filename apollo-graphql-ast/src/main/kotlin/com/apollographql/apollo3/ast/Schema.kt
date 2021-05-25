package com.apollographql.apollo3.ast

/**
 * A wrapper around a schema GQLDocument that:
 * - always contain builtin types contrary to introspection that will not contain directives and SDL that will not contain
 * any builtin definitions
 * - has type extensions merged
 * - allows for easier retrieval of type by name
 */
class Schema(
    document: GQLDocument,
) {
  /**
   * Add the builtin definitions before merging the type extensions.
   * That leaves the possibility to extend the builtin types. Not sure how useful that is
   * but that shouldn't harm
   */
  private val definitions = TypeExtensionsMergeScope().mergeDocumentTypeExtensions(document.definitions + builtinDefinitions())

  val typeDefinitions: Map<String, GQLTypeDefinition> = definitions
      .filterIsInstance<GQLTypeDefinition>()
      .associateBy { it.name }

  val directiveDefinitions: Map<String, GQLDirectiveDefinition> = definitions
      .filterIsInstance<GQLDirectiveDefinition>()
      .associateBy { it.name }

  val queryTypeDefinition: GQLTypeDefinition = document
      .rootOperationTypeDefinition("query")
      ?: throw SchemaValidationException("No query root type found")

  val mutationTypeDefinition: GQLTypeDefinition? = document
      .rootOperationTypeDefinition("mutation")

  val subscriptionTypeDefinition: GQLTypeDefinition? = document
      .rootOperationTypeDefinition("subscription")

  fun toGQLDocument(): GQLDocument = GQLDocument(
      definitions = typeDefinitions.values.toList() + directiveDefinitions.values.toList() + GQLSchemaDefinition(
          description = null,
          directives = emptyList(),
          rootOperationTypeDefinitions = rootOperationTypeDefinition()
      ),
      filePath = null
  ).withoutBuiltinDefinitions()

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
    return typeDefinitions[name]
        ?: throw SchemaValidationException("Cannot find type `$name`")
  }
}
