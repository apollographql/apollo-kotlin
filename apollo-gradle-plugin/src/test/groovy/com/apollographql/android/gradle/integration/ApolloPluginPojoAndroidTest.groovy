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
class ApolloPluginPojoAndroidTest extends Specification {
  @Shared File testProjectDir

  def setupSpec() {
    testProjectDir = setupBasicAndroidProject()
  }

  def "builds successfully and generates expected outputs"() {
    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("generateApolloClasses")
        .forwardStdError(new OutputStreamWriter(System.err))
        .build()

    then:
    result.task(":generateApolloClasses").outcome == TaskOutcome.SUCCESS
    // IR Files generated successfully
    assert new File(testProjectDir,
        "build/generated/source/apollo/generatedIR/src/main/graphql/com/example/ReleaseAPI.json").isFile()
    assert new File(testProjectDir,
        "build/generated/source/apollo/generatedIR/src/main/graphql/com/example/DebugAPI.json").isFile()

    // Java classes generated successfully
    assert new File(testProjectDir, "build/generated/source/apollo/com/example/Films.java").isFile()
    assert new File(testProjectDir, "build/generated/source/apollo/com/example/type/CustomType.java").isFile()
  }

  def cleanupSpec() {
    FileUtils.deleteDirectory(testProjectDir)
  }

  private static File setupBasicAndroidProject() {
    def destDir = ApolloPluginTestHelper.createTempTestDirectory("pojo")
    ApolloPluginTestHelper.prepareProjectTestDir(destDir, ApolloPluginTestHelper.ProjectType.Android, "pojo", "pojo")
    return destDir
  }
}
