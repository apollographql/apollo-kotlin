package com.apollographql.apollo.compiler.introspection


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

internal fun IntrospectionSchema.Type.possibleTypes(schema: IntrospectionSchema): Set<String> {
  return when (this) {
    is IntrospectionSchema.Type.Union -> (possibleTypes ?: emptyList()).flatMap { typeRef ->
      val typeName = typeRef.rawType.name!!
      val schemaType = checkNotNull(schema[typeName]) {
        "Unknown possible type `$typeName` for UNION `$name`"
      }
      schemaType.possibleTypes(schema)
    }.toSet()

    is IntrospectionSchema.Type.Interface -> (possibleTypes ?: emptyList()).flatMap { typeRef ->
      val typeName = typeRef.rawType.name!!
      val schemaType = checkNotNull(schema[typeName]) {
        "Unknown possible type `$typeName` for INTERFACE `$name`"
      }
      schemaType.possibleTypes(schema)
    }.toSet()

    else -> setOf(name)
  }
}
