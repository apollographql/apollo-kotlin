@file:JvmName("BooleanExpressions")

package com.apollographql.apollo.api

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloDeprecatedSince.Version.v4_0_0
import kotlin.jvm.JvmName
import kotlin.reflect.KClass

/**
 * A boolean expression
 *
 * @param T the type of the variable elements. This allows representing BooleanExpression that only contain variables and
 * other that may also contain possibleTypes
 */
sealed class BooleanExpression<out T : Any> {
  @Deprecated(message = "This was only used in internal API and shouldn't have been part of the public API. If you needed this, please open an issue.", level = DeprecationLevel.ERROR)
  @ApolloDeprecatedSince(v4_0_0)
  abstract fun simplify(): BooleanExpression<T>

  object True : BooleanExpression<Nothing>() {
    @Deprecated(message = "This was only used in internal API and shouldn't have been part of the public API. If you needed this, please open an issue.", level = DeprecationLevel.ERROR)
    @ApolloDeprecatedSince(v4_0_0)
    override fun simplify() = this
  }

  object False : BooleanExpression<Nothing>() {
    @Deprecated(message = "This was only used in internal API and shouldn't have been part of the public API. If you needed this, please open an issue.", level = DeprecationLevel.ERROR)
    @ApolloDeprecatedSince(v4_0_0)
    override fun simplify() = this
  }

  data class Not<out T : Any>(val operand: BooleanExpression<T>) : BooleanExpression<T>() {
    @Deprecated(message = "This was only used in internal API and shouldn't have been part of the public API. If you needed this, please open an issue.", level = DeprecationLevel.ERROR)
    @ApolloDeprecatedSince(v4_0_0)
    override fun simplify() = when (this.operand) {
      is True -> False
      is False -> True
      else -> this
    }
  }

  data class Or<T : Any>(val operands: Set<BooleanExpression<T>>) : BooleanExpression<T>() {
    constructor(vararg operands: BooleanExpression<T>) : this(operands.toSet())

    init {
      check(operands.isNotEmpty()) {
        "Apollo: cannot create a 'Or' condition from an empty list"
      }
    }

    @Deprecated(message = "This was only used in internal API and shouldn't have been part of the public API. If you needed this, please open an issue.", level = DeprecationLevel.ERROR)
    @ApolloDeprecatedSince(v4_0_0)
    @Suppress("DEPRECATION_ERROR")
    override fun simplify() = operands.filter {
      it != False
    }.map { it.simplify() }
        .let {
          when {
            it.contains(True) -> True
            it.isEmpty() -> False
            it.size == 1 -> it.first()
            else -> {
              Or(it.toSet())
            }
          }
        }

    override fun toString() = operands.joinToString(" | ")
  }

  data class And<T : Any>(val operands: Set<BooleanExpression<T>>) : BooleanExpression<T>() {
    constructor(vararg operands: BooleanExpression<T>) : this(operands.toSet())

    init {
      check(operands.isNotEmpty()) {
        "Apollo: cannot create a 'And' condition from an empty list"
      }
    }

    @Deprecated(message = "This was only used in internal API and shouldn't have been part of the public API. If you needed this, please open an issue.", level = DeprecationLevel.ERROR)
    @ApolloDeprecatedSince(v4_0_0)
    @Suppress("DEPRECATION_ERROR")
    override fun simplify() = operands.filter {
      it != True
    }.map { it.simplify() }
        .let {
          when {
            it.contains(False) -> False
            it.isEmpty() -> True
            it.size == 1 -> it.first()
            else -> {
              And(it.toSet())
            }
          }
        }
  }

  data class Element<out T : Any>(
      val value: T,
  ) : BooleanExpression<T>() {
    @Deprecated(message = "This was only used in internal API and shouldn't have been part of the public API. If you needed this, please open an issue.", level = DeprecationLevel.ERROR)
    @ApolloDeprecatedSince(v4_0_0)
    @Suppress("DEPRECATION_ERROR")
    override fun simplify() = this
  }
}

