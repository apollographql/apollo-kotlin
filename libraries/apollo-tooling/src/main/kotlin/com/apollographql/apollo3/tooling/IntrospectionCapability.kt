package com.apollographql.apollo3.tooling

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.tooling.IntrospectionCapability.DirectiveArgsIncludeDeprecated
import com.apollographql.apollo3.tooling.IntrospectionCapability.DirectiveIsRepeatable
import com.apollographql.apollo3.tooling.IntrospectionCapability.FieldArgsIncludeDeprecated
import com.apollographql.apollo3.tooling.IntrospectionCapability.InputValueDeprecatedReason
import com.apollographql.apollo3.tooling.IntrospectionCapability.InputValueIsDeprecated
import com.apollographql.apollo3.tooling.IntrospectionCapability.SchemaDescription
import com.apollographql.apollo3.tooling.IntrospectionCapability.TypeInputFieldsIncludeDeprecated
import com.apollographql.apollo3.tooling.IntrospectionCapability.TypeIsOneOf
import com.apollographql.apollo3.tooling.IntrospectionCapability.TypeSpecifiedByURL
import com.apollographql.apollo3.tooling.graphql.PreIntrospectionQuery

@ApolloExperimental
enum class IntrospectionCapability {
  /**
   * `__Schema.description`, introduced in [October 2021](https://spec.graphql.org/October2021/)
   */
  SchemaDescription,

  /**
   * `__Type.specifiedByURL`, introduced in [October 2021](https://spec.graphql.org/October2021/)
   */
  TypeSpecifiedByURL,

  /**
   * `__Directive.isRepeatable`, introduced in [October 2021](https://spec.graphql.org/October2021/)
   */
  DirectiveIsRepeatable,

  /**
   * `__Type.inputFields`, `includeDeprecated` argument, introduced in [Draft](https://spec.graphql.org/draft/)
   */
  TypeInputFieldsIncludeDeprecated,

  /**
   * `__Directive.args`, `includeDeprecated` argument, introduced in [Draft](https://spec.graphql.org/draft/)
   */
  DirectiveArgsIncludeDeprecated,

  /**
   * `__Field.args`, `includeDeprecated` argument, introduced in [Draft](https://spec.graphql.org/draft/)
   */
  FieldArgsIncludeDeprecated,

  /**
   * `__InputValue.isDeprecated`, introduced in [Draft](https://spec.graphql.org/draft/)
   */
  InputValueIsDeprecated,

  /**
   * `__InputValue.deprecationReason`, introduced in [Draft](https://spec.graphql.org/draft/)
   */
  InputValueDeprecatedReason,

  /**
   * `__Type.isOneOf`, introduced in [OneOf Input Objects RFC](https://github.com/graphql/graphql-spec/pull/825/)
   */
  TypeIsOneOf
}

internal fun PreIntrospectionQuery.Data.getCapabilities(): Set<IntrospectionCapability> {
  return buildSet {
    if (schema?.typeFields?.fields.orEmpty().any { it.name == "description" }) {
      add(SchemaDescription)
    }

    if (type?.typeFields?.fields.orEmpty().any { it.name == "specifiedByURL" }) {
      add(TypeSpecifiedByURL)
    }
    if (type?.typeFields?.fields.orEmpty().any { it.name == "isOneOf" }) {
      add(TypeIsOneOf)
    }
    type?.typeFields?.fields.orEmpty().firstOrNull { it.name == "inputFields" }?.let { inputFields ->
      if (inputFields.args.any { it.name == "includeDeprecated" }) {
        add(TypeInputFieldsIncludeDeprecated)
      }
    }

    if (directive?.typeFields?.fields.orEmpty().any { it.name == "isRepeatable" }) {
      add(DirectiveIsRepeatable)
    }
    directive?.typeFields?.fields.orEmpty().firstOrNull { it.name == "args" }?.let { args ->
      if (args.args.any { it.name == "includeDeprecated" }) {
        add(DirectiveArgsIncludeDeprecated)
      }
    }

    field?.typeFields?.fields.orEmpty().firstOrNull { it.name == "args" }?.let { args ->
      if (args.args.any { it.name == "includeDeprecated" }) {
        add(FieldArgsIncludeDeprecated)
      }
    }

    if (inputValue?.typeFields?.fields.orEmpty().any { it.name == "isDeprecated" }) {
      add(InputValueIsDeprecated)
    }
    if (inputValue?.typeFields?.fields.orEmpty().any { it.name == "deprecationReason" }) {
      add(InputValueDeprecatedReason)
    }
  }
}
