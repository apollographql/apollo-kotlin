package test

import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.exception.MissingValueException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.fail

class OptionalTest {
  @Test
  fun present() {
    assertIs<Optional.Present<*>>(Optional.present("some value"))
    assertIs<Optional.Present<*>>(Optional.present(null))
  }

  @Test
  fun presentIfNotNull() {
    assertIs<Optional.Present<*>>(Optional.presentIfNotNull("some value"))
    assertIs<Optional.Absent>(Optional.presentIfNotNull(null))
  }

  @Test
  fun getOrThrowNull() {
    val optional = Optional.present<String?>(null)
    val value = optional.getOrThrow()
    assertNull(value)
  }

  @Test
  fun getOrThrowPresent() {
    val optional = Optional.present<String?>("hello")
    val value = optional.getOrThrow()
    assertEquals("hello", value)
  }

  @Test
  fun getOrThrowAbsent() {
    val optional: Optional<String?> = Optional.absent()
    try {
      val value = optional.getOrThrow()
      fail("An exception was expected but got '$value' instead")
    } catch (_: MissingValueException) {
    }
  }
}
