package com.apollographql.apollo.compiler.parser.introspection

import okio.BufferedSink
import okio.buffer
import okio.sink
import java.io.File

/**
 * Previous versions of this method used to encode defaultValues as Json elements. For an example:
 * "defaultValue": [0, 1]
 *
 * This is wrong. The spec mandates defaultValue to be GraphQL encoded String:
 * "defaultValue": "[0, 1]"
 *
 * For backward compatibility, use [legacyDefaultValues] = true
 */
fun IntrospectionSchema.toSDL(file: File, legacyDefaultValues: Boolean) {
  file.sink().buffer().use {
    toSDL(it, legacyDefaultValues)
  }
}

fun IntrospectionSchema.toSDL(file: File) = toSDL(file, true)

fun IntrospectionSchema.toSDL(sink: BufferedSink, legacyDefaultValues: Boolean) {
  val builtinsTypes = setOf(
      "Int", "Float", "String", "Boolean", "ID",
      "__Directive", "__DirectiveLocation", "__EnumValue", "__Field", "__InputValue", "__Schema", "__Type", "__TypeKind"
  )
  val allInterfaces = types.values.filterIsInstance<IntrospectionSchema.Type.Interface>()
  types.values.forEach {
    if (builtinsTypes.contains(it.name)) {
      // No need to include builtin types in SDL files
      return@forEach
    }
    when (it) {
      is IntrospectionSchema.Type.Scalar -> it.toSDL(sink)
      is IntrospectionSchema.Type.Enum -> it.toSDL(sink)
      is IntrospectionSchema.Type.InputObject -> it.toSDL(sink, legacyDefaultValues)
      is IntrospectionSchema.Type.Interface -> it.toSDL(sink, legacyDefaultValues)
      is IntrospectionSchema.Type.Object -> it.toSDL(sink, allInterfaces, legacyDefaultValues)
      is IntrospectionSchema.Type.Union -> it.toSDL(sink)
    }
    // Add a newline as a separation
    sink.writeUtf8("\n")
  }
  sink.writeUtf8("schema {\n")
  sink.writeUtf8("  query: $queryType\n")
  mutationType?.let { sink.writeUtf8("  mutation: $it\n") }
  subscriptionType?.let { sink.writeUtf8("  subscription: $it\n") }
  sink.writeUtf8("}\n")
}

fun IntrospectionSchema.toSDL(sink: BufferedSink) = toSDL(sink, true)

private fun BufferedSink.writeDescription(description: String?, indent: String = "") {
  if (!description.isNullOrBlank()) {
    writeUtf8(
        """${indent}${"\"\"\""}
${indent}$description
${indent}${"\"\"\""}
"""
    )
  }
}

private fun String.escape(): String {
  // TODO: proper string escape and parsing
  return replace("\"", "\\\"")
}

private fun BufferedSink.writeDeprecatedDirective(isDeprecated: Boolean, deprecationReason: String?) {
  if (isDeprecated) {
    writeUtf8(" @deprecated")
    if (deprecationReason != null) {
      writeUtf8("(reason: \"${deprecationReason.escape()}\")")
    }
  }
}

private fun IntrospectionSchema.Type.Scalar.toSDL(sink: BufferedSink) {
  sink.writeDescription(description)
  sink.writeUtf8("scalar $name\n")
}

private fun IntrospectionSchema.Type.Enum.toSDL(sink: BufferedSink) {
  sink.writeDescription(description)
  sink.writeUtf8("enum $name {\n")
  enumValues.forEach {
    it.toSDL(sink)
    sink.writeUtf8("\n")
  }
  sink.writeUtf8("}\n")
}

private fun IntrospectionSchema.Type.Enum.Value.toSDL(sink: BufferedSink) {
  sink.writeDescription(description, "  ")
  sink.writeUtf8("  $name")
  sink.writeDeprecatedDirective(isDeprecated, deprecationReason)
}

private fun IntrospectionSchema.Type.InputObject.toSDL(sink: BufferedSink, legacyDefaultValues: Boolean) {
  sink.writeDescription(description)
  sink.writeUtf8("input $name {\n")
  inputFields.forEach {
    it.toSDL(sink, legacyDefaultValues)
    sink.writeUtf8("\n")
  }
  sink.writeUtf8("}\n")
}

/**
 * Writes the Json element returned by the Json parser as a GraphQL value
 */
