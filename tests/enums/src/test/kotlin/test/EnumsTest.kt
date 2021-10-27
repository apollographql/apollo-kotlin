package test

import enums.type.Direction
import enums.type.Gravity
import org.junit.Test
import kotlin.test.assertEquals

class EnumsTest {
  @Test
  fun kotlinEnums() {
    assertEquals(Direction.NORTH, Direction.safeValueOf("NORTH"))
    assertEquals(Direction.north, Direction.safeValueOf("north"))
    assertEquals(Direction.UNKNOWN__, Direction.safeValueOf("newDirection"))
  }

  @Test
  fun kotlinSealedClasses() {
    assertEquals(Gravity.TOP, Gravity.safeValueOf("TOP"))
    assertEquals(Gravity.top2, Gravity.safeValueOf("top2"))
    assertEquals(Gravity.UNKNOWN__("newGravity"), Gravity.safeValueOf("newGravity"))
  }

  @Test
  fun sealedClassesKnownValues() {
    // Order is important
    // knownValues() should return the same order that the values are declared in the schema
    assertEquals(
        arrayOf(
            Gravity.TOP,
            Gravity.top2,
            Gravity.BOTTOM,
            Gravity.LEFT,
            Gravity.RIGHT,
        ),
        Gravity.knownValues()
    )
  }
}