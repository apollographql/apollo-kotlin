package com.apollographql.apollo3.graphql.ast

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

fun List<GQLDirective>.findOptional() = any { it.name == "optional" }
fun List<GQLDirective>.findNonnull() = any { it.name == "nonnull" }

fun GQLDirective.isApollo() = name in listOf("optional", "nonnull")