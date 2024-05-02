package com.apollographql.ijplugin.navigation

import com.apollographql.ijplugin.ApolloTestCase
import com.apollographql.ijplugin.util.resolveKtName
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLIdentifier
import com.intellij.lang.jsgraphql.psi.GraphQLTypeNameDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLTypedOperationDefinition
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.Callable

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
    // XXX TypeDeclarationProvider.getSymbolTypeDeclarations() throws KtInaccessibleLifetimeOwnerAccessException when called from the EDT
    val foundGqlDeclarationElements = ApplicationManager.getApplication().executeOnPooledThread(Callable {
      runReadAction {
        kotlinTypeDeclarationProvider.getSymbolTypeDeclarations(ktElement)!!
      }
    }).get()

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
      toElement = { elementAt<GraphQLTypedOperationDefinition>("query animals") }
  )

  @Test
  fun goToFragmentDefinition() = testNavigation(
      fromFile = "src/main/kotlin/com/example/Main.kt",
      fromElement = { elementAt<KtProperty>("computerFields") },
      toFile = "src/main/graphql/fragments/ComputerFields.graphql",
      toElement = { elementAt<GraphQLFragmentDefinition>("fragment computerFields") }
  )

  @Test
  fun goToField() = testNavigation(
      fromFile = "src/main/kotlin/com/example/Main.kt",
      fromElement = { elementAt<KtReferenceExpression>("fieldOnDogAndCat")!!.resolveKtName() },
      toFile = "src/main/graphql/schema.graphqls",
      toElement = { elementAt<GraphQLIdentifier>("fieldOnDogAndCat", afterText = "type dog implements Animal {") }
  )

  @Test
  fun goToEnumType() = testNavigation(
      fromFile = "src/main/kotlin/com/example/Main.kt",
      fromElement = { elementAt<KtProperty>("val myEnum") },
      toFile = "src/main/graphql/schema.graphqls",
      toElement = { elementAt<GraphQLTypeNameDefinition>("myEnum", afterText = "enum myEnum {") }
  )

  @Test
  fun goToInputType() = testNavigation(
      fromFile = "src/main/kotlin/com/example/Main.kt",
      fromElement = { elementAt<KtProperty>("val personInput") },
      toFile = "src/main/graphql/schema.graphqls",
      toElement = { elementAt<GraphQLTypeNameDefinition>("personInput", afterText = "input personInput") }
  )
}

