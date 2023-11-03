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
    assertSize(4, highlightInfos)
    assertTrue(highlightInfos[0].line == 6)
    assertTrue(highlightInfos[1].line == 11)
    assertTrue(highlightInfos[2].line == 24)
    assertTrue(highlightInfos[3].line == 31)
  }
}
