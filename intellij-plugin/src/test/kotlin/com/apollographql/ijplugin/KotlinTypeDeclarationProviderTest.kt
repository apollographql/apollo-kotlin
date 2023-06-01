package com.apollographql.ijplugin

import com.apollographql.ijplugin.navigation.KotlinTypeDeclarationProvider
import com.apollographql.ijplugin.util.resolveKtName
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLIdentifier
import com.intellij.lang.jsgraphql.psi.GraphQLTypeNameDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLTypedOperationDefinition
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KotlinTypeDeclarationProviderTest : ApolloTestCase() {
  private val kotlinTypeDeclarationProvider = KotlinTypeDeclarationProvider()

  @Test
  fun goToOperationDefinition() {
    myFixture.configureFromTempProjectFile("src/main/kotlin/com/example/Main.kt")
    val ktElement = elementAt<KtProperty>("animalsQuery")!!
    val foundGqlDeclarationElements = kotlinTypeDeclarationProvider.getSymbolTypeDeclarations(ktElement)!!
    assertTrue(foundGqlDeclarationElements.size > 1)

    myFixture.configureFromTempProjectFile("src/main/graphql/AnimalsQuery.graphql")
    val gqlDeclarationElementInGqlFile = elementAt<GraphQLTypedOperationDefinition>("query Animals")!!

    assertEquals(gqlDeclarationElementInGqlFile, foundGqlDeclarationElements.first())
  }

  @Test
  fun goToFragmentDefinition() {
    myFixture.configureFromTempProjectFile("src/main/kotlin/com/example/Main.kt")
    val ktElement = elementAt<KtProperty>("computerFields")!!
    val foundGqlDeclarationElements = kotlinTypeDeclarationProvider.getSymbolTypeDeclarations(ktElement)!!
    assertTrue(foundGqlDeclarationElements.size > 1)

    myFixture.configureFromTempProjectFile("src/main/graphql/ComputerFields.graphql")
    val gqlDeclarationElementInGqlFile = elementAt<GraphQLFragmentDefinition>("fragment ComputerFields")!!

    assertEquals(gqlDeclarationElementInGqlFile, foundGqlDeclarationElements.first())
  }

  @Test
  fun goToField() {
    myFixture.configureFromTempProjectFile("src/main/kotlin/com/example/Main.kt")
    val ktElement = elementAt<KtReferenceExpression>("fieldOnDogAndCat")!!
    val resolved = ktElement.resolveKtName()!!
    val foundGqlDeclarationElements = kotlinTypeDeclarationProvider.getSymbolTypeDeclarations(resolved)!!
    assertTrue(foundGqlDeclarationElements.size > 1)

    myFixture.configureFromTempProjectFile("src/main/graphql/schema.graphqls")
    val gqlDeclarationElementInGqlFile = elementAt<GraphQLIdentifier>("fieldOnDogAndCat", afterText = "type Dog implements Animal {")!!

    assertEquals(gqlDeclarationElementInGqlFile, foundGqlDeclarationElements.first())
  }

  @Test
  fun goToEnumType() {
    myFixture.configureFromTempProjectFile("src/main/kotlin/com/example/Main.kt")
    val ktElement = elementAt<KtProperty>("val myEnum")!!
    val foundGqlDeclarationElements = kotlinTypeDeclarationProvider.getSymbolTypeDeclarations(ktElement)!!
    assertTrue(foundGqlDeclarationElements.size > 1)

    myFixture.configureFromTempProjectFile("src/main/graphql/schema.graphqls")
    val gqlDeclarationElementInGqlFile = elementAt<GraphQLTypeNameDefinition>("MyEnum", afterText = "enum MyEnum {")!!

    assertEquals(gqlDeclarationElementInGqlFile, foundGqlDeclarationElements.first())
  }

  @Test
  fun goToInputType() {
    myFixture.configureFromTempProjectFile("src/main/kotlin/com/example/Main.kt")
    val ktElement = elementAt<KtProperty>("val personInput")!!
    val foundGqlDeclarationElements = kotlinTypeDeclarationProvider.getSymbolTypeDeclarations(ktElement)!!
    assertTrue(foundGqlDeclarationElements.size > 1)

    myFixture.configureFromTempProjectFile("src/main/graphql/schema.graphqls")
    val gqlDeclarationElementInGqlFile = elementAt<GraphQLTypeNameDefinition>("PersonInput", afterText = "input PersonInput")!!
    assertEquals(gqlDeclarationElementInGqlFile, foundGqlDeclarationElements.first())
  }
}

