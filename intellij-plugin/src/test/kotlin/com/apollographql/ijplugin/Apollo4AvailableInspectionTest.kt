package com.apollographql.ijplugin

import com.apollographql.ijplugin.inspection.Apollo4AvailableInspection
import com.intellij.testFramework.TestDataPath
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@TestDataPath("\$CONTENT_ROOT/testData/inspection")
@RunWith(JUnit4::class)
class Apollo4AvailableInspectionTest : ApolloTestCase() {

  override fun getTestDataPath() = "src/test/testData/inspection"

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(Apollo4AvailableInspection())
  }

  @Test
  fun testInspectionVersionsToml() {
    myFixture.configureByFile("Apollo4Available.versions.toml")
    checkHighlighting()
  }

  @Test
  fun testInspectionBuildGradleKts() {
    myFixture.openFileInEditor(myFixture.copyFileToProject("Apollo4Available.gradle.kts", "build.gradle.kts"))
    val highlightInfos = doHighlighting()
    assertTrue(highlightInfos.any { it.description == "Apollo Kotlin 4 is available" && it.line == 6 })
    assertTrue(highlightInfos.any { it.description == "Apollo Kotlin 4 is available" && it.line == 11 })
    assertTrue(highlightInfos.any { it.description == "Apollo Kotlin 4 is available" && it.line == 22 })
    assertTrue(highlightInfos.any { it.description == "Apollo Kotlin 4 is available" && it.line == 28 })
  }
}
