package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloTestCase
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.TestDataPath
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@TestDataPath("\$CONTENT_ROOT/testData/inspection/MissingGraphQLDefinitionImport")
@RunWith(JUnit4::class)
class ApolloMissingGraphQLDefinitionImportInspectionTest : ApolloTestCase() {

  override fun getTestDataPath() = "src/test/testData/inspection/MissingGraphQLDefinitionImport"

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(ApolloMissingGraphQLDefinitionImportInspection())
  }

  @Test
  fun missingCatch() {
    myFixture.copyFileToProject("missing-catch.graphqls", "missing-catch.graphqls")
    myFixture.copyFileToProject("missing-catch.config.yml", "graphql.config.yml")
    myFixture.configureByFile("missing-catch.graphql")

    var highlightInfos = doHighlighting()
    assertTrue(highlightInfos.any { it.description == "Unresolved directive: catch" })
    val quickFixAction = myFixture.findSingleIntention("Import directive 'catch'")
    assertNotNull(quickFixAction)

    // Apply quickfix
    myFixture.launchAction(quickFixAction)
    highlightInfos = doHighlighting()
    assertTrue(highlightInfos.none { it.description == "Unresolved directive: catch" })

    myFixture.openFileInEditor(myFixture.findFileInTempDir("extra.graphqls"))
    myFixture.checkResultByFile("missing-catch-extra_after.graphqls", true)
  }


  @Test
  fun missingCatchTo() {
    myFixture.copyFileToProject("missing-CatchTo.graphqls", "missing-CatchTo.graphqls")
    myFixture.copyFileToProject("missing-CatchTo-extra.graphqls", "extra.graphqls")
    myFixture.copyFileToProject("missing-CatchTo.config.yml", "graphql.config.yml")
    myFixture.configureByFile("missing-CatchTo.graphql")

    var highlightInfos = doHighlighting()
    assertTrue(highlightInfos.any { it.description == "Unresolved enum: CatchTo" })
    val quickFixAction = myFixture.findSingleIntention("Import enum 'CatchTo'")
    assertNotNull(quickFixAction)

    // Apply quickfix
    myFixture.launchAction(quickFixAction)
    highlightInfos = doHighlighting()
    assertTrue(highlightInfos.none { it.description == "Unresolved enum: CatchTo" })

    myFixture.openFileInEditor(myFixture.findFileInTempDir("extra.graphqls"))
    myFixture.checkResultByFile("missing-CatchTo-extra_after.graphqls", true)
  }

  @Test
  fun missingTargetNameTo() {
    myFixture.copyFileToProject("missing-targetName.graphqls", "missing-targetName.graphqls")
    myFixture.copyFileToProject("missing-targetName-extra.graphqls", "extra.graphqls")
    myFixture.copyFileToProject("missing-targetName.config.yml", "graphql.config.yml")
    myFixture.configureByFile("missing-targetName.graphql")

    var highlightInfos = doHighlighting()
    assertTrue(highlightInfos.any { it.description == "Usage of implicit directive: targetName" && it.severity == HighlightSeverity.WEAK_WARNING })
    val quickFixAction = myFixture.findSingleIntention("Import directive 'targetName'")
    assertNotNull(quickFixAction)

    // Apply quickfix
    myFixture.launchAction(quickFixAction)
    highlightInfos = doHighlighting()
    assertTrue(highlightInfos.none { it.description == "Usage of implicit directive: targetName" })

    myFixture.openFileInEditor(myFixture.findFileInTempDir("extra.graphqls"))
    myFixture.checkResultByFile("missing-targetName-extra_after.graphqls", true)
  }

}
