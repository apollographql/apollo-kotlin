package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloTestCase
import com.intellij.testFramework.TestDataPath
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@TestDataPath("\$CONTENT_ROOT/testData/inspection/OneOfGraphQLViolation")
@RunWith(JUnit4::class)
class ApolloOneOfGraphQLViolationInspectionTest : ApolloTestCase() {

  override fun getTestDataPath() = "src/test/testData/inspection/OneOfGraphQLViolation"

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(ApolloOneOfGraphQLViolationInspection())
  }

  @Test
  fun testInspection() {
    myFixture.copyFileToProject("schema.graphqls", "schema.graphqls")
    myFixture.copyFileToProject("graphql.config.yml", "graphql.config.yml")
    myFixture.configureByFile("operations.graphql")

    checkHighlighting()
  }
}
