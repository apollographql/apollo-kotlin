package com.apollographql.apollo3.tooling

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.tooling.GraphQLFeature.DeprecatedInputValues
import com.apollographql.apollo3.tooling.GraphQLFeature.OneOf
import com.apollographql.apollo3.tooling.GraphQLFeature.RepeatableDirectives
import com.apollographql.apollo3.tooling.GraphQLFeature.SchemaDescription
import com.apollographql.apollo3.tooling.GraphQLFeature.SpecifiedBy
import com.apollographql.apollo3.tooling.graphql.PreIntrospectionQuery

@ApolloExperimental
enum class GraphQLFeature {
  /**
   * Description on the schema, introduced in [October 2021](https://spec.graphql.org/October2021/).
   *
   * Introspection: `__Schema.description`.
   */
  SchemaDescription,

  /**
   * `specifiedByURL` for scalars, introduced in [October 2021](https://spec.graphql.org/October2021/).
   *
   * Introspection: `__Type.specifiedByURL`.
   */
  SpecifiedBy,

  /**
   * Repeatable directives, introduced in [October 2021](https://spec.graphql.org/October2021/).
   *
   * Introspection: `__Directive.isRepeatable`.
   */
  RepeatableDirectives,

  /**
   * Deprecated input values, introduced in [Deprecation of input values](https://github.com/graphql/graphql-spec/pull/805/).
   *
   * Introspection:
   * - `__InputValue.isDeprecated`
   * - `__InputValue.deprecationReason`
   * - `__Field.args`'s `includeDeprecated` argument
   * - `__Directive.args`'s `includeDeprecated` argument
   * - `__Type.inputFields`'s `includeDeprecated` argument
   */
  DeprecatedInputValues,

  /**
   * OneOf input object types, introduced in [OneOf Input Objects RFC](https://github.com/graphql/graphql-spec/pull/825/).
   *
   * Introspection: `__Type.isOneOf`.
   */
  OneOf
}

internal fun PreIntrospectionQuery.Data.getFeatures(): Set<GraphQLFeature> {
  return buildSet {
    if (schema?.typeFields?.fields.orEmpty().any { it.name == "description" }) {
      add(SchemaDescription)
    }

    if (type?.typeFields?.fields.orEmpty().any { it.name == "specifiedByURL" }) {
      add(SpecifiedBy)
    }
    if (type?.typeFields?.fields.orEmpty().any { it.name == "isOneOf" }) {
      add(OneOf)
    }
    type?.typeFields?.fields.orEmpty().firstOrNull { it.name == "inputFields" }?.let { inputFields ->
      if (inputFields.args.any { it.name == "includeDeprecated" }) {
        add(DeprecatedInputValues)
      }
    }

    if (directive?.typeFields?.fields.orEmpty().any { it.name == "isRepeatable" }) {
      add(RepeatableDirectives)
    }
  }
}
