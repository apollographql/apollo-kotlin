package test

import util.TestUtils
import org.junit.Test

class GroovyTests {
  @Test
  fun groovyGradleProject() {
    TestUtils.withTestProject("groovy") { dir ->
      TestUtils.executeTaskAndAssertSuccess(":tasks", dir)
    }
  }
}
