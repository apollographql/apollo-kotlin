package com.apollographql.apollo.api

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
    val responseName: String,
    val fieldName: String,
    val arguments: Map<String, Any?>,
    val conditions: List<Condition>,
    val possibleFieldSets: Map<String, Array<ResponseField>>,
) {

  /**
   * Resolves field argument value by [name]. If argument represents a references to the variable, it will be resolved from
   * provided operation [variables] values.
   */
  @Suppress("UNCHECKED_CAST")
  fun resolveArgument(
      name: String,
      variables: Operation.Variables
  ): Any? {
    val variableValues = variables.valueMap()
    val argumentValue = arguments[name]
    return if (argumentValue is Map<*, *>) {
      val argumentValueMap = argumentValue as Map<String, Any?>
      if (isArgumentValueVariableType(argumentValueMap)) {
        val variableName = argumentValueMap[VARIABLE_NAME_KEY].toString()
        variableValues[variableName]
      } else {
        null
      }
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
      class Object(name: String): Named(name)
      class Other(name: String): Named(name)
    }
  }

  enum class Kind { OBJECT, OTHER }
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
    private const val VARIABLE_IDENTIFIER_KEY = "kind"
    private const val VARIABLE_IDENTIFIER_VALUE = "Variable"
    const val VARIABLE_NAME_KEY = "variableName"

    @JvmStatic
    fun isArgumentValueVariableType(objectMap: Map<String, Any?>): Boolean {
      return (objectMap.containsKey(VARIABLE_IDENTIFIER_KEY)
          && objectMap[VARIABLE_IDENTIFIER_KEY] == VARIABLE_IDENTIFIER_VALUE && objectMap.containsKey(VARIABLE_NAME_KEY))
    }

    fun Type.leafType(): String = when(this) {
      is Type.NotNull -> ofType.leafType()
      is Type.Named -> name
      is Type.List -> ofType.leafType()
    }

    val Typename = ResponseField(
        type = Type.NotNull(Type.Named("String", Kind.OTHER)),
        responseName = "__typename",
        fieldName = "__typename",
        arguments = emptyMap(),
        conditions = emptyList(),
        possibleFieldSets = emptyMap(),
    )
  }
}
