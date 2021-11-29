package test

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.integration.normalizer.type.Episode
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ApolloExperimental::class)
class EnumTest {
  @Test
  fun safeValueOf() {
    assertEquals(Episode.EMPIRE, Episode.safeValueOf("EMPIRE"))

    // Note: Episode is generated as a sealed class (sealedClassesForEnumsMatching) for this
    // to work both with Kotlin and Java codegens
    assertEquals(Episode.UNKNOWN__("NEW_EPISODE"), Episode.safeValueOf("NEW_EPISODE"))
  }
}
