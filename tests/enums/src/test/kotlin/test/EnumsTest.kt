package test

import enums.kotlin.type.Direction
import enums.kotlin.type.Foo
import enums.kotlin.type.FooEnum
import enums.kotlin.type.FooSealed
import enums.kotlin.type.Gravity
import org.junit.Test
import kotlin.test.assertEquals

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
  fun kotlinSealedClasses() {
    assertEquals(Gravity.TOP, Gravity.safeValueOf("TOP"))
    @Suppress("DEPRECATION")
    assertEquals(Gravity.top2, Gravity.safeValueOf("top2"))
    assertEquals(Gravity.UNKNOWN__("newGravity"), Gravity.safeValueOf("newGravity"))
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
  }

  @Test
  fun javaClasses() {
    assertEquals(enums.java.type.Gravity.TOP, enums.java.type.Gravity.safeValueOf("TOP"))
    assertEquals(enums.java.type.Gravity.top2, enums.java.type.Gravity.safeValueOf("top2"))
    assertEquals(enums.java.type.Gravity.UNKNOWN__("newGravity"), enums.java.type.Gravity.safeValueOf("newGravity"))
    assertEquals(enums.java.type.Gravity.name, enums.java.type.Gravity.safeValueOf("name"))
    assertEquals(enums.java.type.Gravity.ordinal, enums.java.type.Gravity.safeValueOf("ordinal"))
    assertEquals(enums.java.type.Gravity.type__, enums.java.type.Gravity.safeValueOf("type"))
    assertEquals(enums.java.type.Gravity.Companion, enums.java.type.Gravity.safeValueOf("Companion"))
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
}
