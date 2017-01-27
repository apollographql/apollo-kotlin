package com.apollographql.android.gradle.integration

import com.apollographql.android.gradle.ApolloPluginTestHelper
import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Shared
import spock.lang.Specification

/**
 * The ordering of the tests in this file matters, cleanup only happens after all feature
 * methods run.
 */
class ApolloPluginBasicAndroidTest extends Specification {
  @Shared File testProjectDir

  def setupSpec() {
    testProjectDir = setupBasicAndroidProject()
  }

  def "builds successfully and generates expected outputs"() {
    when:
    def result = GradleRunner.create().withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("build")
        .forwardStdError(new OutputStreamWriter(System.err)).build()

    then:
    result.task(":build").outcome == TaskOutcome.SUCCESS

    // IR Files generated successfully
    assert new File(testProjectDir,
        "build/generated/source/apollo/generatedIR/src/main/graphql/ReleaseAPI.json").isFile()
    assert new File(testProjectDir,
        "build/generated/source/apollo/generatedIR/src/main/graphql/DebugAPI.json").isFile()

    // Java classes generated successfully
    assert new File(testProjectDir, "build/generated/source/apollo/com/example/DroidDetails.java").isFile()
    assert new File(testProjectDir, "build/generated/source/apollo/com/example/Films.java").isFile()
  }

  def "installApolloCodegenTask runs incrementally and is up to date after the first run"() {
    when:
    def result = GradleRunner.create().withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("installApolloCodegen")
        .forwardStdError(new OutputStreamWriter(System.err)).build()

    then:
    result.task(":installApolloCodegen").outcome == TaskOutcome.UP_TO_DATE
  }

  def "installApolloCodegenTask gets outdated if node_modules directory is altered"() {
    setup: "a testProject with a modified node_modules directory"
    FileUtils.deleteDirectory(new File(testProjectDir, "node_modules"))

    when:
    def result = GradleRunner.create().withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("installApolloCodegen")
        .forwardStdError(new OutputStreamWriter(System.err)).build()

    then:
    result.task(":installApolloCodegen").outcome == TaskOutcome.SUCCESS
  }

  def "installApolloCodegenTask gets outdated if package.json directory is missing"() {
    setup: "a testProject with a removed package.json file"
    new File(testProjectDir, "package.json").delete()

    when:
    def result = GradleRunner.create().withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("installApolloCodegen")
        .forwardStdError(new OutputStreamWriter(System.err)).build()

    then:
    result.task(":installApolloCodegen").outcome == TaskOutcome.SUCCESS
  }

  def "instrumentation tests run sucessfully"() {
    // TODO: run `connectedCheck` task on an emulator and have android instrumentation tests under testProject
  }

  def cleanupSpec() {
    FileUtils.deleteDirectory(testProjectDir)
  }

  private static File setupBasicAndroidProject() {
    def destDir = ApolloPluginTestHelper.createTempTestDirectory("basic")
    ApolloPluginTestHelper.prepareProjectTestDir(destDir, ApolloPluginTestHelper.ProjectType.Android, "basic",
        "basic")
    return destDir
  }
}
