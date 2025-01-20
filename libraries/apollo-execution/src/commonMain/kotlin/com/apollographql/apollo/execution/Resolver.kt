package com.apollographql.apollo.execution

import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFieldDefinition
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.definitionFromScope

fun interface Resolver {
  /**
   * Resolves a field. A typical implementation uses [ResolveInfo.parentObject]:
   *
   * ```kotlin
   * fun resolve(resolveInfo: ResolveInfo): Any? {
   *   val parent = resolveInfo.parentObject as Map<String, Any?>
   *   return parent[resolveInfo.fieldName]
   * }
   * ```
   *
   * @param resolveInfo information about the field being resolved
   * @return the resolved result
   * @throws Exception if something wrong happens. A GraphQL error for that field
   * is generated.
   */
  suspend fun resolve(resolveInfo: ResolveInfo): ResolverValue
}

/**
 * A resolver that always throws
 */
internal object ThrowingResolver : Resolver {
  override suspend fun resolve(resolveInfo: ResolveInfo): ResolverValue {
    error("Cannot resolve field '${resolveInfo.parentType}.${resolveInfo.fieldName}': no Resolver found.")
  }
}

/**
 * @property type the abstract type that needs to be resolved. [type] is always the name of
 * an interface or union.
 * @property schema a schema that can be used to resolve [type].
 */
class ResolveTypeInfo(
  val type: String,
  val schema: Schema
)

/**
 * @property parentObject the object returned by the resolver.
 * @property parentType the type of the parent object. Always a concrete object type.
 * @property fields the fields being resolved. Because there may be merged fields, this is
 * a list, but they will all share the same information except for sub selections.
 * @property executionContext
 * @property arguments the coerced arguments.
 */
class ResolveInfo internal constructor(
  val parentObject: Any?,
  val parentType: String,
  val executionContext: ExecutionContext,
  val fields: List<GQLField>,
  val schema: Schema,
  private val arguments: Map<String, InternalValue>,
  val path: List<Any>
) {
  val field: GQLField
    get() = fields.first()

  val fieldName: String
    get() = fields.first().name

  fun fieldDefinition(): GQLFieldDefinition {
    return field.definitionFromScope(schema, parentType)
      ?: error("Cannot find fieldDefinition $parentType.${field.name}")
  }

  /**
   * Returns the argument for [name]. It is the caller responsibility to use a type parameter [T] matching
   * the expected argument type. If not, [getArgument] may succeed but subsequent calls may fail with [ClassCastException].
   *
   * @param T the type of the expected [InternalValue]. The caller must have knowledge of what Kotlin type
   * to expect for this argument. T
   *
   * @return the argument for [name] or [Optional.Absent] if that argument is not present. The return
   * value is automatically cast to [T].
   */
  fun <T> getArgument(
    name: String,
  ): Optional<T> {
    return if (!arguments.containsKey(name)) {
      Optional.absent()
    } else {
      @Suppress("UNCHECKED_CAST")
      Optional.present(arguments.get(name)) as Optional<T>
    }
  }

  fun <T> getRequiredArgument(name: String): T {
    return getArgument<T>(name).getOrThrow()
  }

  fun coordinates(): String {
    return "$parentType.$fieldName"
  }
}
