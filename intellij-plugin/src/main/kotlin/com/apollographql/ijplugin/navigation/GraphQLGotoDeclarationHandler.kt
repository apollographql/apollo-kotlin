package com.apollographql.ijplugin.navigation

import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.telemetry.TelemetryEvent
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.lang.jsgraphql.psi.GraphQLElement
import com.intellij.lang.jsgraphql.psi.GraphQLEnumTypeDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLEnumValue
import com.intellij.lang.jsgraphql.psi.GraphQLField
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentSpread
import com.intellij.lang.jsgraphql.psi.GraphQLInputObjectTypeDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLInputValueDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLTypeNameDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLTypedOperationDefinition
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.options.advanced.AdvancedSettings
import com.intellij.psi.PsiElement

/**
 * Allows to navigate to the corresponding Kotlin generated code when middle-clicking/cmd-clicking/cmd-b on GraphQL elements:
 * - operation
 * - fragment
 * - field
 * - enum type/value
 * - input type/field
 */
class GraphQLGotoDeclarationHandler : GotoDeclarationHandler {
  override fun getGotoDeclarationTargets(sourceElement: PsiElement?, offset: Int, editor: Editor?): Array<PsiElement>? {
    val gqlElement = sourceElement?.parent?.parent as? GraphQLElement ?: return null
    if (!gqlElement.project.apolloProjectService.apolloVersion.isAtLeastV3) return null
    val enabledInAdvancedSettings = AdvancedSettings.getBoolean("apollo.graphQLGoToDeclarationGeneratedCode")

    val kotlinDefinitions = when (gqlElement) {
      is GraphQLTypedOperationDefinition -> {
        findKotlinOperationDefinitions(gqlElement)
      }

      is GraphQLFragmentDefinition -> {
        findKotlinFragmentClassDefinitions(gqlElement)
      }

      is GraphQLFragmentSpread -> {
        if (!enabledInAdvancedSettings) return null
        findKotlinFragmentClassDefinitions(gqlElement)
      }

      is GraphQLField -> {
        if (!enabledInAdvancedSettings) return null
        findKotlinFieldDefinitions(gqlElement)
      }

      is GraphQLTypeNameDefinition -> {
        if (!enabledInAdvancedSettings) return null
        when (val parent = gqlElement.parent) {
          is GraphQLEnumTypeDefinition -> findKotlinEnumClassDefinitions(parent)
          is GraphQLInputObjectTypeDefinition -> findKotlinInputClassDefinitions(parent)
          else -> return null
        }
      }

      is GraphQLEnumValue -> {
        if (!enabledInAdvancedSettings) return null
        findKotlinEnumValueDefinitions(gqlElement)
      }

      is GraphQLInputValueDefinition -> {
        if (!enabledInAdvancedSettings) return null
        findKotlinInputFieldDefinitions(gqlElement)
      }

      else -> return null
    }

    return buildList {
      // Add the original referred to element
      val resolvedElement = sourceElement.parent?.reference?.resolve()
      if (resolvedElement != null) {
        add(resolvedElement)
      } else {
        // Special case for Fragment declaration: we switch to the Fragment's usages
        if (gqlElement is GraphQLFragmentDefinition) {
          addAll(findFragmentSpreads(gqlElement.project) { it.nameIdentifier.reference?.resolve() == gqlElement.nameIdentifier })
        }
      }

      // Add Kotlin definition(s)
      addAll(kotlinDefinitions.map { it.logNavigation { TelemetryEvent.ApolloIjNavigateToKotlin() } })
    }.toTypedArray()
  }
}
