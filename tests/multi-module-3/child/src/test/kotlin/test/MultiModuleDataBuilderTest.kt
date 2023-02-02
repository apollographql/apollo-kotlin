package test

import multimodule3.child.GetAnimalQuery
import multimodule3.root.type.buildCat
import org.junit.Test

class MultiModuleDataBuilderTest {
  @Test
  fun test() {

    @Suppress("UNUSED_VARIABLE")
    val data = GetAnimalQuery.Data {
      animal = buildCat {
        species = "cat"
      }
    }
  }
}