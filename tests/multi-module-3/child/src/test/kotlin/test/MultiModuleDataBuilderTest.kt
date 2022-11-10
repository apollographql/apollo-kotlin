package test

import org.junit.Test
import multimodule3.child.GetAnimalQuery
import multimodule3.root.type.buildCat

class MultiModuleDataBuilderTest {
  @Test
  fun test() {
    val data = GetAnimalQuery.Data {
      animal = buildCat {
        species = "cat"
      }
    }
  }
}