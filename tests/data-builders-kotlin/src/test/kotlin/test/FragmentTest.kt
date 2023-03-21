package test

import data.builders.fragment.AnimalDetailsImpl
import data.builders.fragment.CatDetailsImpl
import data.builders.type.Animal
import data.builders.type.buildLion
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith

class FragmentTest {
  @Test
  fun monomorphicFragment() {
    val data = CatDetailsImpl.Data {
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
    val data = AnimalDetailsImpl.Data(Animal) {
      __typename = "Brontaroc"
      species = "alien"
      id = "43"
      country = "Trisolaris"
    }
    // The parsers know that onAnimal is always true despite the unknkown __typename
    assertEquals("alien", data.species)
    assertEquals("43", data.onAnimal.id)
  }

  @Test
  fun polymorphicFragmentMissingFields() {
    // The fake resolvers are not aware that on onAnimal is always true because the
    // __typename is unknown so this fails
    // XXX: we could be smarter about this
    // data.builders.fragment.AnimalDetailsImpl_ResponseAdapter$OnAnimal.fromJson(AnimalDetailsImpl_ResponseAdapter.kt:85)
    assertFailsWith(NullPointerException::class) {
      AnimalDetailsImpl.Data(Animal) {
        __typename = "Brontaroc"
        species = "alien"
      }
    }
  }



  @Test
  fun polymorphicFragmentNoTypename() {
    assertFails {
      AnimalDetailsImpl.Data(Animal) {
        species = ""
      }
    }
  }
}