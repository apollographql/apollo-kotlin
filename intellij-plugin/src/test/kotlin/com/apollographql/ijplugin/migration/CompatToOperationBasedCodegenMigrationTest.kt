package com.apollographql.ijplugin.migration

import com.apollographql.ijplugin.ApolloTestCase
import com.apollographql.ijplugin.refactoring.migration.compattooperationbased.CompatToOperationBasedCodegenMigrationProcessor
import com.intellij.testFramework.TestDataPath
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4


@TestDataPath("\$CONTENT_ROOT/testData/migration/compat-to-operationbased")
@RunWith(JUnit4::class)
class CompatToOperationBasedCodegenMigrationTest : ApolloTestCase() {
  override val mavenLibraries = listOf(
      "com.apollographql.apollo3:apollo-annotations-jvm:3.8.0",
      "com.apollographql.apollo3:apollo-api-jvm:3.8.0",
      "com.apollographql.apollo3:apollo-mpp-utils-jvm:3.8.0",
      "com.apollographql.apollo3:apollo-runtime-jvm:3.8.0",
  )

  override fun getTestDataPath() = "src/test/testData/migration/compat-to-operationbased"

  @Test
  fun testRemoveFragmentsField() = runMigration()

  @Test
  fun testReworkInlineFragmentFields() = runMigration()

  @Test
  fun testUpdateCodegenInBuildKts() = runMigration(extension = "gradle.kts", fileNameInProject = "build.gradle.kts")


  private fun runMigration(extension: String = "kt", fileNameInProject: String? = null) {
    val fileBaseName = getTestName(true)
    if (fileNameInProject != null) {
      myFixture.copyFileToProject("$fileBaseName.$extension", fileNameInProject)
    } else {
      myFixture.copyFileToProject("$fileBaseName.$extension")
    }

    CompatToOperationBasedCodegenMigrationProcessor(project).run()

    if (fileNameInProject != null) {
      myFixture.checkResultByFile(fileNameInProject, fileBaseName + "_after.$extension", true)
    } else {
      myFixture.checkResultByFile("$fileBaseName.$extension", fileBaseName + "_after.$extension", true)
    }
  }
}
