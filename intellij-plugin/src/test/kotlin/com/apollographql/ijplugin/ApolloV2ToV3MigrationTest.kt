package com.apollographql.ijplugin

import com.apollographql.ijplugin.refactoring.migration.v2tov3.ApolloV2ToV3MigrationProcessor
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


@TestDataPath("\$CONTENT_ROOT/testData/migration/v2-to-v3")
@RunWith(JUnit4::class)
class ApolloV2ToV3MigrationTest : LightJavaCodeInsightFixtureTestCase() {
  private val mavenLibraries = listOf(
      "com.apollographql.apollo:apollo-runtime:2.5.14",
      "com.apollographql.apollo:apollo-coroutines-support:2.5.14",
      "com.apollographql.apollo:apollo-normalized-cache-jvm:2.5.14",
      "com.apollographql.apollo:apollo-normalized-cache-sqlite-jvm:2.5.14",
      "com.apollographql.apollo:apollo-http-cache-api:2.5.14",
  )

  override fun getTestDataPath() = "src/test/testData/migration/v2-to-v3"

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
  fun testUpdatePackageName() = runMigration()

  @Test
  fun testUpdateMethodName() = runMigration()

  @Test
  fun testUpdateClassName() = runMigration()

  @Test
  fun testHttpCache() = runMigration()

  @Test
  fun testInMemoryNormalizedCache() = runMigration()

  @Test
  fun testSqlNormalizedCache() = runMigration()

  @Test
  fun testUpgradeGradlePluginInBuildGradleKts() = runMigration(extension = "gradle.kts", fileNameInProject = "build.gradle.kts")

  @Test
  fun testRemoveGradleDependenciesInBuildGradleKts() = runMigration(extension = "gradle.kts", fileNameInProject = "build.gradle.kts")

  @Test
  fun testRemoveGradleDependenciesInLibsVersionsToml() = runMigration(extension = "versions.toml", fileNameInProject = "libs.versions.toml")

  @Test
  fun testUpdateGradleDependenciesInBuildGradleKts() = runMigration(extension = "gradle.kts", fileNameInProject = "build.gradle.kts")

  @Test
  fun testUpdateGradleDependenciesInLibsVersionsToml() = runMigration(extension = "versions.toml", fileNameInProject = "libs.versions.toml")

  @Test
  fun testWatch() = runMigration()

  @Test
  fun testAddCustomTypeAdapter() = runMigration()

  @Test
  fun testUpdateCustomTypeMapping() = runMigration(extension = "gradle.kts", fileNameInProject = "build.gradle.kts")

  @Test
  fun testUpdateEnumValueUpperCase() = runMigration()

  @Test
  fun testUpdateInput() = runMigration()

  @Test
  fun testAddUseVersion2Compat() = runMigration(extension = "gradle.kts", fileNameInProject = "build.gradle.kts")

  @Test
  fun testUpdateGraphqlSourceDirectorySet() = runMigration(extension = "gradle.kts", fileNameInProject = "build.gradle.kts")

  @Test
  fun testUpdateOkHttpExecutionContext() = runMigration()

  @Test
  fun testUpdateOperationName() = runMigration()

  @Test
  fun testUpdateFileUpload() = runMigration()

  private fun runMigration(extension: String = "kt", fileNameInProject: String? = null) {
    val fileBaseName = getTestName(true)
    if (fileNameInProject != null) {
      myFixture.copyFileToProject("$fileBaseName.$extension", fileNameInProject)
    } else {
      myFixture.copyFileToProject("$fileBaseName.$extension")
    }

    ApolloV2ToV3MigrationProcessor(project).run()

    if (fileNameInProject != null) {
      myFixture.checkResultByFile(fileNameInProject, fileBaseName + "_after.$extension", true)
    } else {
      myFixture.checkResultByFile("$fileBaseName.$extension", fileBaseName + "_after.$extension", true)
    }
  }
}
