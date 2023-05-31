package com.apollographql.ijplugin

import com.apollographql.ijplugin.navigation.KotlinGotoDeclarationHandler
import com.apollographql.ijplugin.util.findChildrenOfType
import com.intellij.lang.jsgraphql.psi.GraphQLTypedOperationDefinition
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.psi.PsiElement
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KotlinGoToDeclarationHandlerTest : ApolloTestCase() {
  override val mavenLibraries = listOf(
      "com.apollographql.apollo3:apollo-annotations-jvm:4.0.0-alpha.1",
      "com.apollographql.apollo3:apollo-api-jvm:4.0.0-alpha.1",
      "com.apollographql.apollo3:apollo-mpp-utils-jvm:4.0.0-alpha.1",
      "com.apollographql.apollo3:apollo-runtime-jvm:4.0.0-alpha.1",
  )

  private val kotlinGoToDeclarationHandler = KotlinGotoDeclarationHandler()

  @Test
  fun goToOperationDefinition() {
    myFixture.configureFromTempProjectFile("src/main/kotlin/com/example/Main.kt")
    val ktElement = elementAt<PsiElement>("AnimalsQuery()")!!
    val foundGqlDeclarationElements = kotlinGoToDeclarationHandler.getGotoDeclarationTargets(ktElement, 0, editor)!!

    myFixture.configureFromTempProjectFile("src/main/graphql/AnimalsQuery.graphql")
    val gqlDeclarationElementInGqlFile = elementAt<GraphQLTypedOperationDefinition>("query Animals")!!

    assert(foundGqlDeclarationElements.contains(gqlDeclarationElementInGqlFile))
  }

  @Test
  fun goToFragmentDefinition() {
    // TODO
  }


}

