package test

import codegen.models.HeroHumanOrDroidQuery
import codegen.models.builder.Data
import codegen.models.builder.buildDroid
import codegen.models.builder.buildHuman
import codegen.models.builder.resolver.DefaultFakeResolver
import kotlin.test.Test
import kotlin.test.assertEquals

class WhenTest {
  @Test
  fun exhaustiveWhen() {
    test(
        HeroHumanOrDroidQuery.Data(DefaultFakeResolver()) {
          hero = buildHuman {  }
        }
    )
    test(
        HeroHumanOrDroidQuery.Data(DefaultFakeResolver()) {
          hero = buildDroid {  }
        }
    )
  }

  private fun test(data: HeroHumanOrDroidQuery.Data) {
    when (val hero = data.hero) {
      is HeroHumanOrDroidQuery.HumanHero -> {
        assertEquals("homePlanet", hero.onHuman.homePlanet)
      }
      is HeroHumanOrDroidQuery.DroidHero -> {
        assertEquals("primaryFunction", hero.onDroid.primaryFunction)
      }
      is HeroHumanOrDroidQuery.OtherHero -> {
        assertEquals("", hero.name)
      }
      null -> error("")
    }
  }
}