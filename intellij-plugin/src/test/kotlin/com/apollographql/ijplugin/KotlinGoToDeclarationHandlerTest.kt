package com.apollographql.ijplugin

import com.apollographql.ijplugin.navigation.KotlinGotoDeclarationHandler
import com.intellij.lang.jsgraphql.psi.GraphQLEnumValue
import com.intellij.lang.jsgraphql.psi.GraphQLField
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLInputValueDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLTypeNameDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLTypedOperationDefinition
import com.intellij.psi.PsiElement
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KotlinGoToDeclarationHandlerTest : ApolloTestCase() {
  private val kotlinGoToDeclarationHandler = KotlinGotoDeclarationHandler()

  private fun testNavigation(
      fromFile: String,
      fromElement: () -> PsiElement?,
      toFile: String,
      toElement: () -> PsiElement?,
  ) {
    // Open the destination file
    myFixture.configureFromTempProjectFile(toFile)
    // Find the element to navigate to
    val gqlDeclarationElementInGqlFile = toElement()!!

    // Open the source file
    myFixture.configureFromTempProjectFile(fromFile)
    // Find the element to navigate from
    val ktElement = fromElement()!!

    // Simulate navigation
    val foundGqlDeclarationElements = kotlinGoToDeclarationHandler.getGotoDeclarationTargets(ktElement, 0, editor)!!
    // We want our target (gql), but also the original targets (Kotlin)
    assertTrue(foundGqlDeclarationElements.size > 1)

    // Make sure they're the same
    assertEquals(foundGqlDeclarationElements.first(), gqlDeclarationElementInGqlFile)
  }

  @Test
  fun goToOperationDefinition() = testNavigation(
      fromFile = "src/main/kotlin/com/example/Main.kt",
      fromElement = { elementAt<PsiElement>("AnimalsQuery()") },
      toFile = "src/main/graphql/AnimalsQuery.graphql",
      toElement = { elementAt<GraphQLTypedOperationDefinition>("query Animals") }
  )

  @Test
  fun goToFragmentDefinition() = testNavigation(
      fromFile = "src/main/kotlin/com/example/Main.kt",
      fromElement = { elementAt<PsiElement>("ComputerFields(") },
      toFile = "src/main/graphql/ComputerFields.graphql",
      toElement = { elementAt<GraphQLFragmentDefinition>("fragment ComputerFields") }
  )

  @Test
  fun goToField() = testNavigation(
      fromFile = "src/main/kotlin/com/example/Main.kt",
      fromElement = { elementAt<PsiElement>("fieldOnDogAndCat") },
      toFile = "src/main/graphql/AnimalsQuery.graphql",
      toElement = { elementAt<GraphQLField>("fieldOnDogAndCat", afterText = "... on Dog {") }
  )

  @Test
  fun goToEnumType() = testNavigation(
      fromFile = "src/main/kotlin/com/example/Main.kt",
      fromElement = { elementAt<PsiElement>("MyEnum", afterText = "Optional.present(MyEnum.VALUE_C)") },
      toFile = "src/main/graphql/schema.graphqls",
      toElement = { elementAt<GraphQLTypeNameDefinition>("MyEnum", afterText = "enum MyEnum {") }
  )

  @Test
  fun goToEnumValue() = testNavigation(
      fromFile = "src/main/kotlin/com/example/Main.kt",
      fromElement = { elementAt<PsiElement>("VALUE_C") },
      toFile = "src/main/graphql/schema.graphqls",
      toElement = { elementAt<GraphQLEnumValue>("VALUE_C", afterText = "enum MyEnum {") }
  )

  @Test
  fun goToInputType() = testNavigation(
      fromFile = "src/main/kotlin/com/example/Main.kt",
      fromElement = { elementAt<PsiElement>("PersonInput") },
      toFile = "src/main/graphql/schema.graphqls",
      toElement = { elementAt<GraphQLTypeNameDefinition>("PersonInput", afterText = "input PersonInput") }
  )

  @Test
  fun goToInputField() = testNavigation(
      fromFile = "src/main/kotlin/com/example/Main.kt",
      fromElement = { elementAt<PsiElement>("lastName = ") },
      toFile = "src/main/graphql/schema.graphqls",
      toElement = { elementAt<GraphQLInputValueDefinition>("lastName", afterText = "input PersonInput {") }
  )
}