private fun BufferedSink.writeValue(value: Any?) {
  when (value) {
    null -> writeUtf8("null")
    is Int -> writeUtf8(value.toString()) // We get Ints coming from the SDL parsers
    is Long -> writeUtf8(value.toString()) // And Longs coming from moshi, be robust to both
    is Double -> writeUtf8(value.toString())
    is Boolean -> writeUtf8(value.toString())
    is String -> writeUtf8("\"\"\"$value\"\"\"") // enums will fall in this case as there is no way to express an enum in Json
    is List<*> -> {
      writeUtf8("[")
      value.forEachIndexed { index, item ->
        writeValue(item)
        if (index != value.size - 1) {
          writeUtf8(", ")
        }
      }
      writeUtf8("]")
    }
    is Map<*, *> -> {
      writeUtf8("[")
      value.entries.forEachIndexed { index, entry ->
        writeUtf8("${entry.key}: ")
        writeValue(entry.value)
        if (index != value.size - 1) {
          writeUtf8(", ")
        }
      }
      writeUtf8("]")
    }
    else -> throw IllegalStateException("ApolloGraphQL: Cannot write SDL value: $value")
  }
}

private fun IntrospectionSchema.InputField.toSDL(sink: BufferedSink, legacyDefaultValues: Boolean) {
  sink.writeDescription(description, "  ")
  sink.writeUtf8("  $name: ${type.asGraphQLType()}")
  if (defaultValue != null) {
    sink.writeUtf8(" = ")
    if (!legacyDefaultValues && defaultValue is String) {
      // defaultValue is already encoded as GraphQL, we can pass it verbatim
      sink.writeUtf8(defaultValue)
    } else {
      // legacy mode if we bump into an introspection schema that doesn't encode the default value
      sink.writeValue(defaultValue)
    }

  }
  sink.writeDeprecatedDirective(isDeprecated, deprecationReason)
}

private fun IntrospectionSchema.Type.Interface.toSDL(sink: BufferedSink, legacyDefaultValue: Boolean) {
  sink.writeDescription(description)
  sink.writeUtf8("interface $name {\n")
  fields?.forEach {
    it.toSDL(sink, legacyDefaultValue)
    sink.writeUtf8("\n")
  }
  sink.writeUtf8("}\n")
}

private fun IntrospectionSchema.Field.toSDL(sink: BufferedSink, legacyDefaultValue: Boolean) {
  sink.writeDescription(description, "  ")
  sink.writeUtf8("  $name")
  if (args.isNotEmpty()) {
    sink.writeUtf8("(")
    args.forEachIndexed { index, arg ->
      arg.toSDL(sink, legacyDefaultValue)
      if (index != args.size - 1) {
        sink.writeUtf8(", ")
      }
    }
    sink.writeUtf8(")")
  }
  sink.writeUtf8(": ${type.asGraphQLType()}")
  sink.writeDeprecatedDirective(isDeprecated, deprecationReason)
}

private fun IntrospectionSchema.Field.Argument.toSDL(sink: BufferedSink, legacyDefaultValue: Boolean) {
  if (!description.isNullOrBlank()) {
    // Write the description inline
    sink.writeUtf8("\"\"\"$description\"\"\" ")
  }
  sink.writeUtf8("$name: ${type.asGraphQLType()}")
  if (defaultValue != null) {
    sink.writeUtf8(" = ")
    if (!legacyDefaultValue && defaultValue is String) {
      // defaultValue is already encoded as GraphQL, we can pass it verbatim
      sink.writeUtf8(defaultValue)
    } else {
      // legacy mode if we bump into an introspection schema that doesn't encode the default value
      sink.writeValue(defaultValue)
    }
  }
  sink.writeDeprecatedDirective(isDeprecated, deprecationReason)
}

private fun IntrospectionSchema.Type.Object.toSDL(sink: BufferedSink, interfaces: List<IntrospectionSchema.Type.Interface>, legacyDefaultValue: Boolean) {
  sink.writeDescription(description, "")
  sink.writeUtf8("type $name")
  val implements = interfaces.filter {
    it.possibleTypes?.map {
      check(it.kind == IntrospectionSchema.Kind.OBJECT) {
        "possibleType ${it.name} of interface $name is of type ${it.kind} and not OBJECT"
      }
      it.name
    }?.contains(name) ?: false
  }.map {
    it.name
  }

  if (implements.isNotEmpty()) {
    sink.writeUtf8(" implements ")
    sink.writeUtf8(implements.joinToString(" & "))
  }
  sink.writeUtf8(" {\n")

  fields?.forEach {
    it.toSDL(sink, legacyDefaultValue)
    sink.writeUtf8("\n")
  }

  sink.writeUtf8("}\n")
}

private fun IntrospectionSchema.Type.Union.toSDL(sink: BufferedSink) {
  sink.writeDescription(description, "")
  sink.writeUtf8("union $name = ")
  sink.writeUtf8(possibleTypes!!.map { it.name }.joinToString(" | "))
  sink.writeUtf8("\n")
}

