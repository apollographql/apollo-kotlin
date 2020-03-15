package com.apollographql.apollo.compiler.parser

import com.apollographql.apollo.compiler.ir.SourceLocation
import org.antlr.v4.runtime.Token

internal fun Schema.Type.possibleTypes(schema: Schema): Set<String> {
  return when (this) {
    is Schema.Type.Union -> (possibleTypes ?: emptyList()).flatMap { typeRef ->
      val typeName = typeRef.rawType.name!!
      val schemaType = schema[typeName] ?: throw throw GraphQLParseException(
          message = "Unknown possible type `$typeName` for UNION `$name`"
      )
      schemaType.possibleTypes(schema)
    }.toSet()

    is Schema.Type.Interface -> (possibleTypes ?: emptyList()).flatMap { typeRef ->
      val typeName = typeRef.rawType.name!!
      val schemaType = schema[typeName] ?: throw throw GraphQLParseException(
          message = "Unknown possible type `$typeName` for INTERFACE `$name`"
      )
      schemaType.possibleTypes(schema)
    }.toSet()

    else -> setOf(name)
  }
}

internal fun Schema.Type.isAssignableFrom(schema: Schema, other: Schema.Type): Boolean {
  if (name == other.name) {
    return true
  }
  return when (this) {
    is Schema.Type.Union -> possibleTypes(schema).intersect(other.possibleTypes(schema)).isNotEmpty()
    is Schema.Type.Interface -> {
      val possibleTypes = (possibleTypes ?: emptyList()).mapNotNull { it.rawType.name }
      possibleTypes.contains(other.name) || possibleTypes.any { typeName ->
        val schemaType = schema[typeName] ?: throw throw GraphQLParseException(
            message = "Unknown possible type `$typeName` for INTERFACE `$name`"
        )
        schemaType.isAssignableFrom(schema = schema, other = other)
      }
    }
    else -> false
  }
}

internal operator fun SourceLocation.Companion.invoke(token: Token) = SourceLocation(
    line = token.line,
    position = token.charPositionInLine
)
