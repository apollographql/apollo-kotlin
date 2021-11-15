package test

import com.apollographql.apollo3.integration.normalizer.type.Episode
import kotlin.test.Test
import kotlin.test.assertEquals

class EnumTest {
  @Test
  fun valueOf() {
    assertEquals(Episode.EMPIRE, Episode.safeValueOf("EMPIRE"))
    assertEquals(Episode.UNKNOWN__, Episode.safeValueOf("NEW_EPISODE"))
  }
}
