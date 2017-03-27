package com.apollographql.android.gradle.integration

import com.apollographql.android.gradle.ApolloPluginTestHelper
import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Shared
import spock.lang.Specification

import static com.apollographql.android.gradle.ApolloPluginTestHelper.createTempTestDirectory
import static com.apollographql.android.gradle.ApolloPluginTestHelper.prepareProjectTestDir
import static org.apache.commons.io.FileUtils.copyFile

class LibraryProjectAndroidSpec extends Specification {
  @Shared File testProjectDir

  def setupSpec() {
    testProjectDir = setupAndroidLibraryProject()
  }

  def "builds successfully"() {
    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("build", "-Dapollographql.skipApi=true")
        .forwardStdError(new OutputStreamWriter(System.err))
        .build()

    then:
    result.task(":build").outcome == TaskOutcome.SUCCESS
  }

  def cleanupSpec() {
    FileUtils.deleteDirectory(testProjectDir)
  }

  private static File setupAndroidLibraryProject() {
    def destDir = createTempTestDirectory("libraryProject")
    prepareProjectTestDir(destDir, ApolloPluginTestHelper.ProjectType.Android, "basic", "libraryProject")
    String schemaFilesFixtures = "src/test/testProject/android/schemaFilesFixtures"
    copyFile(new File(schemaFilesFixtures + "/oldswapi.json"), new File("$destDir/src/main/graphql/schema.json"))
    return destDir
  }
}
