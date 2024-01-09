package com.apollographql.ijplugin.graphql

import com.apollographql.ijplugin.ApolloTestCase
import com.intellij.testFramework.TestDataPath
import com.intellij.util.ui.EDT
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@TestDataPath("\$CONTENT_ROOT/testData/graphql/ApolloClientDirectiveStripper")
@RunWith(JUnit4::class)
class ApolloClientDirectiveStripperTest : ApolloTestCase() {

  override fun getTestDataPath() = "src/test/testData/graphql/ApolloClientDirectiveStripper"

  @Test
  fun strip() {
    myFixture.copyFileToProject("schema.graphqls", "schema.graphqls")
    myFixture.copyFileToProject("graphql.config.yml", "graphql.config.yml")
    myFixture.configureByFile("operations.graphql")

    // Make sure GraphQLConfigProvider is initialized
    EDT.dispatchAllInvocationEvents()

    val stripped = stripApolloClientDirectives(myFixture.editor, myFixture.file.text)
    TestCase.assertEquals("""
      query MyQuery {
        person {
          identity {
            firstName
            lastName
          }
        }
      }

    """.trimIndent(), stripped)
  }
}
