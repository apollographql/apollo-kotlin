package com.apollographql.apollo.ast

import com.apollographql.apollo.annotations.ApolloDeprecatedSince

fun GQLOperationDefinition.rootTypeDefinition(schema: Schema) = when (operationType) {
  "query" -> schema.queryTypeDefinition
  "mutation" -> schema.mutationTypeDefinition
  "subscription" -> schema.subscriptionTypeDefinition
  else -> null
}

@Deprecated("Use GQLDocument.validate() instead")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
fun GQLOperationDefinition.validate(
    schema: Schema,
    fragments: Map<String, GQLFragmentDefinition>,
): List<Issue> {
  return GQLDocument(fragments.values + this, null).validateAsExecutable(schema).issues
}