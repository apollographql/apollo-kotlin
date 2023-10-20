package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloTestCase
import com.intellij.testFramework.TestDataPath
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@TestDataPath("\$CONTENT_ROOT/testData/inspection")
@RunWith(JUnit4::class)
class ApolloEndpointNotConfiguredInspectionTest : ApolloTestCase() {

  override fun getTestDataPath() = "src/test/testData/inspection"

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(ApolloEndpointNotConfiguredInspection())
  }

  @Test
  fun testInspectionBuildGradleKts() {
    myFixture.openFileInEditor(myFixture.createFile("build.gradle.kt", loadKtsAsKt("EndpointNotConfigured.gradle.kts")))
    var highlightInfos = doHighlighting().filter { it.description == "GraphQL endpoint not configured" }
    assertSize(2, highlightInfos)
    assertTrue(highlightInfos[0].line == 6)
    assertTrue(highlightInfos[1].line == 24)

    moveCaret("service(\"a\")")
    var introspectionQuickFixAction = myFixture.findSingleIntention("Add introspection block")
    assertNotNull(introspectionQuickFixAction)
    myFixture.launchAction(introspectionQuickFixAction)

    moveCaret("service(\"d\")")
    introspectionQuickFixAction = myFixture.findSingleIntention("Add introspection block")
    assertNotNull(introspectionQuickFixAction)
    myFixture.launchAction(introspectionQuickFixAction)

    myFixture.checkResult(loadKtsAsKt("EndpointNotConfigured_after.gradle.kts"))
    highlightInfos = doHighlighting()
    assertTrue(highlightInfos.none { it.description == "GraphQL endpoint not configured" })
  }
}
