package com.apollographql.apollo3.api.test

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.CompiledListType
import com.apollographql.apollo3.api.CompiledNamedType
import com.apollographql.apollo3.api.CompiledNotNullType
import com.apollographql.apollo3.api.CompiledType
import com.apollographql.apollo3.api.CustomScalarType
import kotlin.native.concurrent.ThreadLocal

/**
 * Implement [TestResolver] to generate fake data during tests.
 */
@ApolloExperimental
interface TestResolver {
  /**
   * Resolve the given field
   *
   * @param responseName the name of the field as seen in the json
   * @param compiledType the GraphQL type of the field
   * @param ctors if [compiledType] is a composite type or any non-null or list combination of a composite type,
   * ctors contain a list of constructors for the possible shapes
   *
   * @return T the Kotlin value for the field. Can be Int, Double, String, List<Any?> or Map<String, Any?> or null
   */
  fun <T> resolve(responseName: String, compiledType: CompiledType, ctors: Array<out () -> Map<String, Any?>>?): T
}

@ApolloExperimental
open class DefaultTestResolver : TestResolver {
  private val MAX_STACK_SIZE = 256

  private val stack = Array<Any>(MAX_STACK_SIZE) { 0 }
  private var stackSize = 0

  private var intCounter = 0
  private var floatCounter = 0.5
  private var compositeCounter = 0
  private var booleanCounter = false

  open fun resolveListSize(path: List<Any>): Int {
    return 3
  }

  open fun resolveInt(path: List<Any>): Int {
    return intCounter++
  }

  open fun resolveString(path: List<Any>): String {
    return path.subList(path.indexOfLast { it is String }, path.size).joinToString("")
  }

  open fun resolveFloat(path: List<Any>): Double {
    return floatCounter++
  }

  open fun resolveBoolean(path: List<Any>): Boolean {
    return booleanCounter.also {
      booleanCounter = !booleanCounter
    }
  }

  open fun resolveComposite(path: List<Any>, ctors: Array<out () -> Map<String, Any?>>): Map<String, Any?> {
    return ctors[(compositeCounter++) % ctors.size]()
  }

  open fun resolveCustomScalar(path: List<Any>): String {
    error("Cannot resolve custom scalar at $path")
  }

  private fun push(v: Any) {
    check(stackSize < MAX_STACK_SIZE) {
      "Nesting too deep at ${stack.joinToString(".")}"
    }
    stack[stackSize++] = v
  }

  private fun pop() {
    stackSize--
    stack[stackSize] = 0 // Allow garbage collection
  }

  private fun <T> resolveInternal(responseName: String, compiledType: CompiledType, ctors: Array<out () -> Map<String, Any?>>?): T {
    val path = stack.take(stackSize).toList()
    @Suppress("UNCHECKED_CAST")
    return when (compiledType) {
      is CompiledNotNullType -> resolve(responseName, compiledType.ofType, ctors)
      is CompiledListType -> {
        0.until(resolveListSize(path)).map { i ->
          push(i)
          resolveInternal<Any>(responseName, compiledType.ofType, ctors).also {
            pop()
          }
        }
      }
      is CustomScalarType -> {
        resolveCustomScalar(path)
      }
      is CompiledNamedType -> {
        when (compiledType.name) {
          "Int" -> resolveInt(path)
          "Float" -> resolveFloat(path)
          "Boolean" -> resolveBoolean(path)
          "String" -> resolveString(path)
          "ID" -> resolveString(path)
          else -> {
            resolveComposite(path, ctors ?: error("no ctors for $responseName"))
          }
        }
      }
    } as T
  }

  override fun <T> resolve(responseName: String, compiledType: CompiledType, ctors: Array<out () -> Map<String, Any?>>?): T {
    push(responseName)
    return resolveInternal<T>(responseName, compiledType, ctors).also {
      pop()
    }
  }
}

@ThreadLocal
@ApolloExperimental
internal var currentTestResolver: TestResolver? = null

@ApolloExperimental
fun <T> withTestResolver(testResolver: TestResolver, block: () -> T): T {
  currentTestResolver = testResolver
  return block().also {
    currentTestResolver = null
  }
}
