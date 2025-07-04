package test

import enums.apollo.type.Color
import enums.apollo.type.experimental.knownOrDefault
import enums.apollo.type.experimental.knownOrNull
import kotlin.test.Test
import kotlin.test.assertEquals


class ApolloEnumTest {
  @Test
  fun knownOrDefault() {
    assertEquals(Color.BLUEBERRY, Color.safeValueOf("UNKNOWN").knownOrDefault { Color.BLUEBERRY })
    assertEquals(Color.CHERRY, Color.safeValueOf("CHERRY").knownOrDefault { Color.CHERRY })
  }

  @Test
  fun knownOrNull() {
    assertEquals(null, Color.safeValueOf("UNKNOWN").knownOrNull())
    assertEquals(Color.CHERRY, Color.safeValueOf("CHERRY").knownOrNull())
  }

  @Test
  fun knownOrCandy() {
    assertEquals(Color.CANDY, Color.safeValueOf("UNKNOWN").knownOrCandy())
  }

  /**
   * This is only used to check it compiles properly
   */
  fun doStuff(color: Color) {
    when (color.knownOrCandy()) {
      Color.BLUEBERRY -> TODO()
      Color.CANDY -> TODO()
      Color.CHERRY -> TODO()
    }
  }

  /**
   * Turns a maybe unknown color value into a known one
   */
  private fun Color.knownOrCandy(): Color.__Known = when (this) {
    is Color.__Unknown -> Color.CANDY
    // Sadly cannot use `else ->` here so we use explicit branches
    // See https://youtrack.jetbrains.com/issue/KT-18950/Smart-Cast-should-work-within-else-branch-for-sealed-subclasses
    is Color.__Known -> this
  }
}