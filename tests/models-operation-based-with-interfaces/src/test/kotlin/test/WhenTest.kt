package test

import codegen.models.HeroHumanOrDroidQuery
import codegen.models.type.buildDroid
import codegen.models.type.buildHuman
import kotlin.test.Test
import kotlin.test.assertEquals

class WhenTest {
  @Test
  fun exhaustiveWhen() {
    test(
        HeroHumanOrDroidQuery.Data {
          hero = buildHuman {  }
        }
    )
    test(
        HeroHumanOrDroidQuery.Data {
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