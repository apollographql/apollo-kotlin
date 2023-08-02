package com.example

import com.apollographql.apollo3.api.EnumType

public enum class myEnum(
    public val rawValue: String,
) {
  VALUE_A("VALUE_A"),
  VALUE_B("VALUE_B"),

  /**
   * Auto generated constant for unknown enum values
   */
  UNKNOWN__("UNKNOWN__"),
  ;

  public companion object {
    public val type: EnumType = EnumType("myEnum", listOf("VALUE_A", "VALUE_B"))

    public fun safeValueOf(rawValue: String): myEnum = values().find { it.rawValue == rawValue } ?: UNKNOWN__

    /**
     * Returns all [myEnum] known at compile time
     */
    public fun knownValues(): Array<myEnum> = arrayOf(
        VALUE_A,
        VALUE_B)
  }
}

fun main() {
  val myEnum: myEnum = myEnum.VALUE_A
}
