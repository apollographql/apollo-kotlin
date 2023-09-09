package com.apollographql.apollo3.ast

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import kotlin.jvm.JvmOverloads

fun GQLOperationDefinition.rootTypeDefinition(schema: Schema) = when (operationType) {
  "query" -> schema.queryTypeDefinition
  "mutation" -> schema.mutationTypeDefinition
  "subscription" -> schema.subscriptionTypeDefinition
  else -> null
}

@JvmOverloads
@Deprecated("Use GQLDocument.validate() instead")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
fun GQLOperationDefinition.validate(
    schema: Schema,
    fragments: Map<String, GQLFragmentDefinition>,
    fieldsOnDisjointTypesMustMerge: Boolean = true,
): List<Issue>{
  return GQLDocument(fragments.values + this, null).validateAsExecutable(schema, fieldsOnDisjointTypesMustMerge).issues
}