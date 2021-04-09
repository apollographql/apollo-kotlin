package com.apollographql.apollo3.api

import kotlin.jvm.JvmStatic

/**
 * An abstraction for a field in a graphQL operation. Field can refer to:
 * - GraphQL
 * - Scalar Types,
 * - Objects
 * - List,
 * - etc.
 *
 * For a complete list of types that a Field object can refer to see [ResponseField.Type] class.
 */
class ResponseField(
    val type: Type,
    val fieldName: String,
    val responseName: String = fieldName,
    val arguments: Map<String, Any?> = emptyMap(),
    val conditions: List<Condition> = emptyList(),
    val fieldSets: List<FieldSet> = emptyList(),
) {

  class FieldSet(val typeCondition: String?, val responseFields: Array<ResponseField>)
  /**
   * Resolves field argument value by [name]. If argument represents a references to the variable, it will be resolved from
   * provided operation [variables] values.
   */
  @Suppress("UNCHECKED_CAST")
  fun resolveArgument(
      name: String,
      variables: Operation.Variables
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
  /**
   * Abstraction for condition to be associated with field
   */
  open class Condition internal constructor() {
    companion object {
      /**
       * Creates new [BooleanCondition] for provided [variableName].
       */
      @JvmStatic
      fun booleanCondition(variableName: String, inverted: Boolean): BooleanCondition {
        return BooleanCondition(variableName, inverted)
      }
    }
  }

  /**
   * Abstraction for boolean condition
   */
  data class BooleanCondition internal constructor(
      val variableName: String,
      val isInverted: Boolean
  ) : Condition()

  companion object {
    /**
     * A pre-computed [ResponseField] to be used from generated code as an optimization
     * It shouldn't be used directly
     */
    val Typename = ResponseField(
        type = Type.NotNull(Type.Named.Other("String")),
        responseName = "__typename",
        fieldName = "__typename",
        arguments = emptyMap(),
        conditions = emptyList(),
        fieldSets = emptyList(),
    )
  }
}

fun ResponseField.Type.notNull() = ResponseField.Type.NotNull(this)
fun ResponseField.Type.list() = ResponseField.Type.List(this)
