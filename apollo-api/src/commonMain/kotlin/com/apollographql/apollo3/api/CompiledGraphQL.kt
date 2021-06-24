package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.CompiledArgument.Companion.resolveVariables
import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.internal.json.Utils
import okio.Buffer
import kotlin.native.concurrent.SharedImmutable


sealed class CompiledSelection

/**
 * A compiled field from a GraphQL operation
 */
class CompiledField(
    val name: String,
    val alias: String? = null,
    val type: CompiledType,
    val condition: List<CompiledCondition> = emptyList(),
    val arguments: List<CompiledArgument> = emptyList(),
    val selections: List<CompiledSelection> = emptyList(),
) : CompiledSelection() {
  val responseName: String
    get() = alias ?: name

  /**
   * Resolves field argument value by [name]. If the argument contains variables, replace them with their actual value
   */
  fun resolveArgument(
      name: String,
      variables: Executable.Variables,
  ): Any? {
    return resolveVariables(arguments.firstOrNull { it.name == name }?.value, variables)
  }

  /**
   * Returns a String containing the name of this field as well as encoded arguments. For an example:
   * `hero({"episode": "Jedi"})`
   * This is mostly used internally to compute records
   */
  fun nameWithArguments(variables: Executable.Variables): String {
    if (arguments.isEmpty()) {
      return name
    }
    val map = arguments.associateBy { it.name }.mapValues { it.value.value }
    val resolvedArguments = resolveVariables(map, variables)
    return try {
      val buffer = Buffer()
      val jsonWriter = BufferedSinkJsonWriter(buffer)
      Utils.writeToJson(resolvedArguments, jsonWriter)
      jsonWriter.close()
      "${name}(${buffer.readUtf8()})"
    } catch (e: Exception) {
      throw RuntimeException(e)
    }
  }
}

/**
 * A compiled inline fragment or fragment spread
 */
class CompiledFragment(
    val possibleTypes: List<String>,
    val condition: List<CompiledCondition> = emptyList(),
    val selections: List<CompiledSelection> = emptyList(),
) : CompiledSelection()


data class CompiledCondition(val name: String, val inverted: Boolean)

sealed class CompiledType

class CompiledNotNullType(val ofType: CompiledType) : CompiledType()
class CompiledListType(val ofType: CompiledType) : CompiledType()

sealed class CompiledNamedType(val name: String) : CompiledType()

class CustomScalarType(
    /**
     * GraphQL schema custom scalar type name (e.g. `ID`, `URL`, `DateTime` etc.)
     */
    name: String,

    /**
     * Fully qualified class name this GraphQL scalar type is mapped to (e.g. `java.lang.String`, `java.net.URL`, `java.util.DateTime`)
     */
    val className: String,
) : CompiledNamedType(name)

class ObjectType(
    name: String,
    val keyFields: List<String> = emptyList(),
    val implements: List<InterfaceType> = emptyList(),
) : CompiledNamedType(name)

class InterfaceType(
    name: String,
    val keyFields: List<String> = emptyList(),
    val implements: List<InterfaceType> = emptyList(),
) : CompiledNamedType(name)

class UnionType(
    name: String,
    vararg val members: ObjectType,
) : CompiledNamedType(name)

class InputObjectType(
    name: String,
) : CompiledNamedType(name)

class EnumType(
    name: String,
) : CompiledNamedType(name)

class ScalarType(
    name: String,
) : CompiledNamedType(name)


fun CompiledType.notNull() = CompiledNotNullType(this)
fun CompiledType.list() = CompiledListType(this)

/**
 * The Kotlin representation of a GraphQL variable value
 */
class CompiledVariable(val name: String)


/**
 * The Kotlin representation of a GraphQL argument
 *
 * value can be
 * - String, Int, Double, Boolean
 * - null
 * - Map<String, Any?>
 * - List<Any?>
 * - [CompiledVariable]
 *
 * Note: for now, enums are mapped to Strings
 */
class CompiledArgument(val name: String, val value: Any?, val isKey: Boolean = false) {
  companion object {
    /**
     * Resolve all variables that may be contained inside `value`
     */
    @Suppress("UNCHECKED_CAST")
    fun resolveVariables(value: Any?, variables: Executable.Variables): Any? {
      return when (value) {
        null -> null
        is CompiledVariable -> {
          variables.valueMap[value.name]
        }
        is Map<*, *> -> {
          value as Map<String, Any?>
          value.mapValues {
            resolveVariables(it.value, variables)
          }.toList()
              .sortedBy { it.first }
              .toMap()
        }
        is List<*> -> {
          value.map {
            resolveVariables(it, variables)
          }
        }
        else -> value
      }
    }
  }
}

fun CompiledType.leafType(): CompiledNamedType {
  return when (this) {
    is CompiledNotNullType -> ofType.leafType()
    is CompiledListType -> ofType.leafType()
    is CompiledNamedType -> this
  }
}

@SharedImmutable
val CompiledStringType = ScalarType("String")
@SharedImmutable
val CompiledIntType = ScalarType("Int")
@SharedImmutable
val CompiledFloatType = ScalarType("Float")
@SharedImmutable
val CompiledBooleanType = ScalarType("Boolean")
@SharedImmutable
val CompiledIDType = ScalarType("ID")

fun CompiledNamedType.isComposite(): Boolean {
  return when (this) {
    is UnionType,
    is InterfaceType,
    is ObjectType,
    -> true
    else
    -> false
  }
}


fun CompiledNamedType.keyFields(): List<String> {
  return when (this) {
    is InterfaceType -> keyFields
    is ObjectType -> keyFields
    else -> emptyList()
  }
}