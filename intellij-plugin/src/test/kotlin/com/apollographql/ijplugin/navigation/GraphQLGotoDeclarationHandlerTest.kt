package com.apollographql.ijplugin.navigation

import com.apollographql.ijplugin.ApolloTestCase
import com.intellij.openapi.options.advanced.AdvancedSettings
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtParameter
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GraphQLGotoDeclarationHandlerTest : ApolloTestCase() {
  private val graphQLGotoDeclarationHandler = GraphQLGotoDeclarationHandler()

  private fun testNavigation(
      fromFile: String,
      fromElement: () -> PsiElement?,
      toFile: String,
      toElement: () -> PsiElement?,
      navigateEvenWithAdvancedSettingChecked: Boolean = false,
      multipleTarget: Boolean = false,
  ) {
    // Open the destination file
    myFixture.configureFromTempProjectFile(toFile)
    // Find the element to navigate to
    val ktDeclarationElementInKtFile = toElement()!!

    // Open the source file
    myFixture.configureFromTempProjectFile(fromFile)
    // Find the element to navigate from
    val gqlElement = fromElement()!!

    // Simulate navigation, with advanced setting unchecked
    AdvancedSettings.setBoolean("apollo.graphQLGoToDeclarationGeneratedCode", false)
    var foundKtDeclarationElements = graphQLGotoDeclarationHandler.getGotoDeclarationTargets(gqlElement, 0, editor)
    if (navigateEvenWithAdvancedSettingChecked) {
      assertNotNull(foundKtDeclarationElements)
    } else {
      assertNull(foundKtDeclarationElements)
      return
    }
    // Simulate navigation, with advanced setting checked
    AdvancedSettings.setBoolean("apollo.graphQLGoToDeclarationGeneratedCode", true)
    foundKtDeclarationElements = graphQLGotoDeclarationHandler.getGotoDeclarationTargets(gqlElement, 0, editor)!!

    if (multipleTarget) {
      // We want our target (Kotlin), but also the original targets (GraphQL)
      assertTrue(foundKtDeclarationElements.size > 1)
    } else {
      assertEquals(1, foundKtDeclarationElements.size)
    }

    // Make sure they're the same
    assertEquals(foundKtDeclarationElements.last(), ktDeclarationElementInKtFile)
  }

  @Test
  fun goToOperationClass() = testNavigation(
      fromFile = "src/main/graphql/AnimalsQuery.graphql",
      fromElement = { elementAt<PsiElement>("animals")!! },
      toFile = "build/generated/source/apollo/main/com/example/generated/AnimalsQuery.kt",
      toElement = { elementAt<KtClass>("class AnimalsQuery")!! },
      navigateEvenWithAdvancedSettingChecked = true,
  )

  @Test
  fun goToFragmentClass() = testNavigation(
      fromFile = "src/main/graphql/fragments/ComputerFields.graphql",
      fromElement = { elementAt<PsiElement>("computerFields")!! },
      toFile = "build/generated/source/apollo/main/com/example/generated/fragment/ComputerFields.kt",
      toElement = { elementAt<KtClass>("class ComputerFields")!! },
      navigateEvenWithAdvancedSettingChecked = true,
      multipleTarget = true
  )

  @Test
  fun goToField() = testNavigation(
      fromFile = "src/main/graphql/fragments/ComputerFields.graphql",
      fromElement = { elementAt<PsiElement>("resolution")!! },
      toFile = "build/generated/source/apollo/main/com/example/generated/fragment/ComputerFields.kt",
      toElement = { elementAt<KtParameter>("resolution")!! },
      multipleTarget = true
  )

  @Test
  fun goToEnumType() = testNavigation(
      fromFile = "src/main/graphql/schema.graphqls",
      fromElement = { elementAt<PsiElement>("myEnum", afterText = "enum myEnum {")!! },
      toFile = "build/generated/source/apollo/main/com/example/generated/type/MyEnum.kt",
      toElement = { elementAt<KtClass>("class MyEnum")!! },
  )

  @Test
  fun goToEnumValue() = testNavigation(
      fromFile = "src/main/graphql/schema.graphqls",
      fromElement = { elementAt<PsiElement>("VALUE_D", afterText = "enum myEnum {")!! },
      toFile = "build/generated/source/apollo/main/com/example/generated/type/MyEnum.kt",
      toElement = { elementAt<KtEnumEntry>("VALUE_D")!! },
  )

  @Test
  fun goToInputType() = testNavigation(
      fromFile = "src/main/graphql/schema.graphqls",
      fromElement = { elementAt<PsiElement>("personInput", afterText = "input personInput {")!! },
      toFile = "build/generated/source/apollo/main/com/example/generated/type/PersonInput.kt",
      toElement = { elementAt<KtClass>("class PersonInput")!! },
  )

  @Test
  fun goToInputField() = testNavigation(
      fromFile = "src/main/graphql/schema.graphqls",
      fromElement = { elementAt<PsiElement>("lastName", afterText = "input personInput {")!! },
      toFile = "build/generated/source/apollo/main/com/example/generated/type/PersonInput.kt",
      toElement = { elementAt<KtParameter>("lastName: String?")!! },
      // We have 2 targets because of builders
      multipleTarget = true,
  )
}

