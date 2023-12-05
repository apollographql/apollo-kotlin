package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ApolloOneOfInputCreationInspectionTest : ApolloTestCase() {
  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(ApolloOneOfInputCreationInspection())
  }

  @Test
  fun testOneOfConstructorInvocations() {
    myFixture.configureFromTempProjectFile("src/main/kotlin/com/example/OneOf.kt")
    val highlightInfos = doHighlighting()
    assertTrue(highlightInfos.any { it.description == "@oneOf input class constructor must have exactly one argument" && it.text == "FindUserInput" && it.line == 8 })
    assertTrue(highlightInfos.any { it.description == "@oneOf input class constructor must have exactly one argument" && it.text == "FindUserInput" && it.line == 10 })
    assertTrue(highlightInfos.any { it.description == "@oneOf input class argument must be Present" && it.text == "FindUserInput" && it.line == 20 })
    assertTrue(highlightInfos.any { it.description == "@oneOf input class builder must have exactly one field set" && it.text == "name(\"John\")" && it.line == 28 })
    assertTrue(highlightInfos.any { it.description == "@oneOf input class argument must be non-null" && it.text == "email(null)" && it.line == 36 })
    assertTrue(highlightInfos.any { it.description == "@oneOf input class argument must be non-null" && it.text == "email(someNullableEmail)" && it.line == 41 && it.type.getSeverity(null).name == "WARNING" })
  }
}
