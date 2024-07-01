package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.annotations.ApolloExperimental
import kotlin.reflect.KClass

/**
 * A boolean expression
 *
 * @param T the type of the variable elements. This allows representing BooleanExpression that only contain variables and
 * other that may also contain possibleTypes
 */
@ApolloExperimental
sealed class BooleanExpression<out T : Any> {
  /**
   * This is not super well defined but works well enough for our simple use cases
   */
  abstract fun simplify(): BooleanExpression<T>

  @ApolloExperimental
  object True : BooleanExpression<Nothing>() {
    override fun simplify() = this
  }

  @ApolloExperimental
  object False : BooleanExpression<Nothing>() {
    override fun simplify() = this
  }

  @ApolloExperimental
  data class Not<out T : Any>(val operand: BooleanExpression<T>) : BooleanExpression<T>() {
    override fun simplify() = when (this.operand) {
      is True -> False
      is False -> True
      else -> this
    }
  }

  @ApolloExperimental
  data class Or<T : Any>(val operands: Set<BooleanExpression<T>>) : BooleanExpression<T>() {
    constructor(vararg operands: BooleanExpression<T>) : this(operands.toSet())

    init {
      check(operands.isNotEmpty()) {
        "Apollo: cannot create a 'Or' condition from an empty list"
      }
    }

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

  @ApolloExperimental
  data class And<T : Any>(val operands: Set<BooleanExpression<T>>) : BooleanExpression<T>() {
    constructor(vararg operands: BooleanExpression<T>) : this(operands.toSet())

    init {
      check(operands.isNotEmpty()) {
        "Apollo: cannot create a 'And' condition from an empty list"
      }
    }

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

  @ApolloExperimental
  data class Element<out T : Any>(
      val value: T,
  ) : BooleanExpression<T>() {
    override fun simplify() = this
  }
}

internal fun <T : Any> BooleanExpression<T>.or(vararg other: BooleanExpression<T>): BooleanExpression<T> = BooleanExpression.Or((other.toList() + this).toSet())
internal fun <T : Any> BooleanExpression<T>.and(vararg other: BooleanExpression<T>): BooleanExpression<T> = BooleanExpression.And((other.toList() + this).toSet())
internal fun <T : Any> not(other: BooleanExpression<T>): BooleanExpression<T> = BooleanExpression.Not(other)

/**
 * A generic term in a [BooleanExpression]
 */
@ApolloExperimental
sealed class BTerm

/**
 * A term that comes from @include/@skip or @defer directives and that needs to be matched against operation variables
 */
@ApolloExperimental
data class BVariable(val name: String) : BTerm()

/**
 * A term that comes from @defer directives and that needs to be matched against label and current JSON path
 */
@ApolloExperimental
data class BLabel(val label: String?) : BTerm()

/**
 * A term that comes from a fragment type condition and that needs to be matched against __typename
 */
@ApolloExperimental
data class BPossibleTypes(val possibleTypes: Set<String>) : BTerm() {
  constructor(vararg types: String) : this(types.toSet())
}


internal fun <T : Any> BooleanExpression<T>.containsPossibleTypes(): Boolean {
  return when (this) {
    BooleanExpression.True -> false
    BooleanExpression.False -> false
    is BooleanExpression.Not -> operand.containsPossibleTypes()
    is BooleanExpression.Or -> operands.any { it.containsPossibleTypes() }
    is BooleanExpression.And -> operands.any { it.containsPossibleTypes() }
    is BooleanExpression.Element -> value is BPossibleTypes
  }
}

internal fun <T : Any, U : Any> BooleanExpression<T>.firstElementOfType(type: KClass<U>): U? {
  return when (this) {
    BooleanExpression.True -> null
    BooleanExpression.False -> null
    is BooleanExpression.Element -> @Suppress("UNCHECKED_CAST") if (type.isInstance(this.value)) this.value as U else null
    is BooleanExpression.Not -> this.operand.firstElementOfType(type)
    is BooleanExpression.And -> (this.operands.firstOrNull { it.firstElementOfType(type) != null })?.firstElementOfType(type)
    is BooleanExpression.Or -> (this.operands.firstOrNull { it.firstElementOfType(type) != null })?.firstElementOfType(type)
  }
}
