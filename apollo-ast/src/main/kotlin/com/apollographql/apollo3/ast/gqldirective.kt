package com.apollographql.apollo3.ast

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

/**
 * @return `true` or `false` based on the `if` argument if the `optional` directive is present, `null` otherwise
 */
fun List<GQLDirective>.optionalValue(): Boolean? {
  val directive = firstOrNull { it.name == "optional" } ?: return null
  val argument = directive.arguments?.arguments?.firstOrNull { it.name == "if" }
  // "if" argument defaults to true
  return argument == null || argument.name == "if" && (argument.value as GQLBooleanValue).value
}

fun List<GQLDirective>.findNonnull() = any { it.name == "nonnull" }

fun GQLDirective.isApollo() = name in listOf("optional", "nonnull")
