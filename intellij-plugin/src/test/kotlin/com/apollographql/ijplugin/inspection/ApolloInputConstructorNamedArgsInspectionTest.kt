package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ApolloInputConstructorNamedArgsInspectionTest : ApolloTestCase() {
  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    myFixture.enableInspections(ApolloInputConstructorNamedArgsInspection())
  }

  @Test
  fun testOneOfConstructorInvocations() {
    myFixture.configureFromTempProjectFile("src/main/kotlin/com/example/InputConstructor.kt")
    val highlightInfos = doHighlighting()
    assertTrue(highlightInfos.any { it.description == "Input class constructor should use named arguments" && it.line == 9 })
    assertTrue(highlightInfos.any { it.description == "Input class constructor should use named arguments" && it.line == 16 })
    assertTrue(highlightInfos.any { it.description == "Input class constructor should use named arguments" && it.line == 21 })
    assertTrue(highlightInfos.any { it.description == "Input class constructor should use named arguments" && it.line == 27 })

    assertTrue(highlightInfos.any { it.description == "Input class constructor should use named arguments" && it.line == 35 })
    assertTrue(highlightInfos.any { it.description == "Input class constructor should use named arguments" && it.line == 42 })
    assertTrue(highlightInfos.any { it.description == "Input class constructor should use named arguments" && it.line == 47 })
    assertTrue(highlightInfos.any { it.description == "Input class constructor should use named arguments" && it.line == 53 })

    val addNamesToCallArgumentsIntention = "Add names to call arguments"
    applyIntentionOn(addNamesToCallArgumentsIntention, "PersonInput(\n" +
        "      firstName = Optional.present(\"John\"),\n" +
        "      Optional.present(\"Doe\"),\n" +
        "      Optional.present(42),\n" +
        "      Optional.present(AddressInput(street = Optional.present(\"street\"))),\n" +
        "  )")
    applyIntentionOn(addNamesToCallArgumentsIntention, "PersonInput(\n" +
        "      Optional.Present(\"John\"),\n" +
        "      Optional.present(\"Doe\"),\n" +
        "  )")
    applyIntentionOn(addNamesToCallArgumentsIntention, "PersonInput(\n" +
        "      Optional.Absent,\n" +
        "      Optional.absent(),\n" +
        "      Optional.present(42),\n" +
        "  )")
    applyIntentionOn(addNamesToCallArgumentsIntention, "PersonInput(\n" +
        "      Optional.Absent,\n" +
        "      Optional.absent(),\n" +
        "      getAge(),\n" +
        "  )")

    val changeToBuilderConstructionIntention = "Change to builder construction"
    applyIntentionOn(changeToBuilderConstructionIntention, "PersonInput(\n" +
        "      firstName = Optional.present(\"John\"),\n" +
        "      Optional.present(\"Doe\"),\n" +
        "      Optional.present(42),\n" +
        "      Optional.present(AddressInput(street = Optional.present(\"street\"))),\n" +
        "  )")
    applyIntentionOn(changeToBuilderConstructionIntention, "PersonInput(\n" +
        "      Optional.Present(\"John\"),\n" +
        "      Optional.present(\"Doe\"),\n" +
        "  )")
    applyIntentionOn(changeToBuilderConstructionIntention, "PersonInput(\n" +
        "      Optional.Absent,\n" +
        "      Optional.absent(),\n" +
        "      Optional.present(42),\n" +
        "  )")
    applyIntentionOn(changeToBuilderConstructionIntention, "PersonInput(\n" +
        "      Optional.Absent,\n" +
        "      Optional.absent(),\n" +
        "      getAge(),\n" +
        "  )")

    myFixture.checkResultByFile("../../intellij-plugin/src/test/testData/inspection/ApolloInputConstructorNamedArgsInspection/InputConstructor_after.kt")
  }

  private fun applyIntentionOn(intention: String, text: String) {
    moveCaret(text)
    val quickFixAction = myFixture.filterAvailableIntentions(intention).first()
    assertNotNull(quickFixAction)
    myFixture.launchAction(quickFixAction)
  }
}
