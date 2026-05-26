package test

import com.google.common.truth.Truth
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Test
import util.TestUtils
import util.replaceInText
import java.io.File

class ParserOptionsTests {
  @Test
  fun allowDirectivesOnDirectivesSetToTrueSucceeds() {
    TestUtils.withTestProject("parser-options") { dir ->
      TestUtils.executeTask("generateApolloSources", dir)
    }
  }

  @Test
  fun allowDirectivesOnDirectivesNotSetFails() {
    TestUtils.withTestProject("parser-options") { dir ->
      File(dir, "build.gradle.kts").apply {
        replaceInText("@OptIn(ApolloExperimental::class)", "")
        replaceInText("allowDirectivesOnDirectives.set(true)", "")
      }
      try {
        TestUtils.executeTask("generateApolloSources", dir)
      } catch (e: UnexpectedBuildFailure) {
        Truth.assertThat(e.message).contains("Experimental `allowDirectivesOnDirectives` must be set to true")
      }
    }
  }
}
