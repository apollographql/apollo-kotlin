package com.apollographql.apollo.ast

import com.apollographql.apollo.annotations.ApolloExperimental

/**
 * A known foreign schema
 *
 * @param name the name of the foreign schema as in https://specs.apollo.dev/link/v1.0/#@link.url
 * @param version the version of the foreign schema as in https://specs.apollo.dev/link/v1.0/#@link.url
 * @param definitions the definitions in the foreign schema
 * @param directivesToStrip the name of directives that must be stripped before being sent to the server
 * without the leading '@'.
 * For an example: `"catch"`
 */
@ApolloExperimental
class ForeignSchema(
    val name: String,
    val version: String,
    val definitions: List<GQLDefinition>,
    val directivesToStrip: List<String> = definitions.filterIsInstance<GQLDirective>().map { it.name },
)