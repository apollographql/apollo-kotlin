package com.apollographql.apollo.api

import java.util.Collections.unmodifiableList
import java.util.Collections.unmodifiableMap

/**
 * Field is an abstraction for a field in a graphQL operation. Field can refer to: **GraphQL Scalar Types, Objects or
 * List**. For a complete list of types that a Field object can refer to see [ResponseField.Type] class.
 */
open class ResponseField internal constructor(
    private val type: Type,
    private val responseName: String,
    private val fieldName: String,
    arguments: Map<String, Any>?,
    private val optional: Boolean,
    conditions: List<Condition>?
) {

  private val arguments: Map<String, Any> = arguments?.let { unmodifiableMap(it) }.orEmpty()
  private val conditions: List<Condition> = conditions?.let { unmodifiableList(it) }.orEmpty()

  fun type(): Type {
    return type
  }

  fun responseName(): String {
    return responseName
  }

  fun fieldName(): String {
    return fieldName
  }

  fun arguments(): Map<String, Any> {
    return arguments
  }

  fun optional(): Boolean {
    return optional
  }

  fun conditions(): List<Condition> {
    return conditions
  }

  /**
   * Resolve field argument value by name. If argument represents a references to the variable, it will be resolved from
   * provided operation variables values.
   *
   * @param name      argument name
   * @param variables values of operation variables
   * @return resolved argument value
   */
  fun resolveArgument(
      name: String,
      variables: Operation.Variables
  ): Any? {
    val variableValues = variables.valueMap()
    val argumentValue = arguments[name]
    return if (argumentValue is Map<*, *>) {
      @Suppress("UNCHECKED_CAST")
      val argumentValueMap = argumentValue as Map<String, Any>
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

  /**
   * An abstraction for the field types
   */
  enum class Type {
    STRING,
    INT,
    LONG,
    DOUBLE,
    BOOLEAN,
    ENUM,
    OBJECT,
    LIST,
    CUSTOM,
    FRAGMENT,
    INLINE_FRAGMENT
  }

  /**
   * Abstraction for a Field representing a custom GraphQL scalar type.
   */
  class CustomTypeField internal constructor(
      responseName: String,
      fieldName: String,
      arguments: Map<String, Any>?,
      optional: Boolean,
      private val scalarType: ScalarType,
      conditions: List<Condition>?
  ) : ResponseField(Type.CUSTOM, responseName, fieldName, arguments, optional, conditions) {

    fun scalarType(): ScalarType = scalarType
  }

  /**
   * Abstraction for condition to be associated with field
   */
  open class Condition internal constructor() {
    companion object {

      /**
       * Creates new [TypeNameCondition]
       */
      fun typeCondition(type: String): TypeNameCondition {
        return TypeNameCondition(type)
      }

      /**
       * Creates new [BooleanCondition]
       */
      fun booleanCondition(variableName: String, inverted: Boolean): BooleanCondition {
        return BooleanCondition(variableName, inverted)
      }
    }
  }

  /**
   * Abstraction for type name condition
   */
  class TypeNameCondition internal constructor(private val typeName: String) : Condition() {

    fun typeName(): String = typeName
  }

  /**
   * Abstraction for boolean condition
   */
  class BooleanCondition internal constructor(private val variableName: String, private val inverted: Boolean) : Condition() {

    fun variableName(): String = variableName
    fun inverted(): Boolean = inverted
  }

  companion object {

    const val VARIABLE_IDENTIFIER_KEY = "kind"
    const val VARIABLE_IDENTIFIER_VALUE = "Variable"
    const val VARIABLE_NAME_KEY = "variableName"

    /**
     * Factory method for creating a Field instance representing [Type.STRING].
     *
     * @param responseName alias for the result of a field
     * @param fieldName    name of the field in the GraphQL operation
     * @param arguments    arguments to be passed along with the field
     * @param optional     whether the arguments passed along are optional or required
     * @param conditions   list of conditions for this field
     * @return Field instance representing [Type.STRING]
     */
    @JvmStatic
    fun forString(responseName: String, fieldName: String, arguments: Map<String, Any>?,
                  optional: Boolean, conditions: List<Condition>?): ResponseField {
      return ResponseField(Type.STRING, responseName, fieldName, arguments, optional, conditions)
    }

    /**
     * Factory method for creating a Field instance representing [Type.INT].
     *
     * @param responseName alias for the result of a field
     * @param fieldName    name of the field in the GraphQL operation
     * @param arguments    arguments to be passed along with the field
     * @param optional     whether the arguments passed along are optional or required
     * @param conditions   list of conditions for this field
     * @return Field instance representing [Type.INT]
     */
    @JvmStatic
    fun forInt(responseName: String, fieldName: String, arguments: Map<String, Any>?,
               optional: Boolean, conditions: List<Condition>?): ResponseField {
      return ResponseField(Type.INT, responseName, fieldName, arguments, optional, conditions)
    }

    /**
     * Factory method for creating a Field instance representing [Type.LONG].
     *
     * @param responseName alias for the result of a field
     * @param fieldName    name of the field in the GraphQL operation
     * @param arguments    arguments to be passed along with the field
     * @param optional     whether the arguments passed along are optional or required
     * @param conditions   list of conditions for this field
     * @return Field instance representing [Type.LONG]
     */
    @JvmStatic
    fun forLong(responseName: String, fieldName: String, arguments: Map<String, Any>?,
                optional: Boolean, conditions: List<Condition>?): ResponseField {
      return ResponseField(Type.LONG, responseName, fieldName, arguments, optional, conditions)
    }

    /**
     * Factory method for creating a Field instance representing [Type.DOUBLE].
     *
     * @param responseName alias for the result of a field
     * @param fieldName    name of the field in the GraphQL operation
     * @param arguments    arguments to be passed along with the field
     * @param optional     whether the arguments passed along are optional or required
     * @param conditions   list of conditions for this field
     * @return Field instance representing [Type.DOUBLE]
     */
    @JvmStatic
    fun forDouble(responseName: String, fieldName: String, arguments: Map<String, Any>?,
                  optional: Boolean, conditions: List<Condition>?): ResponseField {
      return ResponseField(Type.DOUBLE, responseName, fieldName, arguments, optional, conditions)
    }

    /**
     * Factory method for creating a Field instance representing [Type.BOOLEAN].
     *
     * @param responseName alias for the result of a field
     * @param fieldName    name of the field in the GraphQL operation
     * @param arguments    arguments to be passed along with the field
     * @param optional     whether the arguments passed along are optional or required
     * @param conditions   list of conditions for this field
     * @return Field instance representing [Type.BOOLEAN]
     */
    @JvmStatic
    fun forBoolean(responseName: String, fieldName: String, arguments: Map<String, Any>?,
                   optional: Boolean, conditions: List<Condition>?): ResponseField {
      return ResponseField(Type.BOOLEAN, responseName, fieldName, arguments, optional, conditions)
    }

    /**
     * Factory method for creating a Field instance representing [Type.ENUM].
     *
     * @param responseName alias for the result of a field
     * @param fieldName    name of the field in the GraphQL operation
     * @param arguments    arguments to be passed along with the field
     * @param optional     whether the arguments passed along are optional or required
     * @param conditions   list of conditions for this field
     * @return Field instance representing [Type.ENUM]
     */
    @JvmStatic
    fun forEnum(responseName: String, fieldName: String, arguments: Map<String, Any>?,
                optional: Boolean, conditions: List<Condition>?): ResponseField {
      return ResponseField(Type.ENUM, responseName, fieldName, arguments, optional, conditions)
    }

    /**
     * Factory method for creating a Field instance representing a custom [Type.OBJECT].
     *
     * @param responseName alias for the result of a field
     * @param fieldName    name of the field in the GraphQL operation
     * @param arguments    arguments to be passed along with the field
     * @param optional     whether the arguments passed along are optional or required
     * @param conditions   list of conditions for this field
     * @return Field instance representing custom [Type.OBJECT]
     */
    @JvmStatic
    fun forObject(responseName: String, fieldName: String, arguments: Map<String, Any>?,
                  optional: Boolean, conditions: List<Condition>?): ResponseField {
      return ResponseField(Type.OBJECT, responseName, fieldName, arguments, optional, conditions)
    }

    /**
     * Factory method for creating a Field instance representing [Type.LIST].
     *
     * @param responseName alias for the result of a field
     * @param fieldName    name of the field in the GraphQL operation
     * @param arguments    arguments to be passed along with the field
     * @param optional     whether the arguments passed along are optional or required
     * @param conditions   list of conditions for this field
     * @return Field instance representing [Type.LIST]
     */
    @JvmStatic
    fun forList(responseName: String, fieldName: String, arguments: Map<String, Any>?,
                optional: Boolean, conditions: List<Condition>?): ResponseField {
      return ResponseField(Type.LIST, responseName, fieldName, arguments, optional, conditions)
    }

    /**
     * Factory method for creating a Field instance representing a custom GraphQL Scalar type, [Type.CUSTOM]
     *
     * @param responseName alias for the result of a field
     * @param fieldName    name of the field in the GraphQL operation
     * @param arguments    arguments to be passed along with the field
     * @param optional     whether the arguments passed along are optional or required
     * @param scalarType   the custom scalar type of the field
     * @param conditions   list of conditions for this field
     * @return Field instance representing [Type.CUSTOM]
     */
    @JvmStatic
    fun forCustomType(responseName: String, fieldName: String, arguments: Map<String, Any>?,
                      optional: Boolean, scalarType: ScalarType, conditions: List<Condition>?): CustomTypeField {
      return CustomTypeField(responseName, fieldName, arguments, optional, scalarType, conditions)
    }

    /**
     * Factory method for creating a Field instance representing [Type.FRAGMENT].
     *
     * @param responseName   alias for the result of a field
     * @param fieldName      name of the field in the GraphQL operation
     * @param typeConditions conditional GraphQL types
     * @return Field instance representing [Type.FRAGMENT]
     */
    @JvmStatic
    fun forFragment(responseName: String, fieldName: String, typeConditions: List<String>): ResponseField {
      return ResponseField(Type.FRAGMENT, responseName, fieldName, emptyMap(),
          false, typeConditions.map { Condition.typeCondition(it) })
    }

    /**
     * Factory method for creating a Field instance representing [Type.INLINE_FRAGMENT].
     *
     * @param responseName   alias for the result of a field
     * @param fieldName      name of the field in the GraphQL operation
     * @param typeConditions conditional GraphQL types
     * @return Field instance representing [Type.INLINE_FRAGMENT]
     */
    @JvmStatic
    fun forInlineFragment(responseName: String, fieldName: String, typeConditions: List<String>): ResponseField {
      return ResponseField(Type.INLINE_FRAGMENT, responseName, fieldName, emptyMap(),
          false, typeConditions.map { Condition.typeCondition(it) })
    }

    @JvmStatic
    fun isArgumentValueVariableType(objectMap: Map<String, Any>): Boolean {
      return (objectMap.containsKey(VARIABLE_IDENTIFIER_KEY)
          && objectMap[VARIABLE_IDENTIFIER_KEY] == VARIABLE_IDENTIFIER_VALUE
          && objectMap.containsKey(VARIABLE_NAME_KEY))
    }
  }
}
