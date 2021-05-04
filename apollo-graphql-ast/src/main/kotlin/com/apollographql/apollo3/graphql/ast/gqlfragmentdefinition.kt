package com.apollographql.apollo3.graphql.ast



fun GQLFragmentDefinition.validate(
    schema: Schema,
    fragments: Map<String, GQLFragmentDefinition>,
) = ExecutableValidationScope(schema, fragments).validateFragment(this)

fun GQLFragmentDefinition.inferVariables(
    schema: Schema,
    fragments: Map<String, GQLFragmentDefinition>,
) = ExecutableValidationScope(schema, fragments).inferFragmentVariables(this)

