package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.compiler.codegen.ResolverClassName
import com.apollographql.apollo3.compiler.codegen.ResolverInfo
import com.apollographql.apollo3.compiler.codegen.ResolverKeyKind
import kotlinx.serialization.Serializable

@Serializable
@ApolloInternal
class CodegenMetadata internal constructor(
    /**
     * resolver info used by the codegen to lookup already existing ClassNames
     */
    internal val resolverInfo: ResolverInfo,
)

@ApolloInternal
fun CodegenMetadata.resolveSchemaType(name: String): ResolverClassName? {
  return resolverInfo.entries.firstOrNull { it.key.kind == ResolverKeyKind.SchemaType && it.key.id == name }?.className
}
