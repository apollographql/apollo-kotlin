package test

import com.apollographql.apollo.integration.normalizer.type.Episode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class EnumTest {
  @Test
  fun safeValueOf() {
    assertEquals(Episode.EMPIRE, Episode.safeValueOf("EMPIRE"))
    assertIs<Episode.UNKNOWN__>(Episode.safeValueOf("NEW_EPISODE"))
  }
}
