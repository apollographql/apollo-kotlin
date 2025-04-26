package test

import com.apollographql.apollo.exception.NullOrMissingField
import data.builders.builder.CatBuilder
import data.builders.builder.Data
import data.builders.builder.OtherAnimalBuilder
import data.builders.builder.buildLion
import data.builders.builder.resolver.DefaultFakeResolver
import data.builders.fragment.AnimalDetailsImpl
import data.builders.fragment.CatDetailsImpl
import data.builders.fragment.TrivialFragmentImpl
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FragmentTest {
  @Test
  fun monomorphicFragment() {
    val data = CatDetailsImpl.Data(DefaultFakeResolver()) {
      species = "cat"
      mustaches = 42
      bestFriend = buildLion {
        id = "43"
      }
    }

    assertEquals("Cat", data.__typename)
    assertEquals("Lion", data.bestFriend.__typename)
    assertEquals(42, data.mustaches)
    assertEquals("cat", data.species)
  }

  @Test
  fun polymorphicFragment() {
    val data = AnimalDetailsImpl.Data(OtherAnimalBuilder, DefaultFakeResolver()) {
      __typename = "Brontaroc"
      species = "alien"
    }
    assertEquals("alien", data.species)
  }

  @Test
  fun polymorphicFragment2() {
    val data = AnimalDetailsImpl.Data(CatBuilder, DefaultFakeResolver()) {
      species = "Maine Coon"
      mustaches = 42
    }
    assertEquals("Maine Coon", data.species)
  }


  @Test
  fun polymorphicFragmentMissingFields() {
    // The fake resolvers are not aware that on onAnimal is always true because the
    // __typename is unknown so this fails
    // XXX: we could be smarter about this (the parsers are
    // data.builders.fragment.AnimalDetailsImpl_ResponseAdapter$OnAnimal.fromJson(AnimalDetailsImpl_ResponseAdapter.kt:85)
    assertFailsWith(NullOrMissingField::class) {
      TrivialFragmentImpl.Data(OtherAnimalBuilder, DefaultFakeResolver()) {
        __typename = "Brontaroc"
        species = "alien"
      }
    }
  }
}
