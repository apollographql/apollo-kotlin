package com.apollographql.ijplugin.navigation

import com.apollographql.ijplugin.ApolloTestCase
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLTypedOperationDefinition
import com.intellij.psi.PsiElement
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4


@RunWith(JUnit4::class)
class KotlinDefinitionMarkerProviderTest : ApolloTestCase() {

  private lateinit var infoList: MutableList<LineMarkerInfo<*>>

  override fun setUp() {
    super.setUp()
    myFixture.configureFromTempProjectFile("src/main/kotlin/com/example/Markers.kt")
    doHighlighting()
    infoList = DaemonCodeAnalyzerImpl.getLineMarkers(editor.document, project)
  }

  @Test
  fun operationMarker() {
    val (operationSource, operationTarget) = infoList[2].sourceAndTarget()
    operationSource.assertTypeAndText<PsiElement>("AliasedAnimalsQuery")
    operationTarget.assertTypeAndText<GraphQLTypedOperationDefinition>("query animals {")
  }

  @Test
  fun fragmentMarker() {
    val (fragmentSource, fragmentTarget) = infoList[1].sourceAndTarget()
    fragmentSource.assertTypeAndText<PsiElement>("ScreenFields")
    fragmentTarget.assertTypeAndText<GraphQLFragmentDefinition>("fragment ScreenFields")
  }


  private fun LineMarkerInfo<*>.sourceAndTarget(): Pair<PsiElement, PsiElement> {
    val markerInfo = this as RelatedItemLineMarkerInfo
    val sourceElement = markerInfo.element!!
    val targetElement = markerInfo.createGotoRelatedItems().first().element!!
    return sourceElement to targetElement
  }
}

