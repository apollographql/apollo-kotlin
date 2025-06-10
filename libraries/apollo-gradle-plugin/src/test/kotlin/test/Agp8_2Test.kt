package test

import org.junit.Test
import util.TestUtils

class Agp8_2Test {

  @Test
  fun `android-plugin-max`() {
    TestUtils.withTestProject("android-plugin-max") { dir ->
      TestUtils.executeTaskAndAssertSuccess(":build", dir)
    }
  }
}