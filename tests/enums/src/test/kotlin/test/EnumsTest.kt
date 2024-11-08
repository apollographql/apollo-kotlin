package test

import enums.kotlin15.type.Direction
import enums.kotlin15.type.Foo
import enums.kotlin15.type.FooEnum
import enums.kotlin15.type.FooSealed
import enums.kotlin15.type.Gravity
import enums.kotlin19.type.Color
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class EnumsTest {
  @Test
  fun kotlinEnums() {
    assertEquals(Direction.NORTH, Direction.safeValueOf("NORTH"))
    @Suppress("DEPRECATION")
    assertEquals(Direction.north, Direction.safeValueOf("north"))
    assertEquals(Direction.UNKNOWN__, Direction.safeValueOf("newDirection"))
    assertEquals(Direction.name_, Direction.safeValueOf("name"))
    assertEquals(Direction.ordinal_, Direction.safeValueOf("ordinal"))
    assertEquals(Direction.type__, Direction.safeValueOf("type"))
    assertEquals(Direction.Companion_, Direction.safeValueOf("Companion"))
  }

  @Test
  fun kotlin19Enums() {
    assertEquals(enums.kotlin19.type.Direction.safeValueOf("NORTH"), enums.kotlin19.type.Direction.NORTH)
  }

  @Test
  fun kotlinSealedClasses() {
    assertEquals(Gravity.TOP, Gravity.safeValueOf("TOP"))
    @Suppress("DEPRECATION")
    assertEquals(Gravity.top2, Gravity.safeValueOf("top2"))
    assertIs<Gravity.UNKNOWN__>(Gravity.safeValueOf("newGravity"))
    assertEquals(Gravity.name, Gravity.safeValueOf("name"))
    assertEquals(Gravity.ordinal, Gravity.safeValueOf("ordinal"))
    assertEquals(Gravity.type__, Gravity.safeValueOf("type"))
    assertEquals(Gravity.Companion_, Gravity.safeValueOf("Companion"))
  }

  @Test
  fun javaEnums() {
    assertEquals(enums.java.type.Direction.NORTH, enums.java.type.Direction.safeValueOf("NORTH"))
    @Suppress("DEPRECATION")
    assertEquals(enums.java.type.Direction.north, enums.java.type.Direction.safeValueOf("north"))
    assertEquals(enums.java.type.Direction.UNKNOWN__, enums.java.type.Direction.safeValueOf("newDirection"))
    assertEquals(enums.java.type.Direction.name, enums.java.type.Direction.safeValueOf("name"))
    assertEquals(enums.java.type.Direction.ordinal, enums.java.type.Direction.safeValueOf("ordinal"))
    assertEquals(enums.java.type.Direction.type__, enums.java.type.Direction.safeValueOf("type"))
    assertEquals(enums.java.type.Direction.Companion, enums.java.type.Direction.safeValueOf("Companion"))
    assertIs<NullPointerException>(
        assertFails {
          enums.java.type.Direction.safeValueOf(null)
        }
    )
  }

  @Test
  fun javaClasses() {
    assertEquals(enums.java.type.Gravity.TOP, enums.java.type.Gravity.safeValueOf("TOP"))
    assertEquals(enums.java.type.Gravity.top2, enums.java.type.Gravity.safeValueOf("top2"))
    val unknown = enums.java.type.Gravity.safeValueOf("newGravity")
    assertEquals(enums.java.type.Gravity.UNKNOWN__::class.java, unknown::class.java)
    assertEquals("newGravity", unknown.rawValue)
    assertEquals(enums.java.type.Gravity.safeValueOf("newGravity"), unknown)
    assertNotEquals(enums.java.type.Gravity.safeValueOf("newGravity2"), unknown)
    assertEquals(enums.java.type.Gravity.name, enums.java.type.Gravity.safeValueOf("name"))
    assertEquals(enums.java.type.Gravity.ordinal, enums.java.type.Gravity.safeValueOf("ordinal"))
    assertEquals(enums.java.type.Gravity.type__, enums.java.type.Gravity.safeValueOf("type"))
    assertEquals(enums.java.type.Gravity.Companion, enums.java.type.Gravity.safeValueOf("Companion"))
    assertIs<NullPointerException>(
        assertFails {
          enums.java.type.Gravity.safeValueOf(null)
        }
    )
  }


  @Test
  fun headerAndImpl() {
    assertEquals(Foo.header.rawValue, "header")
  }

  @Test
  fun type() {
    assertEquals(Direction.type__, Direction.safeValueOf("type"))
    assertEquals(Gravity.type__, Gravity.safeValueOf("type"))
    assertEquals(FooSealed.type_, FooSealed.safeValueOf("type"))
    assertEquals(FooEnum.type_, FooEnum.safeValueOf("type"))
  }

  @Test
  fun sealedClassesKnownValues() {
    // Order is important
    // knownValues() should return the same order that the values are declared in the schema
    // Convert to List because Array.equals uses referential equality, see
    // https://blog.jetbrains.com/kotlin/2015/09/feedback-request-limitations-on-data-classes/
    @Suppress("DEPRECATION")
    assertEquals(
        arrayOf(
            Gravity.TOP,
            Gravity.top2,
            Gravity.BOTTOM,
            Gravity.LEFT,
            Gravity.RIGHT,
            Gravity.Companion_,
            Gravity.name,
            Gravity.ordinal,
            Gravity.type__,
        ).toList(),
        Gravity.knownValues().toList()
    )
  }

  /**
   * This is only used to check it compiles properly
   */
  @Suppress("unused")
  fun foo(color: Color) {
    when (color.unwrap()) {
      Color.BLUEBERRY -> TODO()
      Color.CANDY -> TODO()
      Color.CHERRY -> TODO()
    }
  }

  /**
   * Turns a maybe unknown color value into a known one
   */
  private fun Color.unwrap(): Color.KNOWN__ = when (this) {
    is Color.UNKNOWN__ -> Color.CANDY
    // Sadly cannot use `else ->` here so we use explicit branches
    // See https://youtrack.jetbrains.com/issue/KT-18950/Smart-Cast-should-work-within-else-branch-for-sealed-subclasses
    is Color.KNOWN__ -> this
  }
}
