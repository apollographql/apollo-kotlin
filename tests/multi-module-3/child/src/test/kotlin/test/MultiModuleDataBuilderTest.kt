package test

import multimodule3.child.GetAnimalQuery
import multimodule3.root.builder.Data
import multimodule3.root.builder.buildCat
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