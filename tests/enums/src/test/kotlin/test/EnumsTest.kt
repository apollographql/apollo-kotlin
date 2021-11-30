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
    assertEquals(Direction.name_, Direction.safeValueOf("name"))
    assertEquals(Direction.ordinal_, Direction.safeValueOf("ordinal"))
  }

  @Test
  fun kotlinSealedClasses() {
    assertEquals(Gravity.TOP, Gravity.safeValueOf("TOP"))
    assertEquals(Gravity.top2, Gravity.safeValueOf("top2"))
    assertEquals(Gravity.UNKNOWN__("newGravity"), Gravity.safeValueOf("newGravity"))
    assertEquals(Gravity.name, Gravity.safeValueOf("name"))
    assertEquals(Gravity.ordinal, Gravity.safeValueOf("ordinal"))
  }

  @Test
  fun sealedClassesKnownValues() {
    // Order is important
    // knownValues() should return the same order that the values are declared in the schema
    // Convert to List because Array.equals uses referential equality, see
    // https://blog.jetbrains.com/kotlin/2015/09/feedback-request-limitations-on-data-classes/
    assertEquals(
        arrayOf(
            Gravity.TOP,
            Gravity.top2,
            Gravity.BOTTOM,
            Gravity.LEFT,
            Gravity.RIGHT,
            Gravity.name,
            Gravity.ordinal,
        ).toList(),
        Gravity.knownValues().toList()
    )
  }
}
