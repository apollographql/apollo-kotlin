package com.apollographql.apollo.ast

import com.apollographql.apollo.annotations.ApolloInternal

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

fun List<GQLDirective>.findOneOf() = any { it.name == Schema.ONE_OF }

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
fun List<GQLDirective>.findTargetName(schema: Schema): String? = firstOrNull { schema.originalDirectiveName(it.name) == Schema.TARGET_NAME }
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
fun List<GQLDirective>.findNonnull(schema: Schema) = any { schema.originalDirectiveName(it.name) == Schema.NONNULL }

@ApolloInternal
enum class CatchTo {
  RESULT,
  NULL,
  THROW
}

@ApolloInternal
data class Catch(val to: CatchTo, val levels: List<Int>?)

private fun GQLDirectiveDefinition.getArgumentDefaultValue(argName: String): GQLValue? {
  return arguments.firstOrNull { it.name == argName }?.defaultValue
}

@ApolloInternal
fun GQLDirective.getArgumentValueOrDefault(argName: String, schema: Schema): GQLValue? {
  val argument = arguments.firstOrNull { it.name == argName }
  if (argument == null) {
    val directiveDefinition: GQLDirectiveDefinition = schema.directiveDefinitions.get(name)!!
    return directiveDefinition.getArgumentDefaultValue(argName)
  }
  return argument.value
}

private fun GQLValue.toListOfInt(): List<Int> {
  check(this is GQLListValue) {
    error("${sourceLocation}: expected a list value")
  }
  return this.values.map {
    check(it is GQLIntValue) {
      error("${it.sourceLocation}: expected an int value")
    }
    it.value.toInt()
  }
}

private fun GQLValue.toIntOrNull(): Int? {
  return when (this) {
    is GQLNullValue -> null
    is GQLIntValue -> this.value.toIntOrNull()
    else -> error("${sourceLocation}: expected Int value")
  }
}

private fun GQLValue.toStringOrNull(): String? {
  return when (this) {
    is GQLNullValue -> null
    is GQLStringValue -> this.value
    else -> error("${sourceLocation}: expected String value")
  }
}

private fun GQLValue?.toBoolean(): Boolean {
  return when (this) {
    is GQLBooleanValue -> this.value
    else -> error("${this?.sourceLocation}: expected Boolean! value")
  }
}

private fun GQLValue?.toCatchTo(): CatchTo {
  return when (this) {
    is GQLEnumValue -> when (this.value) {
      "NULL" -> CatchTo.NULL
      "RESULT" -> CatchTo.RESULT
      "THROW" -> CatchTo.THROW
      else -> error("Unknown CatchTo value: ${this.value}")
    }

    else -> error("${this?.sourceLocation}: expected CatchTo! value")
  }
}

@ApolloInternal
fun List<GQLDirective>.findCatch(schema: Schema): Catch? {
  return filter {
    schema.originalDirectiveName(it.name) == Schema.CATCH
  }.map {
    Catch(
        to = it.getArgumentValueOrDefault("to", schema).toCatchTo(),
        levels = it.getArgumentValueOrDefault("levels", schema)!!.toListOfInt(),
    )
  }.singleOrNull()
}

private fun List<GQLDirective>.findCatchByDefault(schema: Schema): CatchTo? {
  return filter {
    schema.originalDirectiveName(it.name) == Schema.CATCH_BY_DEFAULT
  }.map {
    it.getArgumentValueOrDefault("to", schema).toCatchTo()
  }.singleOrNull()
}

@ApolloInternal
fun GQLOperationDefinition.findCatchByDefault(schema: Schema): CatchTo? {
  return directives.findCatchByDefault(schema) ?: schema.schemaDefinition?.directives?.findCatchByDefault(schema)
}

@ApolloInternal
fun GQLFragmentDefinition.findCatchByDefault(schema: Schema): CatchTo? {
  return directives.findCatchByDefault(schema) ?: schema.schemaDefinition?.directives?.findCatchByDefault(schema)
}

@ApolloInternal
fun GQLField.findCatch(schema: Schema): Catch? {
  var catch = directives.findCatch(schema)
  if (catch != null) {
    return catch
  }

  return null
}

@ApolloInternal
fun GQLFieldDefinition.findSemanticNonNulls(schema: Schema): List<Int> {
  val semanticNonNulls = directives.filter {
    schema.originalDirectiveName(it.name) == Schema.SEMANTIC_NON_NULL
  }

  val semanticNonNull = semanticNonNulls.singleOrNull()
  if (semanticNonNull == null) {
    return emptyList()
  }
  return semanticNonNull.getArgumentValueOrDefault("levels", schema)!!.toListOfInt()
}
