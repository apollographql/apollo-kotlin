package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloTestCase
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
    myFixture.openFileInEditor(myFixture.createFile("build.gradle.kt", loadKtsAsKt("Apollo4Available.gradle.kts")))
    val highlightInfos = doHighlighting().filter { it.description == "Apollo Kotlin 4 is available" }
    assertSize(15, highlightInfos)
    listOf(6, 7, 8, 11, 12, 13, 15, 24, 25, 26, 27, 32, 33, 34, 35).forEachIndexed { index, line ->
      assertTrue(highlightInfos[index].line == line)
    }
  }
}
