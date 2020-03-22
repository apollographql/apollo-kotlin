package com.apollographql.apollo.compiler.parser

import com.apollographql.apollo.compiler.ir.SourceLocation
import org.antlr.v4.runtime.Token

internal fun Schema.Type.possibleTypes(schema: Schema): Set<String> {
  return when (this) {
    is Schema.Type.Union -> (possibleTypes ?: emptyList()).flatMap { typeRef ->
      val typeName = typeRef.rawType.name!!
      val schemaType = schema[typeName] ?: throw GraphQLParseException(
          message = "Unknown possible type `$typeName` for UNION `$name`"
      )
      schemaType.possibleTypes(schema)
    }.toSet()

    is Schema.Type.Interface -> (possibleTypes ?: emptyList()).flatMap { typeRef ->
      val typeName = typeRef.rawType.name!!
      val schemaType = schema[typeName] ?: throw GraphQLParseException(
          message = "Unknown possible type `$typeName` for INTERFACE `$name`"
      )
      schemaType.possibleTypes(schema)
    }.toSet()

    else -> setOf(name)
  }
}

internal fun Schema.Type.isAssignableFrom(other: Schema.Type, schema: Schema): Boolean {
  return Schema.TypeRef(kind = kind, name = name)
      .isAssignableFrom(
          other = Schema.TypeRef(kind = other.kind, name = other.name),
          schema = schema
      )
}

internal operator fun SourceLocation.Companion.invoke(token: Token) = SourceLocation(
    line = token.line,
    position = token.charPositionInLine
)

internal fun Schema.TypeRef.asGraphQLType(): String {
  return when (kind) {
    Schema.Kind.NON_NULL -> "${ofType!!.asGraphQLType()}!"
    Schema.Kind.LIST -> "[${ofType!!.asGraphQLType()}]"
    else -> name!!
  }
}

internal fun Schema.TypeRef.isAssignableFrom(other: Schema.TypeRef, schema: Schema): Boolean {
  return when (kind) {
    Schema.Kind.NON_NULL -> {
      other.kind == Schema.Kind.NON_NULL && ofType!!.isAssignableFrom(other = other.ofType!!, schema = schema)
    }

    Schema.Kind.LIST -> {
      if (other.kind == Schema.Kind.NON_NULL) {
        isAssignableFrom(other = other.ofType!!, schema = schema)
      } else {
        other.kind == Schema.Kind.LIST && ofType!!.isAssignableFrom(other = other.ofType!!, schema = schema)
      }
    }

    else -> {
      if (other.kind == Schema.Kind.NON_NULL) {
        isAssignableFrom(other = other.ofType!!, schema = schema)
      } else {
        val possibleTypes = schema.resolveType(this).possibleTypes(schema)
        val otherPossibleTypes = schema.resolveType(other).possibleTypes(schema)
        possibleTypes.intersect(otherPossibleTypes).isNotEmpty()
      }
    }
  }
}

internal fun Schema.resolveType(graphqlType: String): Schema.TypeRef = when {
  graphqlType.startsWith("[") && graphqlType.endsWith("]") -> Schema.TypeRef(
      kind = Schema.Kind.LIST,
      ofType = resolveType(graphqlType.removeSurrounding(prefix = "[", suffix = "]"))
  )

  graphqlType.endsWith("!") -> Schema.TypeRef(
      kind = Schema.Kind.NON_NULL,
      ofType = resolveType(graphqlType.removeSuffix("!"))
  )

  else -> this[graphqlType]?.let {
    Schema.TypeRef(
        kind = it.kind,
        name = it.name
    )
  }
} ?: throw GraphQLParseException("Unknown type `$graphqlType`")

internal fun Schema.resolveType(typeRef: Schema.TypeRef): Schema.Type {
  return this[typeRef.name] ?: throw GraphQLParseException("Unknown type `${typeRef.name}`")
}
