package com.example

import com.apollographql.apollo.api.EnumType

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

    public fun safeValueOf(rawValue: String): MyEnum = values().find { it.rawValue == rawValue } ?: UNKNOWN__

    /**
     * Returns all [MyEnum] known at compile time
     */
    public fun knownValues(): Array<MyEnum> = arrayOf(
        VALUE_A,
        VALUE_B)
  }
}

fun main() {
  val myEnum: MyEnum = MyEnum.VALUE_A
}
