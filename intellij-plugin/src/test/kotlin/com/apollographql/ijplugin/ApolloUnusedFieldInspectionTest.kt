package com.apollographql.ijplugin

import com.apollographql.ijplugin.inspection.ApolloUnusedFieldInspection
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ApolloUnusedFieldInspectionTest : ApolloTestCase() {
  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(ApolloUnusedFieldInspection())
  }

  @Test
  fun testUnusedFields() {
    myFixture.configureFromTempProjectFile("src/main/graphql/AnimalsQuery.graphql")
    var highlightInfos = doHighlighting()
    // id is unused
    assertTrue(highlightInfos.any { it.description == "Unused field" && it.text == "id" && it.line == 3})
    // name is used
    assertTrue(highlightInfos.none { it.description == "Unused field" && it.text == "name" })
    // id inside the fragment is used
    assertTrue(highlightInfos.none { it.description == "Unused field" && it.text == "id" && it.line > 3 })

    moveCaret("id")

    val quickFixAction = myFixture.findSingleIntention("Delete field");
    assertNotNull(quickFixAction)
    myFixture.launchAction(quickFixAction)
    myFixture.checkResult("""
      query animals {
        animals {
          name
          ... on Cat {
            meowVolume
            fieldOnDogAndCat
          }
          ... on dog {
            id
            barkVolume
            fieldOnDogAndCat
          }
        }
      }

    """.trimIndent());

    highlightInfos = doHighlighting()
    assertTrue(highlightInfos.none { it.description == "Unused field" && it.text == "id" })
  }

  @Test
  fun testUnusedInlineFragments() {
    myFixture.configureFromTempProjectFile("src/main/graphql/AnimalsQuery.graphql")
    var highlightInfos = doHighlighting()
    // ... on Cat is unused
    assertTrue(highlightInfos.any { it.description == "Unused field" && it.text.startsWith("... on Cat") && it.line == 5})
    // ... on dog is used
    assertTrue(highlightInfos.none { it.description == "Unused field" && it.text.startsWith("... on dog") })

    moveCaret("... on Cat")

    val quickFixAction = myFixture.findSingleIntention("Delete field");
    assertNotNull(quickFixAction)
    myFixture.launchAction(quickFixAction)
    myFixture.checkResult("""
      query animals {
        animals {
          id
          name
          ... on dog {
            id
            barkVolume
            fieldOnDogAndCat
          }
        }
      }

    """.trimIndent());

    highlightInfos = doHighlighting()
    assertTrue(highlightInfos.none { it.description == "Unused field" && it.text.startsWith("... on Cat")})
  }
}
