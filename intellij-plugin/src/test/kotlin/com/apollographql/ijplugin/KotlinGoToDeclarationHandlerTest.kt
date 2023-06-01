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

  @Test
  fun goToOperationDefinition() {
    myFixture.configureFromTempProjectFile("src/main/kotlin/com/example/Main.kt")
    val ktElement = elementAt<PsiElement>("AnimalsQuery()")!!
    val foundGqlDeclarationElements = kotlinGoToDeclarationHandler.getGotoDeclarationTargets(ktElement, 0, editor)!!
    assertTrue(foundGqlDeclarationElements.size > 1)

    myFixture.configureFromTempProjectFile("src/main/graphql/AnimalsQuery.graphql")
    val gqlDeclarationElementInGqlFile = elementAt<GraphQLTypedOperationDefinition>("query Animals")!!

    assertEquals(gqlDeclarationElementInGqlFile, foundGqlDeclarationElements.first())
  }

  @Test
  fun goToFragmentDefinition() {
    myFixture.configureFromTempProjectFile("src/main/kotlin/com/example/Main.kt")
    val ktElement = elementAt<PsiElement>("ComputerFields(")!!
    val foundGqlDeclarationElements = kotlinGoToDeclarationHandler.getGotoDeclarationTargets(ktElement, 0, editor)!!
    assertTrue(foundGqlDeclarationElements.size > 1)

    myFixture.configureFromTempProjectFile("src/main/graphql/ComputerFields.graphql")
    val gqlDeclarationElementInGqlFile = elementAt<GraphQLFragmentDefinition>("fragment ComputerFields")!!

    assertEquals(gqlDeclarationElementInGqlFile, foundGqlDeclarationElements.first())
  }

  @Test
  fun goToField() {
    myFixture.configureFromTempProjectFile("src/main/kotlin/com/example/Main.kt")
    val ktElement = elementAt<PsiElement>("fieldOnDogAndCat")!!
    val foundGqlDeclarationElements = kotlinGoToDeclarationHandler.getGotoDeclarationTargets(ktElement, 0, editor)!!
    assertTrue(foundGqlDeclarationElements.size > 1)

    myFixture.configureFromTempProjectFile("src/main/graphql/AnimalsQuery.graphql")
    val gqlDeclarationElementInGqlFile = elementAt<GraphQLField>("fieldOnDogAndCat", afterText = "... on Dog {")!!

    assertEquals(gqlDeclarationElementInGqlFile, foundGqlDeclarationElements.first())
  }

  @Test
  fun goToEnumType() {
    myFixture.configureFromTempProjectFile("src/main/kotlin/com/example/Main.kt")
    val ktElement = elementAt<PsiElement>("MyEnum", afterText = "Optional.present(MyEnum.VALUE_C)")!!
    val foundGqlDeclarationElements = kotlinGoToDeclarationHandler.getGotoDeclarationTargets(ktElement, 0, editor)!!
    assertTrue(foundGqlDeclarationElements.size > 1)

    myFixture.configureFromTempProjectFile("src/main/graphql/schema.graphqls")
    val gqlDeclarationElementInGqlFile = elementAt<GraphQLTypeNameDefinition>("MyEnum", afterText = "enum MyEnum {")!!

    assertEquals(gqlDeclarationElementInGqlFile, foundGqlDeclarationElements.first())
  }

  @Test
  fun goToEnumValue() {
    myFixture.configureFromTempProjectFile("src/main/kotlin/com/example/Main.kt")
    val ktElement = elementAt<PsiElement>("VALUE_C")!!
    val foundGqlDeclarationElements = kotlinGoToDeclarationHandler.getGotoDeclarationTargets(ktElement, 0, editor)!!
    assertTrue(foundGqlDeclarationElements.size > 1)

    myFixture.configureFromTempProjectFile("src/main/graphql/schema.graphqls")
    val gqlDeclarationElementInGqlFile = elementAt<GraphQLEnumValue>("VALUE_C")!!

    assertEquals(gqlDeclarationElementInGqlFile, foundGqlDeclarationElements.first())
  }

  @Test
  fun goToInputType() {
    myFixture.configureFromTempProjectFile("src/main/kotlin/com/example/Main.kt")
    val ktElement = elementAt<PsiElement>("PersonInput")!!
    val foundGqlDeclarationElements = kotlinGoToDeclarationHandler.getGotoDeclarationTargets(ktElement, 0, editor)!!
    assertTrue(foundGqlDeclarationElements.size > 1)

    myFixture.configureFromTempProjectFile("src/main/graphql/schema.graphqls")
    val gqlDeclarationElementInGqlFile = elementAt<GraphQLTypeNameDefinition>("PersonInput", afterText = "input PersonInput")!!
    assertEquals(gqlDeclarationElementInGqlFile, foundGqlDeclarationElements.first())
  }

  @Test
  fun goToInputField() {
    myFixture.configureFromTempProjectFile("src/main/kotlin/com/example/Main.kt")
    val ktElement = elementAt<PsiElement>("lastName = ")!!
    val foundGqlDeclarationElements = kotlinGoToDeclarationHandler.getGotoDeclarationTargets(ktElement, 0, editor)!!
    assertTrue(foundGqlDeclarationElements.size > 1)

    myFixture.configureFromTempProjectFile("src/main/graphql/schema.graphqls")
    val gqlDeclarationElementInGqlFile = elementAt<GraphQLInputValueDefinition>("lastName")!!

    assertEquals(gqlDeclarationElementInGqlFile, foundGqlDeclarationElements.first())
  }

}

