package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloTestCase
import com.intellij.testFramework.TestDataPath
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@TestDataPath("/testData/inspection")
@RunWith(JUnit4::class)
class MissingIntrospectionInspectionTest : ApolloTestCase() {

  override fun getTestDataPath() = "src/test/testData/inspection"

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(MissingIntrospectionInspection())
  }

  @Test
  fun testInspectionBuildGradleKts() {
    myFixture.openFileInEditor(myFixture.copyFileToProject("MissingIntrospection.gradle.kts", "build.gradle.kts"))
    var highlightInfos = doHighlighting().filter { it.description == "No introspection or registry block" }
    assertSize(2, highlightInfos)
    assertTrue(highlightInfos[0].line == 6)
    assertTrue(highlightInfos[1].line == 13)

    moveCaret("service(\"a\")")
    val introspectionQuickFixAction = myFixture.findSingleIntention("Add introspection block")
    assertNotNull(introspectionQuickFixAction)
    myFixture.launchAction(introspectionQuickFixAction)

    moveCaret("service(\"b\")")
    val registryQuickFixAction = myFixture.findSingleIntention("Add registry block")
    assertNotNull(registryQuickFixAction)
    myFixture.launchAction(registryQuickFixAction)

    myFixture.checkResultByFile("MissingIntrospection_after.gradle.kts")
    highlightInfos = doHighlighting()
    assertTrue(highlightInfos.none { it.description == "No introspection or registry block" })
  }
}
