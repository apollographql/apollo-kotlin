package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloTestCase
import com.intellij.testFramework.TestDataPath
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@TestDataPath("\$CONTENT_ROOT/testData/inspection")
@RunWith(JUnit4::class)
class ApolloMissingGraphQLDefinitionImportInspectionTest : ApolloTestCase() {

  override fun getTestDataPath() = "src/test/testData/inspection"

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(ApolloMissingGraphQLDefinitionImportInspection())
  }

  @Test
  fun testInspection() {
    myFixture.copyFileToProject("MissingGraphQLDefinitionImport.graphqls", "MissingGraphQLDefinitionImport.graphqls")
    myFixture.copyFileToProject("MissingGraphQLDefinitionImport.config.yml", "graphql.config.yml")
    myFixture.configureByFile("MissingGraphQLDefinitionImport.graphql")

    var highlightInfos = doHighlighting()
    assertTrue(highlightInfos.any { it.description == "The directive is not imported" })
    val quickFixAction = myFixture.findSingleIntention("Import directive")
    assertNotNull(quickFixAction)

    // Apply quickfix
    myFixture.launchAction(quickFixAction)
    highlightInfos = doHighlighting()
    assertTrue(highlightInfos.none { it.description == "The directive is not imported" })

    myFixture.openFileInEditor(myFixture.findFileInTempDir("extra.graphqls"))
    myFixture.checkResultByFile("MissingGraphQLDefinitionImport_extra_after.graphqls", true)
  }
}
