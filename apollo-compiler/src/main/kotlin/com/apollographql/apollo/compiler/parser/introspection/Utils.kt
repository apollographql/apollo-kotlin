package com.apollographql.apollo.compiler.parser.introspection


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
} ?: throw UnkownTypeException("Unknown type `$graphqlType`")

internal fun IntrospectionSchema.resolveType(typeRef: IntrospectionSchema.TypeRef): IntrospectionSchema.Type {
  return this[typeRef.name] ?: throw UnkownTypeException("Unknown type `${typeRef}`")
}
