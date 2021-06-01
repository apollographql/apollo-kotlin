package com.apollographql.apollo3.api

/**
 * A [MergedField] represents a field potentially merged as in https://spec.graphql.org/draft/#sec-Field-Selection-Merging
 *
 * Because the merging depends on the actual type being received, a [MergedField] contains multiple possible [FieldSet] for the
 * different concrete types
 *
 */
class MergedField(
    val type: Type,
    val fieldName: String,
    val responseName: String = fieldName,
    val arguments: Map<String, Any?> = emptyMap(),
    val condition: BooleanExpression<BVariable> = BooleanExpression.True,
    val fieldSets: List<FieldSet> = emptyList(),
) {

  /**
   * Resolves field argument value by [name]. If argument represents a references to the variable, it will be resolved from
   * provided operation [variables] values.
   */
  @Suppress("UNCHECKED_CAST")
  fun resolveArgument(
      name: String,
      variables: Executable.Variables
  ): Any? {
    val variableValues = variables.valueMap
    val argumentValue = arguments[name]
    return if (argumentValue is Variable) {
      variableValues[argumentValue.name]
    } else {
      argumentValue
    }
  }

  sealed class Type {
    class NotNull(val ofType: Type): Type()
    class List(val ofType: Type): Type()

    /**
     * a Named GraphQL type
     *
     * We make the distinction between objects and non-objects ones for the CacheKeyResolver API.
     * In a typical server scenario, the resolvers would have access to the schema and would look up the complete type
     * but we want to stay lightweight so for now we add this information
     */
    sealed class Named(val name: String): Type() {
      /**
       * This is field is a Kotlin object. It can be a GraphQL union, interface or object
       */
      class Object(name: String): Named(name)
      class Other(name: String): Named(name)
    }
  }

  companion object {
    /**
     * A pre-computed [MergedField] to be used from generated code as an optimization
     * It shouldn't be used directly
     */
    val Typename = MergedField(
        type = Type.NotNull(Type.Named.Other("String")),
        responseName = "__typename",
        fieldName = "__typename",
        arguments = emptyMap(),
        condition = BooleanExpression.True,
        fieldSets = emptyList(),
    )
  }
}

fun MergedField.Type.notNull() = MergedField.Type.NotNull(this)
fun MergedField.Type.list() = MergedField.Type.List(this)
