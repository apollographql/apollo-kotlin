package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloTestCase
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
    var highlightInfos = doHighlighting()
    assertTrue(highlightInfos.any { it.description == "Unused operation" })

    moveCaret("Computers")
    val quickFixAction = myFixture.findSingleIntention("Delete operation")
    assertNotNull(quickFixAction)
    myFixture.launchAction(quickFixAction)
    myFixture.checkResult("""
      fragment ScreenFields on Screen {
        isColor
      }

    """.trimIndent());

    highlightInfos = doHighlighting()
    assertTrue(highlightInfos.none { it.description == "Unused operation" })
  }

  @Test
  fun testUsage() {
    myFixture.configureFromTempProjectFile("src/main/graphql/AnimalsQuery.graphql")
    val highlightInfos = doHighlighting()
    assertTrue(highlightInfos.none { it.description == "Unused operation" })
  }
}
