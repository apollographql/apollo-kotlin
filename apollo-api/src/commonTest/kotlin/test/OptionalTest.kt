package test

import com.apollographql.apollo3.api.Optional
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertIs

class OptionalTest {
  @Test
  fun presentTest() {
    assertIs<Optional.Present<*>>(Optional.present("some value"))
    assertIs<Optional.Present<*>>(Optional.present(null))
  }

  @Test
  fun presentIfNotNullTest() {
    assertIs<Optional.Present<*>>(Optional.presentIfNotNull("some value"))
    assertIs<Optional.Absent>(Optional.presentIfNotNull(null))
  }
}
