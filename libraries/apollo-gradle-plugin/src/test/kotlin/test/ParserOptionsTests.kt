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
}
