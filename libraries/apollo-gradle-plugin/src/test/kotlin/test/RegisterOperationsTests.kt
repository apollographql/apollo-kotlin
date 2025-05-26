package test

import util.TestUtils
import com.google.common.truth.Truth
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Test

class RegisterOperationsTests {
  @Test
  fun `simpleTest`() {
    TestUtils.withTestProject("register-operations") { dir ->
      try {
        TestUtils.executeTask("registerServiceApolloOperations", dir)
      } catch (e: UnexpectedBuildFailure) {
        // because there's no API key, this test will fail but that'll at least verify that we reach that point
        Truth.assertThat(e.message).contains("Cannot upload persisted query list")
      }
    }
  }
}