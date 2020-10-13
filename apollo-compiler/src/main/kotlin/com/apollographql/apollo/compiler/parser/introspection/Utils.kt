package com.apollographql.apollo.compiler.parser.introspection

import com.apollographql.apollo.compiler.parser.error.ParseException

internal fun IntrospectionSchema.Type.possibleTypes(schema: IntrospectionSchema): Set<String> {
  return when (this) {
    is IntrospectionSchema.Type.Union -> (possibleTypes ?: emptyList()).flatMap { typeRef ->
      val typeName = typeRef.rawType.name!!
      val schemaType = schema[typeName] ?: throw ParseException(
          message = "Unknown possible type `$typeName` for UNION `$name`"
      )
      schemaType.possibleTypes(schema)
    }.toSet()

    is IntrospectionSchema.Type.Interface -> (possibleTypes ?: emptyList()).flatMap { typeRef ->
      val typeName = typeRef.rawType.name!!
      val schemaType = schema[typeName] ?: throw ParseException(
          message = "Unknown possible type `$typeName` for INTERFACE `$name`"
      )
      schemaType.possibleTypes(schema)
    }.toSet()

    else -> setOf(name)
  }
}

internal fun IntrospectionSchema.Type.isAssignableFrom(other: IntrospectionSchema.Type, schema: IntrospectionSchema): Boolean {
  return IntrospectionSchema.TypeRef(kind = kind, name = name)
      .isAssignableFrom(
          other = IntrospectionSchema.TypeRef(kind = other.kind, name = other.name),
          schema = schema
      )
}

internal fun IntrospectionSchema.TypeRef.asGraphQLType(): String {
  return when (kind) {
    IntrospectionSchema.Kind.NON_NULL -> "${ofType!!.asGraphQLType()}!"
    IntrospectionSchema.Kind.LIST -> "[${ofType!!.asGraphQLType()}]"
    else -> name!!
  }
}

internal fun IntrospectionSchema.TypeRef.isAssignableFrom(other: IntrospectionSchema.TypeRef, schema: IntrospectionSchema): Boolean {
  return when (kind) {
    IntrospectionSchema.Kind.NON_NULL -> {
      other.kind == IntrospectionSchema.Kind.NON_NULL && ofType!!.isAssignableFrom(other = other.ofType!!, schema = schema)
    }

    IntrospectionSchema.Kind.LIST -> {
      if (other.kind == IntrospectionSchema.Kind.NON_NULL) {
        isAssignableFrom(other = other.ofType!!, schema = schema)
      } else {
        other.kind == IntrospectionSchema.Kind.LIST && ofType!!.isAssignableFrom(other = other.ofType!!, schema = schema)
      }
    }

    else -> {
      if (other.kind == IntrospectionSchema.Kind.NON_NULL) {
        isAssignableFrom(other = other.ofType!!, schema = schema)
      } else {
        val possibleTypes = schema.resolveType(this).possibleTypes(schema)
        val otherPossibleTypes = schema.resolveType(other).possibleTypes(schema)
        possibleTypes.intersect(otherPossibleTypes).isNotEmpty()
      }
    }
  }
}

internal fun IntrospectionSchema.resolveType(graphqlType: String): IntrospectionSchema.TypeRef = when {
  graphqlType.startsWith("[") && graphqlType.endsWith("]") -> IntrospectionSchema.TypeRef(
      kind = IntrospectionSchema.Kind.LIST,
      ofType = resolveType(graphqlType.removeSurrounding(prefix = "[", suffix = "]"))
  )

  graphqlType.endsWith("!") -> IntrospectionSchema.TypeRef(
      kind = IntrospectionSchema.Kind.NON_NULL,
      ofType = resolveType(graphqlType.removeSuffix("!"))
  )

  else -> this[graphqlType]?.let {
    IntrospectionSchema.TypeRef(
        kind = it.kind,
        name = it.name
    )
  }
} ?: throw ParseException("Unknown type `$graphqlType`")

internal fun IntrospectionSchema.resolveType(typeRef: IntrospectionSchema.TypeRef): IntrospectionSchema.Type {
  return this[typeRef.name] ?: throw ParseException("Unknown type `${typeRef.name}`")
}

internal fun IntrospectionSchema.rootTypeForOperationType(operationType: String): String? {
  return when (operationType) {
    "query" -> queryType
    "mutation" -> mutationType
    "subscription" -> subscriptionType
    else -> null
  }
}
