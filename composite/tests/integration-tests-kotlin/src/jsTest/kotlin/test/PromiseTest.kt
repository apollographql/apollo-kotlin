package test

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

class PromiseTest {
  var inTest = false

  @BeforeTest
  fun before() {
    inTest = true
  }

  @AfterTest
  fun after() {
    inTest = false
  }

  @Test
  fun withoutPromise() {
    assertTrue(inTest)
  }

  @Test
  fun withPromise() = GlobalScope.promise {
    assertTrue(inTest) // FAIL?!
  }
}
