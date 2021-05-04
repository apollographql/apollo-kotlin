package com.apollographql.apollo3.graphql.ast

fun GQLOperationDefinition.rootTypeDefinition(schema: Schema) = when (operationType) {
  "query" -> schema.queryTypeDefinition
  "mutation" -> schema.mutationTypeDefinition
  "subscription" -> schema.subscriptionTypeDefinition
  else -> null
}

fun GQLOperationDefinition.validate(
    schema: Schema,
    fragments: Map<String, GQLFragmentDefinition>,
) = ExecutableValidationScope(schema, fragments).validateOperation(this)