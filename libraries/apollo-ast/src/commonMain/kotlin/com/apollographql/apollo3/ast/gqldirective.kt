package com.apollographql.apollo3.ast

import com.apollographql.apollo3.annotations.ApolloInternal

fun List<GQLDirective>.findDeprecationReason() = firstOrNull { it.name == "deprecated" }
    ?.let {
      it.arguments
          .firstOrNull { it.name == "reason" }
          ?.value
          ?.let { value ->
            if (value !is GQLStringValue) {
              throw ConversionException("reason must be a string", it.sourceLocation)
            }
            value.value
          }
          ?: "No longer supported"
    }

fun List<GQLDirective>.findSpecifiedBy() = firstOrNull { it.name == "specifiedBy" }
    ?.let { directive ->
      directive.arguments
          .firstOrNull { it.name == "url" }
          ?.value
          ?.let { value ->
            if (value !is GQLStringValue) {
              throw ConversionException("url must be a string", directive.sourceLocation)
            }
            value.value
          }
    }

@ApolloInternal
fun List<GQLDirective>.findOptInFeature(schema: Schema): String? = filter { schema.originalDirectiveName(it.name) == Schema.REQUIRES_OPT_IN }
    .map {
      it.arguments
          .firstOrNull { it.name == "feature" }
          ?.value
          ?.let { value ->
            if (value !is GQLStringValue) {
              throw ConversionException("feature must be a string", it.sourceLocation)
            }
            value.value
          }
          ?: "ExperimentalAPI"
    }.apply {
      if (size > 1) {
        error("Multiple @requiresOptIn directives are not supported at the moment")
      }
    }.firstOrNull()

@ApolloInternal
fun List<GQLDirective>.findTargetName(schema: Schema): String? = firstOrNull { schema.originalDirectiveName(it.name) == "targetName" }
    ?.let {
      it.arguments
          .firstOrNull { it.name == "name" }
          ?.value
          ?.let { value ->
            if (value !is GQLStringValue) {
              throw ConversionException("name must be a string", it.sourceLocation)
            }
            value.value
          }
    }

@ApolloInternal
fun List<GQLDirective>.optionalValue(schema: Schema?): Boolean? {
  val directive = firstOrNull { (schema?.originalDirectiveName(it.name) ?: it.name) == Schema.OPTIONAL } ?: return null
  val argument = directive.arguments.firstOrNull { it.name == "if" }
  // "if" argument defaults to true
  return argument == null || argument.name == "if" && (argument.value as GQLBooleanValue).value
}

@ApolloInternal
fun List<GQLDirective>.findCatchLevels(schema: Schema): List<Int?> {
  return filter {
    schema.originalDirectiveName(it.name) == Schema.CATCH
  }.map {
    val argument = it.arguments.firstOrNull { it.name == "level" }
    if (argument == null) {
       null
    } else {
      (argument.value as? GQLIntValue)?.value ?: error("bad @catch directive")
    }
  }
}

@ApolloInternal
fun List<GQLDirective>.findNullOnlyOnErrorLevels(schema: Schema): List<Int?> {
  return filter {
    schema.originalDirectiveName(it.name) == Schema.NULL_ONLY_ON_ERROR
  }.map {
    val argument = it.arguments.firstOrNull { it.name == "level" }
    if (argument == null) {
      null
    } else {
      (argument.value as? GQLIntValue)?.value ?: error("bad @nullOnlyOnError directive")
    }
  }
}


@ApolloInternal
fun List<GQLDirective>.findNonnull(schema: Schema) = any { schema.originalDirectiveName(it.name) == Schema.NONNULL }
