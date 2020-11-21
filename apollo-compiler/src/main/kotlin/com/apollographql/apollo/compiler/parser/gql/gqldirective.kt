package com.apollographql.apollo.compiler.parser.gql

internal fun List<GQLDirective>.findDeprecationReason() = firstOrNull { it.name == "deprecated" }
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