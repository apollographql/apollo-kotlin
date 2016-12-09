package com.apollostack.compiler.ir

data class InlineFragment(
    val typeCondition: String,
    val fields: List<Field>
) {

  fun interfaceName() = "$INTERFACE_PREFIX${typeCondition.capitalize()}"

  companion object {
    private val INTERFACE_PREFIX = "As"
  }
}