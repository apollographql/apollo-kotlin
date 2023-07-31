package com.apollographql.ijplugin

import com.apollographql.ijplugin.inspection.Apollo4AvailableTomlInspection
import com.intellij.testFramework.TestDataPath
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@TestDataPath("\$CONTENT_ROOT/testData/inspection")
@RunWith(JUnit4::class)
class Apollo4AvailableTomlInspectionTest : ApolloTestCase() {

  override fun getTestDataPath() = "src/test/testData/inspection"

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(Apollo4AvailableTomlInspection())
  }

  @Test
  fun testInspection() {
    myFixture.configureByFile("Apollo4AvailableToml.versions.toml")
    myFixture.checkHighlighting()
  }
}
