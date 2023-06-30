package com.apollographql.ijplugin

import com.apollographql.ijplugin.inspection.ApolloSchemaInGraphqlFileInspection
import com.apollographql.ijplugin.util.cast
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.testFramework.TestDataPath
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@TestDataPath("\$CONTENT_ROOT/testData/inspection")
@RunWith(JUnit4::class)
class ApolloSchemaInGraphqlFileInspectionTest : ApolloTestCase() {

  override fun getTestDataPath() = "src/test/testData/inspection"

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(ApolloSchemaInGraphqlFileInspection())
  }

  @Test
  fun testInspection() {
    myFixture.configureByFile("SchemaInGraphqlFile.graphql")
    val highlightInfos = myFixture.doHighlighting()
    assertTrue(highlightInfos.any { it.description == "Schema definitions must reside in a .graphqls file" })
    val quickFixAction = myFixture.findSingleIntention("Rename file with .graphqls extension");
    assertNotNull(quickFixAction)
    myFixture.launchAction(quickFixAction)
    TestCase.assertEquals("SchemaInGraphqlFile.graphqls", myFixture.editor.cast<EditorEx>()!!.virtualFile.name)
  }
}
