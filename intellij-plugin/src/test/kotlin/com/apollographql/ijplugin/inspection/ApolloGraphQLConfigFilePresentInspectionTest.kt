package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloTestCase
import com.intellij.testFramework.TestDataPath
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@TestDataPath("\$CONTENT_ROOT/testData/inspection")
@RunWith(JUnit4::class)
class ApolloGraphQLConfigFilePresentInspectionTest : ApolloTestCase() {

  override fun getTestDataPath() = "src/test/testData/inspection"

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(ApolloGraphQLConfigFilePresentInspection())
  }

  @Test
  fun testGraphqlConfigPresenceError() {
    myFixture.configureByFile("graphql.config.yml")
    assertTrue(doHighlighting().any { it.description == "The Apollo plugin retrieves the GraphQL configuration from Gradle and doesn't use the GraphQL config file" })
//    checkHighlighting()
  }
}
