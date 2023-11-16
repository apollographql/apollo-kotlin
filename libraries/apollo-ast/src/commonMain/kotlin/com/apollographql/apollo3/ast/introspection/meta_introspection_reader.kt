package com.apollographql.apollo3.ast.introspection

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.ast.introspection.IntrospectionCapability.DirectiveArgsIncludeDeprecated
import com.apollographql.apollo3.ast.introspection.IntrospectionCapability.DirectiveIsRepeatable
import com.apollographql.apollo3.ast.introspection.IntrospectionCapability.FieldArgsIncludeDeprecated
import com.apollographql.apollo3.ast.introspection.IntrospectionCapability.InputValueDeprecatedReason
import com.apollographql.apollo3.ast.introspection.IntrospectionCapability.InputValueIsDeprecated
import com.apollographql.apollo3.ast.introspection.IntrospectionCapability.SchemaDescription
import com.apollographql.apollo3.ast.introspection.IntrospectionCapability.TypeInputFieldsIncludeDeprecated
import com.apollographql.apollo3.ast.introspection.IntrospectionCapability.TypeIsOneOf
import com.apollographql.apollo3.ast.introspection.IntrospectionCapability.TypeSpecifiedByURL
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@ApolloInternal
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
   * `__Type.inputFields`, `includeDeprecated` argument
   */
  TypeInputFieldsIncludeDeprecated,

  /**
   * `__Directive.args`, `includeDeprecated` argument
   */
  DirectiveArgsIncludeDeprecated,

  /**
   * `__Field.args`, `includeDeprecated` argument
   */
  FieldArgsIncludeDeprecated,

  /**
   * `__InputValue.isDeprecated`
   */
  InputValueIsDeprecated,

  /**
   * `__InputValue.deprecationReason`
   */
  InputValueDeprecatedReason,

  /**
   * `__Type.isOneOf`, introduced in [OneOf Input Objects RFC](https://github.com/graphql/graphql-spec/pull/825/)
   */
  TypeIsOneOf
}

@ApolloInternal
fun String.toIntrospectionCapabilities(): Set<IntrospectionCapability> {
  val introspection = this.toMIntrospection()
  return buildSet {
    val types = introspection.data.__schema.types
    types.firstOrNull { it.name == "__Schema" }?.let { schema ->
      if (schema.fields.orEmpty().any { it.name == "description" }) {
        add(SchemaDescription)
      }
    }
    types.firstOrNull { it.name == "__Type" }?.let { type ->
      if (type.fields.orEmpty().any { it.name == "specifiedByURL" }) {
        add(TypeSpecifiedByURL)
      }
      if (type.fields.orEmpty().any { it.name == "isOneOf" }) {
        add(TypeIsOneOf)
      }
      type.fields.orEmpty().firstOrNull{ it.name == "inputFields" }?.let { inputFields ->
        if (inputFields.args.any { it.name == "includeDeprecated" }) {
          add(TypeInputFieldsIncludeDeprecated)
        }
      }
    }
    types.firstOrNull { it.name == "__Directive" }?.let { directive ->
      if (directive.fields.orEmpty().any { it.name == "isRepeatable" }) {
        add(DirectiveIsRepeatable)
      }
      directive.fields.orEmpty().firstOrNull{ it.name == "args" }?.let { args ->
        if (args.args.any { it.name == "includeDeprecated" }) {
          add(DirectiveArgsIncludeDeprecated)
        }
      }
    }
    types.firstOrNull { it.name == "__Field" }?.let { field ->
      field.fields.orEmpty().firstOrNull{ it.name == "args" }?.let { args ->
        if (args.args.any { it.name == "includeDeprecated" }) {
          add(FieldArgsIncludeDeprecated)
        }
      }
    }
    types.firstOrNull { it.name == "__InputValue" }?.let { inputValue ->
      if (inputValue.fields.orEmpty().any { it.name == "isDeprecated" }) {
        add(InputValueIsDeprecated)
      }
      if (inputValue.fields.orEmpty().any { it.name == "deprecationReason" }) {
        add(InputValueDeprecatedReason)
      }
    }
  }
}

private val json = Json {
  // be robust to errors: [] keys
  ignoreUnknownKeys = true
}

private fun String.toMIntrospection(): MIntrospection{
  return json.decodeFromString(this)
}

@Serializable
private class MIntrospection(
    val data: MData
)

@Serializable
private class MData(
    @Suppress("PropertyName")
    val __schema: MSchema
)

@Serializable
private class MSchema(
    val types: List<MType>,
)

@Serializable
private class MType(
    val name: String,
    val fields: List<MField>?,
)

@Serializable
private class MField(
    val name: String,
    val args: List<MInputValue>,
)

@Serializable
private class MInputValue(
    val name: String,
)
