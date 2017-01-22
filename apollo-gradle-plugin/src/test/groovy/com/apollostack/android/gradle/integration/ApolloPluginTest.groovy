package com.apollostack.android.gradle.integration

import com.apollostack.android.gradle.ApolloPluginTestHelper
import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Shared
import spock.lang.Specification

/**
 * The ordering of the tests in this file matters, cleanup only happens after all feature
 * methods run.
 */
class ApolloPluginTest extends Specification {
  @Shared def static testProjectDir = "src/test/testProject"

  def "testProject builds successfully and generates expected outputs"() {
    setup: "a testProject"
    prepareTestProjectDir()

    when:
    def result = GradleRunner.create().withProjectDir(new File(testProjectDir))
        .withPluginClasspath()
        .withArguments("build")
        .forwardStdError(new OutputStreamWriter(System.err)).build()

    then:
    result.task(":build").outcome == TaskOutcome.SUCCESS
    assert new File(testProjectDir, "build/generated/source/apollo/generatedIR/src/main/graphql/ReleaseAPI.json").isFile()
    assert new File(testProjectDir, "build/generated/source/apollo/generatedIR/src/main/graphql/DebugAPI.json").isFile()

    assert new File(testProjectDir, "build/generated/source/apollo/com/example/DroidDetails.java").isFile()
    assert new File(testProjectDir, "build/generated/source/apollo/com/example/Films.java").isFile()
  }

  def "installApolloCodegenTask runs incrementally and is up to date"() {
    setup: "a testProject"
    prepareTestProjectDir()

    when:
    def result = GradleRunner.create().withProjectDir(new File(testProjectDir))
        .withPluginClasspath()
        .withArguments("installApolloCodegen")
        .forwardStdError(new OutputStreamWriter(System.err)).build()

    then:
    result.task(":installApolloCodegen").outcome == TaskOutcome.UP_TO_DATE
  }

  def "installApolloCodegenTask runs if node_modules directory is altered"() {
    setup: "a testProject with a modified node_modules directory"
    prepareTestProjectDir()
    FileUtils.deleteDirectory(new File(testProjectDir, "node_modules"))

    when:
    def result = GradleRunner.create().withProjectDir(new File(testProjectDir))
        .withPluginClasspath()
        .withArguments("installApolloCodegen")
        .forwardStdError(new OutputStreamWriter(System.err)).build()

    then:
    result.task(":installApolloCodegen").outcome == TaskOutcome.SUCCESS
  }

  def "installApolloCodegenTask runs if package.json directory is missing"() {
    setup: "a testProject with a removed package.json file"
    prepareTestProjectDir()
    new File(testProjectDir, "package.json").delete()

    when:
    def result = GradleRunner.create().withProjectDir(new File(testProjectDir))
        .withPluginClasspath()
        .withArguments("installApolloCodegen")
        .forwardStdError(new OutputStreamWriter(System.err)).build()

    then:
    result.task(":installApolloCodegen").outcome == TaskOutcome.SUCCESS
  }

  def "generateApolloIR generates IR files successfully"() {
    setup: "a testProject with a clean build directory"
    prepareTestProjectDir()

    when:
    def result = GradleRunner.create().withProjectDir(new File(testProjectDir))
        .withPluginClasspath()
        .withArguments("clean", "generateApolloIR")
        .forwardStdError(new OutputStreamWriter(System.err)).build()

    then:
    result.task(":generateApolloIR").outcome == TaskOutcome.SUCCESS
    assert new File(testProjectDir, "build/generated/source/apollo/generatedIR/src/main/graphql/ReleaseAPI.json").isFile()
    assert new File(testProjectDir, "build/generated/source/apollo/generatedIR/src/main/graphql/DebugAPI.json").isFile()
  }

  def "generateApolloClasses generates java classes successfully"() {
    setup: "a testProject with a clean build directory"
    prepareTestProjectDir()

    when:
    def result = GradleRunner.create().withProjectDir(new File(testProjectDir))
        .withPluginClasspath()
        .withArguments("clean", "generateApolloClasses")
        .forwardStdError(new OutputStreamWriter(System.err)).build()

    then:
    result.task(":generateApolloClasses").outcome == TaskOutcome.SUCCESS
    assert new File(testProjectDir, "build/generated/source/apollo/com/example/DroidDetails.java").isFile()
    assert new File(testProjectDir, "build/generated/source/apollo/com/example/Films.java").isFile()
  }

  def "instrumentation tests run sucessfully"() {
    // TODO: run `connectedCheck` task on an emulator and have android instrumentation tests under testProject
  }

  def cleanupSpec() {
    def usedDirectories = ["build", "node_modules", ".gradle"]
    usedDirectories.each {
      FileUtils.deleteDirectory(new File(testProjectDir, it))
    }
  }
  private static def prepareTestProjectDir() {
    def localProperties = new File(testProjectDir, "local.properties")
    localProperties.write("sdk.dir=${ApolloPluginTestHelper.androidHome()}")
    return new File(testProjectDir)
  }
}