@Deprecated(message = "This was only used in internal API and shouldn't have been part of the public API. If you needed this, please open an issue.", level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(v4_0_0)
fun <T : Any> BooleanExpression<T>.or(vararg other: BooleanExpression<T>): BooleanExpression<T> = BooleanExpression.Or((other.toList() + this).toSet())

@Deprecated(message = "This was only used in internal API and shouldn't have been part of the public API. If you needed this, please open an issue.", level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(v4_0_0)
fun <T : Any> BooleanExpression<T>.and(vararg other: BooleanExpression<T>): BooleanExpression<T> = BooleanExpression.And((other.toList() + this).toSet())

fun <T : Any> or(vararg other: BooleanExpression<T>): BooleanExpression<T> = BooleanExpression.Or((other.toList()).toSet())
fun <T : Any> and(vararg other: BooleanExpression<T>): BooleanExpression<T> = BooleanExpression.And((other.toList()).toSet())
fun <T : Any> not(other: BooleanExpression<T>): BooleanExpression<T> = BooleanExpression.Not(other)
fun variable(name: String): BooleanExpression<BVariable> = BooleanExpression.Element(BVariable(name))
fun label(label: String? = null): BooleanExpression<BLabel> = BooleanExpression.Element(BLabel(label))
fun possibleTypes(vararg typenames: String): BooleanExpression<BPossibleTypes> = BooleanExpression.Element(BPossibleTypes(typenames.toSet()))

@Deprecated(message = "This was only used in internal API and shouldn't have been part of the public API. If you needed this, please open an issue.", level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(v4_0_0)
@Suppress("DEPRECATION_ERROR")
fun <T : Any> BooleanExpression<T>.evaluate(block: (T) -> Boolean): Boolean {
  return when (this) {
    BooleanExpression.True -> true
    BooleanExpression.False -> false
    is BooleanExpression.Not -> !operand.evaluate(block)
    is BooleanExpression.Or -> operands.any { it.evaluate(block) }
    is BooleanExpression.And -> operands.all { it.evaluate(block) }
    is BooleanExpression.Element -> block(value)
  }
}

@Suppress("DEPRECATION_ERROR")
fun BooleanExpression<BTerm>.evaluate(
    variables: Set<String>?,
    typename: String?,
    deferredFragmentIdentifiers: Set<DeferredFragmentIdentifier>?,
    path: List<Any>?,
): Boolean {
  // Remove "data" from the path
  val croppedPath = path?.drop(1)
  return evaluate {
    when (it) {
      is BVariable -> !(variables?.contains(it.name) ?: false)
      is BLabel -> hasDeferredFragment(deferredFragmentIdentifiers, croppedPath!!, it.label)
      is BPossibleTypes -> it.possibleTypes.contains(typename)
    }
  }
}

private fun hasDeferredFragment(deferredFragmentIdentifiers: Set<DeferredFragmentIdentifier>?, path: List<Any>, label: String?): Boolean {
  if (deferredFragmentIdentifiers == null) {
    // By default, parse all deferred fragments - this is the case when parsing from the normalized cache.
    return true
  }
  return deferredFragmentIdentifiers.contains(DeferredFragmentIdentifier(path, label))
}

/**
 * A generic term in a [BooleanExpression]
 */
sealed class BTerm

/**
 * A term that comes from @include/@skip or @defer directives and that needs to be matched against operation variables
 */
data class BVariable(val name: String) : BTerm()

/**
 * A term that comes from @defer directives and that needs to be matched against label and current JSON path
 */
data class BLabel(val label: String?) : BTerm()

/**
 * A term that comes from a fragment type condition and that needs to be matched against __typename
 */
data class BPossibleTypes(val possibleTypes: Set<String>) : BTerm() {
  constructor(vararg types: String) : this(types.toSet())
}

@Deprecated(message = "This was only used in internal API and shouldn't have been part of the public API. If you needed this, please open an issue.", level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(v4_0_0)
@Suppress("DEPRECATION_ERROR")
fun <T : Any> BooleanExpression<T>.containsPossibleTypes(): Boolean {
  return when (this) {
    BooleanExpression.True -> false
    BooleanExpression.False -> false
    is BooleanExpression.Not -> operand.containsPossibleTypes()
    is BooleanExpression.Or -> operands.any { it.containsPossibleTypes() }
    is BooleanExpression.And -> operands.any { it.containsPossibleTypes() }
    is BooleanExpression.Element -> value is BPossibleTypes
  }
}

@Deprecated(message = "This was only used in internal API and shouldn't have been part of the public API. If you needed this, please open an issue.", level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(v4_0_0)
@Suppress("DEPRECATION_ERROR")
fun <T : Any, U : Any> BooleanExpression<T>.firstElementOfType(type: KClass<U>): U? {
  return when (this) {
    BooleanExpression.True -> null
    BooleanExpression.False -> null
    is BooleanExpression.Element -> @Suppress("UNCHECKED_CAST") if (type.isInstance(this.value)) this.value as U else null
    is BooleanExpression.Not -> this.operand.firstElementOfType(type)
    is BooleanExpression.And -> (this.operands.firstOrNull { it.firstElementOfType(type) != null })?.firstElementOfType(type)
    is BooleanExpression.Or -> (this.operands.firstOrNull { it.firstElementOfType(type) != null })?.firstElementOfType(type)
  }
}
