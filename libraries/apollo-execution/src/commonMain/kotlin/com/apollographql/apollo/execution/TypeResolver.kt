package com.apollographql.apollo.execution

/**
 * A [TypeResolver] resolves concrete object types for abstract types.
 */
fun interface TypeResolver {
  /**
   * Returns the GraphQL object typename of the given Kotlin instance.
   *
   * This is used for polymorphic types to return the correct __typename depending on the runtime type of `obj`.
   *
   * Example:
   * ```
   * when (it) {
   *   is Product -> "Product"
   * }
   * ```
   *
   * @throws Exception if [obj] cannot be resolved. A GraphQL error is raised for that field.
   */
  fun resolveType(obj: ResolverValue, resolveTypeInfo: ResolveTypeInfo): String
}

internal object ThrowingTypeResolver: TypeResolver {
  override fun resolveType(obj: ResolverValue, resolveTypeInfo: ResolveTypeInfo): String {
    error("Cannot resolve type for obj '$obj' of abstract type '${resolveTypeInfo.type}': no TypeResolver found.")
  }
}