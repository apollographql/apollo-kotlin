package com.apollographql.apollo3.compiler.unified

/**
 * A condition.
 * It initially comes from @include/@skip directives but is extended to account for variables, type conditions and any combination
 */
sealed class BooleanExpression {
  abstract fun evaluate(variables: Set<String>, typeConditions: Set<String>): Boolean

  /**
   * This is not super well defined but works well enough for our simple use cases
   */
  abstract fun simplify(): BooleanExpression

  fun or(vararg other: BooleanExpression) = Or((other.toList() + this).toSet())
  fun and(vararg other: BooleanExpression) = And((other.toList() + this).toSet())
  fun not() = Not(this)

  object True : BooleanExpression() {
    override fun evaluate(variables: Set<String>, typeConditions: Set<String>) = true
    override fun simplify() = this
    override fun toString() = "true"
  }

  object False : BooleanExpression() {
    override fun evaluate(variables: Set<String>, typeConditions: Set<String>) = false
    override fun simplify() = this
    override fun toString() = "false"
  }

  data class Not(val booleanExpression: BooleanExpression): BooleanExpression() {
    override fun evaluate(variables: Set<String>, typeConditions: Set<String>) = !booleanExpression.evaluate(variables, typeConditions)
    override fun simplify() = when(this.booleanExpression) {
      is True -> False
      is False -> True
      else -> this
    }
    override fun toString() = "!$booleanExpression"
  }

  data class Or(val booleanExpressions: Set<BooleanExpression>) : BooleanExpression() {
    init {
      check(booleanExpressions.isNotEmpty()) {
        "ApolloGraphQL: cannot create a 'Or' condition from an empty list"
      }
    }

    override fun evaluate(variables: Set<String>, typeConditions: Set<String>) =
        booleanExpressions.firstOrNull { it.evaluate(variables, typeConditions) } != null

    override fun simplify() = booleanExpressions.filter {
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

    override fun toString() = booleanExpressions.joinToString(" | ")
  }

  data class And(val booleanExpressions: Set<BooleanExpression>) : BooleanExpression() {
    init {
      check(booleanExpressions.isNotEmpty()) {
        "ApolloGraphQL: cannot create a 'And' condition from an empty list"
      }
    }

    override fun evaluate(variables: Set<String>, typeConditions: Set<String>) =
        booleanExpressions.firstOrNull { !it.evaluate(variables, typeConditions) } == null

    override fun simplify() = booleanExpressions.filter {
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
    override fun toString() = booleanExpressions.joinToString(" & ")
  }


  data class Variable(
      val name: String,
  ) : BooleanExpression() {
    override fun evaluate(variables: Set<String>, typeConditions: Set<String>) = variables.contains(name)
    override fun simplify() = this
    override fun toString() = "Var($name)"
  }

  data class Type(
      val name: String,
  ) : BooleanExpression() {
    override fun evaluate(variables: Set<String>, typeConditions: Set<String>) = typeConditions.contains(name)

    override fun simplify() = this

    override fun toString() = "Type($name)"
  }
}