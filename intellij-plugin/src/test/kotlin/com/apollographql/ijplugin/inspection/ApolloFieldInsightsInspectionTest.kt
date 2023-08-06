package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloTestCase
import com.apollographql.ijplugin.gradle.ApolloKotlinService
import com.apollographql.ijplugin.studio.fieldinsights.ApolloFieldInsightsInspection
import com.apollographql.ijplugin.studio.fieldinsights.FieldInsightsService
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.replaceService
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@TestDataPath("\$CONTENT_ROOT/testData/inspection")
@RunWith(JUnit4::class)
class ApolloFieldInsightsInspectionTest : ApolloTestCase() {

  override fun getTestDataPath() = "src/test/testData/inspection"

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(ApolloFieldInsightsInspection())
    myFixture.project.replaceService(
        FieldInsightsService::class.java,
        FakeFieldInsightsService(),
        myFixture.testRootDisposable
    )
  }

  class FakeFieldInsightsService : FieldInsightsService {
    override fun fetchLatencies() {}
    override fun hasLatencies() = true
    override fun getLatency(serviceId: ApolloKotlinService.Id, typeName: String, fieldName: String) = 150.0
  }

  @Test
  fun testInspection() {
    myFixture.copyFileToProject("FieldInsights.graphqls", "FieldInsights.graphqls")
    myFixture.configureByFile("FieldInsights.graphql")
    var highlightInfos = doHighlighting()
    assertTrue(highlightInfos.any { it.description == "firstName has a high latency: ~150 ms" })
    val quickFixAction = myFixture.findSingleIntention("Enclose in @defer fragment")
    assertNotNull(quickFixAction)
    myFixture.launchAction(quickFixAction)
    myFixture.checkResultByFile("FieldInsights_after.graphql")

    highlightInfos = doHighlighting()
    assertTrue(highlightInfos.none { it.description == "firstName has a high latency: ~150 ms" })
  }
}
