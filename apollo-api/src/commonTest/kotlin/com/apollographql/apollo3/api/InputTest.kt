package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.Input.Companion.fromNullable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class InputTest {
  @Test
  fun testInputEqualsOnNotNullValue() {
    val value = "Hello world!"
    val stringInput = fromNullable(value)
    val anotherStringInput = fromNullable(value)
    assertEquals(stringInput, anotherStringInput)
  }

  @Test
  fun testInputNotEqualsOnDifferentValues() {
    val value = "Hello world!"
    val value2 = "Bye world!"
    val stringInput = fromNullable(value)
    val anotherStringInput = fromNullable(value2)
    assertNotEquals(stringInput, anotherStringInput)
  }

  @Test
  fun testInputEqualsOnNullValue() {
    val stringInput = fromNullable<String>(null)
    val anotherStringInput = fromNullable<String>(null)
    assertEquals(stringInput, anotherStringInput)
  }

  @Test
  fun testInputEqualsOnNotNullObjects() {
    val `object` = TestObject("Hello world!")
    val aInput = fromNullable(`object`)
    val anotherInput = fromNullable(`object`)
    assertEquals(aInput, anotherInput)
  }

  @Test
  fun testInputEqualsOnEqualObjectsWithDifferentReferences() {
    val object1 = TestObject("Hello world!")
    val object2 = TestObject("Hello world!")
    val input1 = fromNullable(object1)
    val input2 = fromNullable(object2)
    assertEquals(input1, input2)
  }

  @Test
  fun testInputNotEqualsOnDifferentObjects() {
    val `object` = TestObject("Hello world!")
    val anotherObject = TestObject("Bye world!")
    val aInput = fromNullable(`object`)
    val anotherInput = fromNullable(anotherObject)
    assertNotEquals(aInput, anotherInput)
  }

  @Test
  fun testInputEqualsOnObjectsWithNullValue() {
    val `object` = TestObject(null)
    val aInput = fromNullable(`object`)
    val anotherInput = fromNullable(`object`)
    assertEquals(aInput, anotherInput)
  }

  @Test
  fun testInputNotEqualsWhenAnObjectIsNull() {
    val `object` = TestObject(null)
    val aInput = fromNullable(`object`)
    val anotherInput: Input<TestObject> = fromNullable(null)
    assertNotEquals(aInput, anotherInput)
  }

  //==================================================================
  //==================================================================
  internal data class TestObject(private val value: String?)
}
