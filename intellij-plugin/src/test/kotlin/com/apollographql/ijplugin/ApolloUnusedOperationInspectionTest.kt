package com.apollographql.ijplugin

import com.apollographql.ijplugin.inspection.ApolloUnusedOperationInspection
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ApolloUnusedOperationInspectionTest : ApolloTestCase() {
  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(ApolloUnusedOperationInspection())
  }

  @Test
  fun testNoUsageAndQuickFix() {
    myFixture.configureFromTempProjectFile("src/main/graphql/ComputersQuery.graphql")
    var highlightInfos = myFixture.doHighlighting()
    assertTrue(highlightInfos.any { it.description == "Unused operation" })

    val quickFixAction = myFixture.findSingleIntention("Delete operation");
    assertNotNull(quickFixAction)
    myFixture.launchAction(quickFixAction)
    myFixture.checkResult("""
      fragment ScreenFields on Screen {
          isColor
      }

    """.trimIndent());

    highlightInfos = myFixture.doHighlighting()
    assertTrue(highlightInfos.none { it.description == "Unused operation" })

  }

  @Test
  fun testUsage() {
    myFixture.configureFromTempProjectFile("src/main/graphql/AnimalsQuery.graphql")
    val highlightInfos = myFixture.doHighlighting()
    assertTrue(highlightInfos.none { it.description == "Unused operation" })
  }
}
