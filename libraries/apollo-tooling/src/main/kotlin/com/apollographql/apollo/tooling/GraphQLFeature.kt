package com.apollographql.apollo.tooling

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.tooling.GraphQLFeature.DeprecatedInputValues
import com.apollographql.apollo.tooling.GraphQLFeature.OneOf
import com.apollographql.apollo.tooling.GraphQLFeature.RepeatableDirectives
import com.apollographql.apollo.tooling.GraphQLFeature.SchemaDescription
import com.apollographql.apollo.tooling.GraphQLFeature.SpecifiedBy
import com.apollographql.apollo.tooling.graphql.PreIntrospectionQuery

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
  val schema = __schema.types.firstOrNull { it.typeFields.name == "__Schema" }
  val type = __schema.types.firstOrNull { it.typeFields.name == "__Type" }
  val directive = __schema.types.firstOrNull { it.typeFields.name == "__Directive" }
  val field = __schema.types.firstOrNull { it.typeFields.name == "__Field" }
  val inputValue = __schema.types.firstOrNull { it.typeFields.name == "__InputValue" }
  return buildSet {
    if (schema?.typeFields?.fields.orEmpty().any { it.name == "description" }) {
      add(SchemaDescription)
    }

    val typeFields = type?.typeFields?.fields.orEmpty()
    if (typeFields.any { it.name == "specifiedByURL" }) {
      add(SpecifiedBy)
    }
    if (typeFields.any { it.name == "isOneOf" }) {
      add(OneOf)
    }
    val typeInputFieldsArgsIncludeDeprecated: Boolean = typeFields.firstOrNull { it.name == "inputFields" }?.let { inputFields ->
      inputFields.args.any { it.name == "includeDeprecated" }
    } == true

    val directiveFields = directive?.typeFields?.fields.orEmpty()
    if (directiveFields.any { it.name == "isRepeatable" }) {
      add(RepeatableDirectives)
    }
    val directiveArgsIncludeDeprecated = directiveFields.firstOrNull { it.name == "args" }?.let { args ->
      args.args.any { it.name == "includeDeprecated" }
    } == true

    val fieldArgsIncludeDeprecated = field?.typeFields?.fields.orEmpty().firstOrNull { it.name == "args" }?.let { args ->
      args.args.any { it.name == "includeDeprecated" }
    } == true

    val inputValueFields = inputValue?.typeFields?.fields.orEmpty()
    val inputValueIsDeprecated = inputValueFields.any { it.name == "isDeprecated" }
    val inputValueDeprecatedReason = inputValueFields.any { it.name == "deprecationReason" }

    if (typeInputFieldsArgsIncludeDeprecated && directiveArgsIncludeDeprecated && fieldArgsIncludeDeprecated && inputValueIsDeprecated && inputValueDeprecatedReason) {
      add(DeprecatedInputValues)
    }
  }
}
