import js.test.CreateCustomerMutation
import kotlin.test.Test

class JsTest {
  @Test
  fun nameAndIdParametersCompile() {
    CreateCustomerMutation(name = "a", id = 42)
  }
}
