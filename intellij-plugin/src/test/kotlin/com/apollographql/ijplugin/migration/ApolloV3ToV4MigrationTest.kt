package com.apollographql.ijplugin.migration

import com.apollographql.ijplugin.ApolloTestCase
import com.apollographql.ijplugin.refactoring.migration.v3tov4.ApolloV3ToV4MigrationProcessor
import com.intellij.testFramework.TestDataPath
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4


@TestDataPath("\$CONTENT_ROOT/testData/migration/v3-to-v4")
@RunWith(JUnit4::class)
class ApolloV3ToV4MigrationTest : ApolloTestCase() {
  override val mavenLibraries = listOf(
      "com.apollographql.apollo3:apollo-annotations-jvm:3.8.2",
      "com.apollographql.apollo3:apollo-api-jvm:3.8.2",
      "com.apollographql.apollo3:apollo-mpp-utils-jvm:3.8.2",
      "com.apollographql.apollo3:apollo-runtime-jvm:3.8.2",
      "com.apollographql.apollo3:apollo-normalized-cache-jvm:3.8.2",
      "com.apollographql.apollo3:apollo-http-cache:3.8.2",
  )

  override fun getTestDataPath() = "src/test/testData/migration/v3-to-v4"

  @Test
  fun testUpgradeGradlePluginInBuildGradleKts() = runMigration(extension = "gradle.kts", fileNameInProject = "build.gradle.kts")

  @Test
  fun testUpdateGradleDependenciesInBuildGradleKts() = runMigration(extension = "gradle.kts", fileNameInProject = "build.gradle.kts")

  @Test
  fun testUpdateGradleDependenciesInLibsVersionsToml() = runMigration(extension = "versions.toml", fileNameInProject = "libs.versions.toml")

  @Test
  fun testDeprecations() = runMigration()

  @Test
  fun testEncloseInService() = runMigration(extension = "gradle.kts", fileNameInProject = "build.gradle.kts")

  @Test
  fun testGradleDeprecations() = runMigration(extension = "gradle.kts", fileNameInProject = "build.gradle.kts")

  @Test
  fun testUpdateCustomScalarsMapping() = runMigration(extension = "gradle.kts", fileNameInProject = "build.gradle.kts")

  @Test
  fun testUpdateScalarAdaptersInBuildKts() = runMigration(extension = "gradle.kts", fileNameInProject = "build.gradle.kts")

  @Test
  fun testMultiModule() = runMigration(extension = "gradle.kts", fileNameInProject = "build.gradle.kts")

  @Test
  fun testUpdateEnumClassUpperCase() = runMigration()

  @Test
  fun testAddLinkDirective() = runMigration(extension = "graphqls", fileNameInProject = "extra.graphqls")

  @Test
  fun testRemoveGraphqlConfigFiles() {
    myFixture.copyFileToProject("graphql.config.yml")
    ApolloV3ToV4MigrationProcessor(project).run()
    TestCase.assertNull(myFixture.tempDirFixture.getFile("graphql.config.yml"))
  }

  private fun runMigration(extension: String = "kt", fileNameInProject: String? = null) {
    val fileBaseName = getTestName(true)
    if (fileNameInProject != null) {
      myFixture.copyFileToProject("$fileBaseName.$extension", fileNameInProject)
    } else {
      myFixture.copyFileToProject("$fileBaseName.$extension")
    }

    ApolloV3ToV4MigrationProcessor(project).run()

    if (fileNameInProject != null) {
      myFixture.checkResultByFile(fileNameInProject, fileBaseName + "_after.$extension", true)
    } else {
      myFixture.checkResultByFile("$fileBaseName.$extension", fileBaseName + "_after.$extension", true)
    }
  }
}
