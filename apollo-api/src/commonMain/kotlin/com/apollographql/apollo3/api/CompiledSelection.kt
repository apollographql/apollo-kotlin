package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.internal.json.Utils
import okio.Buffer

sealed class CompiledSelection

/**
 * @param arguments can be
 * - String, Int, Double, Boolean
 * - null
 * - Map<String, Any?>
 * - List<Any?>
 * - [Variable]
 *
 * Note: for now, enums are mapped to Strings
 */
class CompiledField(
    val name: String,
    val alias: String? = null,
    val type: CompiledType,
    val condition: List<CompiledCondition> = emptyList(),
    val arguments: Map<String, Any?> = emptyMap(),
    val selections: List<CompiledSelection> = emptyList(),
) : CompiledSelection() {
  val responseName: String
    get() = alias ?: name

  /**
   * Resolves field argument value by [name]. If the argument contains variables, resolve them
   */
  @Suppress("UNCHECKED_CAST")
  fun resolveArgument(
      name: String,
      variables: Executable.Variables,
  ): Any? {
    return resolveVariables(arguments[name], variables)
  }

  fun nameWithArguments(variables: Executable.Variables): String {
    if (arguments.isEmpty()) {
      return name
    }
    val resolvedArguments = resolveVariables(arguments, variables)
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

  /**
   * Resolve all variables
   */
  @Suppress("UNCHECKED_CAST")
  private fun resolveVariables(value: Any?, variables: Executable.Variables): Any? {
    return when (value) {
      null -> null
      is Variable -> {
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

class CompiledFragment(
    val possibleTypes: List<String>,
    val condition: List<CompiledCondition> = emptyList(),
    val selections: List<CompiledSelection> = emptyList(),
) : CompiledSelection()


data class CompiledCondition(val name: String, val inverted: Boolean)

sealed class CompiledType

class CompiledNotNullType(val ofType: CompiledType) : CompiledType()
class CompiledListType(val ofType: CompiledType) : CompiledType()

/**
 * a named GraphQL type
 *
 * We make the distinction between objects and non-objects ones for the CacheKeyResolver API.
 * In a typical server scenario, the resolvers would have access to the schema and would look up the complete type
 * but we want to stay lightweight so for now we add this information
 */
sealed class CompiledNamedType(val name: String) : CompiledType()

/**
 * A GraphQL union, interface or object
 */
class CompiledCompoundType(name: String) : CompiledNamedType(name)

/**
 * Not compound: scalar or enum
 */
class CompiledOtherType(name: String) : CompiledNamedType(name)

fun CompiledType.notNull() = CompiledNotNullType(this)
fun CompiledType.list() = CompiledListType(this)

