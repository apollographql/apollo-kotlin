package com.apollographql.ijplugin

import com.apollographql.ijplugin.refactoring.migration.compattooperationbased.CompatToOperationBasedCodegenMigrationProcessor
import com.intellij.application.options.CodeStyle
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4


@TestDataPath("\$CONTENT_ROOT/testData/migration/compat-to-operationbased")
@RunWith(JUnit4::class)
class CompatToOperationBasedCodegenMigrationTest : LightJavaCodeInsightFixtureTestCase() {
  private val mavenLibraries = listOf(
      "com.apollographql.apollo3:apollo-annotations-jvm:3.8.0",
      "com.apollographql.apollo3:apollo-api-jvm:3.8.0",
      "com.apollographql.apollo3:apollo-mpp-utils-jvm:3.8.0",
      "com.apollographql.apollo3:apollo-runtime-jvm:3.8.0",
  )

  override fun getTestDataPath() = "src/test/testData/migration/compat-to-operationbased"

  private val projectDescriptor = object : DefaultLightProjectDescriptor() {
    override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
      for (library in mavenLibraries) {
        addFromMaven(model, library, true, DependencyScope.COMPILE)
      }
    }
  }

  override fun getProjectDescriptor(): LightProjectDescriptor {
    return projectDescriptor
  }

  override fun setUp() {
    super.setUp()
    // Set the indent size to 2 to match the fixtures (default is 4)
    val codeStyleSettings = CodeStyle.getSettings(project)
    val kotlinSettings = codeStyleSettings.getCommonSettings(KotlinLanguage.INSTANCE)
    kotlinSettings.indentOptions!!.INDENT_SIZE = 2
  }

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
