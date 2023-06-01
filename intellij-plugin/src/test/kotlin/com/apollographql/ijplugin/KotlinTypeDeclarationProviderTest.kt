package com.apollographql.ijplugin

import com.apollographql.ijplugin.navigation.KotlinTypeDeclarationProvider
import com.apollographql.ijplugin.util.resolveKtName
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLIdentifier
import com.intellij.lang.jsgraphql.psi.GraphQLTypeNameDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLTypedOperationDefinition
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KotlinTypeDeclarationProviderTest : ApolloTestCase() {
  private val kotlinTypeDeclarationProvider = KotlinTypeDeclarationProvider()

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

    // Open the file
    myFixture.configureFromTempProjectFile(fromFile)
    // Find the element to navigate from
    val ktElement = fromElement()!!

    // Simulate navigation
    val foundGqlDeclarationElements = kotlinTypeDeclarationProvider.getSymbolTypeDeclarations(ktElement)!!
    // We want our target (gql), but also the original targets (Kotlin)
    assertTrue(foundGqlDeclarationElements.size > 1)

    // Make sure they're the same
    assertEquals(foundGqlDeclarationElements.first(), gqlDeclarationElementInGqlFile)
  }

  @Test
  fun goToOperationDefinition() = testNavigation(
      fromFile = "src/main/kotlin/com/example/Main.kt",
      fromElement = { elementAt<KtProperty>("animalsQuery") },
      toFile = "src/main/graphql/AnimalsQuery.graphql",
      toElement = { elementAt<GraphQLTypedOperationDefinition>("query Animals") }
  )

  @Test
  fun goToFragmentDefinition() = testNavigation(
      fromFile = "src/main/kotlin/com/example/Main.kt",
      fromElement = { elementAt<KtProperty>("computerFields") },
      toFile = "src/main/graphql/ComputerFields.graphql",
      toElement = { elementAt<GraphQLFragmentDefinition>("fragment ComputerFields") }
  )

  @Test
  fun goToField() = testNavigation(
      fromFile = "src/main/kotlin/com/example/Main.kt",
      fromElement = { elementAt<KtReferenceExpression>("fieldOnDogAndCat")!!.resolveKtName() },
      toFile = "src/main/graphql/schema.graphqls",
      toElement = { elementAt<GraphQLIdentifier>("fieldOnDogAndCat", afterText = "type Dog implements Animal {") }
  )

  @Test
  fun goToEnumType() = testNavigation(
      fromFile = "src/main/kotlin/com/example/Main.kt",
      fromElement = { elementAt<KtProperty>("val myEnum") },
      toFile = "src/main/graphql/schema.graphqls",
      toElement = { elementAt<GraphQLTypeNameDefinition>("MyEnum", afterText = "enum MyEnum {") }
  )

  @Test
  fun goToInputType() = testNavigation(
      fromFile = "src/main/kotlin/com/example/Main.kt",
      fromElement = { elementAt<KtProperty>("val personInput") },
      toFile = "src/main/graphql/schema.graphqls",
      toElement = { elementAt<GraphQLTypeNameDefinition>("PersonInput", afterText = "input PersonInput") }
  )
}

