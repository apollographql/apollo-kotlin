package com.apollographql.apollo3.ast

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloInternal

fun List<GQLDirective>.findDeprecationReason() = firstOrNull { it.name == "deprecated" }
    ?.let {
      it.arguments
          ?.arguments
          ?.firstOrNull { it.name == "reason" }
          ?.value
          ?.let { value ->
            if (value !is GQLStringValue) {
              throw ConversionException("reason must be a string", it.sourceLocation)
            }
            value.value
          }
          ?: "No longer supported"
    }

fun List<GQLDirective>.findExperimentalReason() = firstOrNull { it.name == "experimental" }
    ?.let {
      it.arguments
          ?.arguments
          ?.firstOrNull { it.name == "reason" }
          ?.value
          ?.let { value ->
            if (value !is GQLStringValue) {
              throw ConversionException("reason must be a string", it.sourceLocation)
            }
            value.value
          }
          ?: "Experimental"
    }

@ApolloInternal
fun List<GQLDirective>.findTargetName() = firstOrNull { it.name == "targetName" }
    ?.let {
      it.arguments
          ?.arguments
          ?.firstOrNull { it.name == "name" }
          ?.value
          ?.let { value ->
            if (value !is GQLStringValue) {
              throw ConversionException("name must be a string", it.sourceLocation)
            }
            value.value
          }
    }


/**
 * @return `true` or `false` based on the `if` argument if the `optional` directive is present, `null` otherwise
 */
@Deprecated("This method is for use in Apollo Kotlin only, please file an issue if you need it")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_3_1)
fun List<GQLDirective>.optionalValue(): Boolean? = optionalValue(null)

@ApolloInternal
fun List<GQLDirective>.optionalValue(schema: Schema?): Boolean? {
  val directive = firstOrNull { (schema?.originalDirectiveName(it.name) ?: it.name) == Schema.OPTIONAL } ?: return null
  val argument = directive.arguments?.arguments?.firstOrNull { it.name == "if" }
  // "if" argument defaults to true
  return argument == null || argument.name == "if" && (argument.value as GQLBooleanValue).value
}

@Deprecated("This function doesn't work with foreign definitions. " +
    "More generally, it wasn't meant to be used outside Apollo Kotlin." +
    "Please file an issue if you need something like this.")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_3_1)
fun List<GQLDirective>.findNonnull() = any { it.name == Schema.NONNULL }

fun List<GQLDirective>.findNonnull(schema: Schema) = any { schema.originalDirectiveName(it.name) == Schema.NONNULL }

@Deprecated("This function is misleading as it only enumarates the stripable directive. Use apolloDefinition() instead.")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_3_1)
fun GQLDirective.isApollo() = name in listOf("optional", Schema.NONNULL)
