package test.trivial_fragment_spread

import trivial_fragment_spread.GetAnimalQuery
import trivial_fragment_spread.fragment.AnimalFragment
import kotlin.test.Test
import kotlin.test.assertEquals

class TrivialFragmentSpreadTest {
  @Test
  fun trivialFragmentSpreadIsGeneratedNonNull() {
    val data = GetAnimalQuery.Data(
        animal = GetAnimalQuery.Animal(
            __typename = "Animal",
            fragments = GetAnimalQuery.Animal.Fragments(
                animalFragment = AnimalFragment("Kitty")
            )
        )
    )

    assertEquals("Kitty", data.animal.fragments.animalFragment.name)
  }
}