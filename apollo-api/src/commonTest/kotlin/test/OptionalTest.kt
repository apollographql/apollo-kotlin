package test

import com.apollographql.apollo3.api.Optional
import kotlin.test.Test
import kotlin.test.assertIs

class OptionalTest {
  @Test
  fun presentIfNotNullTest() {
    assertIs<Optional.Present<*>>(Optional.presentIfNotNull("some value"))
    assertIs<Optional.Absent>(Optional.presentIfNotNull(null))
  }
}