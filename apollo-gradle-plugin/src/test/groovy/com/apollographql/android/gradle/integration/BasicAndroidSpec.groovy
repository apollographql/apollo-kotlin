package com.apollographql.android.gradle.integration

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Shared
import spock.lang.Specification

import static com.apollographql.android.gradle.ApolloPluginTestHelper.*
import static org.apache.commons.io.FileUtils.copyFile

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
    assert new File(testProjectDir, "build/generated/source/apollo/fragment/SpeciesInformation.java").isFile()

    // verify that the custom type generated was Object.class because no customType mapping was specified
    assert new File(testProjectDir, "build/generated/source/apollo/type/CustomType.java").isFile()
    assert new File(testProjectDir, "build/generated/source/apollo/type/CustomType.java").getText('UTF-8').contains(
        "return Object.class;")

    // Optional is not added to the generated classes
    assert !new File(testProjectDir, "build/generated/source/apollo/com/example/DroidDetails.java").getText(
        'UTF-8').contains(
        "import com.apollographql.android.api.graphql.internal.Optional;")
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

  // ApolloExtension tests
  def "generateApolloClasses is up to date if inputs and apollo extension don't change"() {
    setup: "a testProject with a previous build"

    when:
    def result = GradleRunner.create().withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("generateApolloClasses", "-Dapollographql.skipRuntimeDep=true")
        .forwardStdError(new OutputStreamWriter(System.err)).build()

    then:
    result.task(":generateApolloClasses").outcome == TaskOutcome.UP_TO_DATE
  }

  def "adding a custom type to the build script re-generates the CustomType class"() {
    setup: "a testProject with a previous build and an apollo extension appended"
    new File("$testProjectDir/build.gradle") << "apollo { customTypeMapping { DateTime = \"java.util.Date\" } }"

    when:
    def result = GradleRunner.create().withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("generateApolloClasses", "-Dapollographql.skipRuntimeDep=true")
        .forwardStdError(new OutputStreamWriter(System.err)).build()

    then:
    // modifying the customTypeMapping should cause the task to be out of date
    // and the task should run again
    result.task(":generateApolloClasses").outcome == TaskOutcome.SUCCESS

    assert new File(testProjectDir, "build/generated/source/apollo/type/CustomType.java").isFile()
    assert new File(testProjectDir, "build/generated/source/apollo/type/CustomType.java").getText('UTF-8').contains(
        "return Date.class;")
  }

  def "changing the class for a customTypeMapping key regenerates with the new class"() {
    setup: "a testProject with a previous build and a modified apollo extension"
    replaceTextInFile(new File("$testProjectDir/build.gradle")) {
      it.replace("java.util.Date", "java.util.Currency")
    }

    when:
    def result = GradleRunner.create().withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("generateApolloClasses", "-Dapollographql.skipRuntimeDep=true")
        .forwardStdError(new OutputStreamWriter(System.err)).build()

    then:
    result.task(":generateApolloClasses").outcome == TaskOutcome.SUCCESS

    assert new File(testProjectDir, "build/generated/source/apollo/type/CustomType.java").isFile()
    assert new File(testProjectDir, "build/generated/source/apollo/type/CustomType.java").getText('UTF-8').contains(
        "return Currency.class;")
  }

  def "adding generateOptional in Apollo Extension generates classes with apollographql Optional"() {
    setup: "a testProject with a previous build and a modified build script"
    replaceTextInFile(new File("$testProjectDir/build.gradle")) {
      it.replace("apollo { ", "apollo { generateOptional = true \n")
    }

    when:
    def result = GradleRunner.create().withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("generateApolloClasses", "-Dapollographql.skipRuntimeDep=true")
        .forwardStdError(new OutputStreamWriter(System.err)).build()

    then:
    result.task(":generateApolloClasses").outcome == TaskOutcome.SUCCESS
    assert new File(testProjectDir, "build/generated/source/apollo/com/example/DroidDetails.java").isFile()
    assert new File(testProjectDir, "build/generated/source/apollo/com/example/DroidDetails.java").getText(
        'UTF-8').contains("import com.apollographql.android.api.graphql.internal.Optional;")
  }

  def cleanupSpec() {
    FileUtils.deleteDirectory(testProjectDir)
  }

  private static File setupBasicAndroidProject() {
    def destDir = createTempTestDirectory("basic")
    prepareProjectTestDir(destDir, ProjectType.Android, "basic", "basic")
    String schemaFilesFixtures = "src/test/testProject/android/schemaFilesFixtures"
    copyFile(new File(schemaFilesFixtures + "/oldswapi.json"), new File("$destDir/src/main/graphql/schema.json"))
    return destDir
  }
}
