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
class BasicAndroidSpec extends Specification {
  @Shared File testProjectDir

  def setupSpec() {
    testProjectDir = setupBasicAndroidProject()
  }

  def "builds successfully and generates expected outputs"() {
    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("build", "-Dapollographql.skipRuntimeDep=true")
        .forwardStdError(new OutputStreamWriter(System.err))
        .build()

    then:
    result.task(":build").outcome == TaskOutcome.SUCCESS
    // IR Files generated successfully
    assert new File(testProjectDir,
        "build/generated/source/apollo/generatedIR/release/src/main/graphql/ReleaseAPI.json").isFile()
    assert new File(testProjectDir,
        "build/generated/source/apollo/generatedIR/debug/src/main/graphql/DebugAPI.json").isFile()

    // Java classes generated successfully
    assert new File(testProjectDir, "build/generated/source/apollo/com/example/DroidDetails.java").isFile()
    assert new File(testProjectDir, "build/generated/source/apollo/com/example/Films.java").isFile()
    assert new File(testProjectDir, "build/generated/source/apollo/type/CustomType.java").isFile()
    assert new File(testProjectDir, "build/generated/source/apollo/fragment/SpeciesInformation.java").isFile()
  }

  def "installApolloCodegenTask is up to date if no changes occur to node_modules and package.json"() {
    setup: "a testProject with a previous build run"

    when:
    def result = GradleRunner.create().withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("installApolloCodegen")
        .forwardStdError(new OutputStreamWriter(System.err)).build()

    then:
    result.task(":installApolloCodegen").outcome == TaskOutcome.UP_TO_DATE
  }

  def "installApolloCodegenTask gets outdated if node_modules directory is altered"() {
    setup: "a testProject with a deleted node_modules directory"
    FileUtils.deleteDirectory(new File(testProjectDir, "node_modules"))

    when:
    def result = GradleRunner.create().withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("installApolloCodegen")
        .forwardStdError(new OutputStreamWriter(System.err)).build()

    then:
    result.task(":installApolloCodegen").outcome == TaskOutcome.SUCCESS
  }

  def "installApolloCodegenTask gets outdated if package.json is altered"() {
    setup: "a testProject with a deleted package.json directory"
    FileUtils.deleteDirectory(new File(testProjectDir, "node_modules"))

    when:
    def result = GradleRunner.create().withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("installApolloCodegen")
        .forwardStdError(new OutputStreamWriter(System.err)).build()

    then:
    result.task(":installApolloCodegen").outcome == TaskOutcome.SUCCESS
  }

  def "instrumentation tests run successfully"() {
    // TODO: run `connectedCheck` task on an emulator and have android instrumentation tests under testProject
  }

  def cleanupSpec() {
    FileUtils.deleteDirectory(testProjectDir)
  }

  private static File setupBasicAndroidProject() {
    def destDir = ApolloPluginTestHelper.createTempTestDirectory("basic")
    ApolloPluginTestHelper.prepareProjectTestDir(destDir, ApolloPluginTestHelper.ProjectType.Android, "basic",
        "basic")
    String schemaFilesFixtures = "src/test/testProject/android/schemaFilesFixtures"
    FileUtils.copyFile(new File(schemaFilesFixtures + "/oldswapi.json"), new File("$destDir/src/main/graphql/schema.json"))
    return destDir
  }
}
