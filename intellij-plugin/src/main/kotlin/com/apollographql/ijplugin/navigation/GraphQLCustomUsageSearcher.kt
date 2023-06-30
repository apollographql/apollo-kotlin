package com.apollographql.ijplugin.navigation

import com.apollographql.ijplugin.navigation.compat.KotlinFindUsagesHandlerFactoryCompat
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.util.isGenerated
import com.intellij.find.findUsages.CustomUsageSearcher
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.lang.jsgraphql.psi.GraphQLElement
import com.intellij.lang.jsgraphql.psi.GraphQLEnumTypeDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLEnumValue
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLInputObjectTypeDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLInputValueDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLTypeNameDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLTypedOperationDefinition
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageInfo
import com.intellij.usages.Usage
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.util.Processor

/**
 * Allows to find usages of the corresponding Kotlin generated code when invoking 'Find Usages' on GraphQL elements:
 * - operation definition
 * - fragment definition
 * - enum type/value
 * - input type/field
 *
 * TODO: operation/fragment field is missing for now because the GraphQL PSI doesn't provide the necessary information
 */
class GraphQLCustomUsageSearcher : CustomUsageSearcher() {
  override fun processElementUsages(element: PsiElement, processor: Processor<in Usage>, options: FindUsagesOptions) {
    if (element !is GraphQLElement) return

    runReadAction {
      if (!element.project.apolloProjectService.apolloVersion.isAtLeastV3) return@runReadAction

      var isProperty = false
      val kotlinDefinitions = when (val parent = element.parent) {
        is GraphQLTypedOperationDefinition -> {
          findKotlinOperationDefinitions(parent)
        }

        is GraphQLFragmentDefinition -> {
          findKotlinFragmentClassDefinitions(parent)
        }

        is GraphQLTypeNameDefinition -> {
          when (val grandParent = parent.parent) {
            is GraphQLEnumTypeDefinition -> findKotlinEnumClassDefinitions(grandParent)
            is GraphQLInputObjectTypeDefinition -> findKotlinInputClassDefinitions(grandParent)
            else -> emptyList()
          }
        }

        is GraphQLEnumValue -> {
          findKotlinEnumValueDefinitions(parent)
        }

        is GraphQLInputValueDefinition -> {
          isProperty = true
          findKotlinInputFieldDefinitions(parent)
        }

        else -> emptyList()

      }.ifEmpty { return@runReadAction }
      val kotlinFindUsagesHandlerFactory = KotlinFindUsagesHandlerFactoryCompat(element.project)
      val ignoreGeneratedFilesProcessor = IgnoreGeneratedFilesProcessor(processor)
      for (kotlinDefinition in kotlinDefinitions) {
        if (kotlinFindUsagesHandlerFactory.canFindUsages(kotlinDefinition)) {
          val kotlinFindUsagesHandler = kotlinFindUsagesHandlerFactory.createFindUsagesHandler(kotlinDefinition, false)
              ?: return@runReadAction
          val findUsageOptions = if (isProperty) {
            kotlinFindUsagesHandlerFactory.findPropertyOptions
          } else {
            kotlinFindUsagesHandlerFactory.findClassOptions
          } ?: return@runReadAction
          kotlinFindUsagesHandler.processElementUsages(kotlinDefinition, ignoreGeneratedFilesProcessor, findUsageOptions)
        }
      }
    }
  }
}

private class IgnoreGeneratedFilesProcessor(private val processor: Processor<in Usage>) : Processor<UsageInfo> {
  override fun process(usageInfo: UsageInfo): Boolean {
    if (usageInfo.virtualFile?.isGenerated(usageInfo.project) != true) {
      processor.process(UsageInfo2UsageAdapter(usageInfo))
    }
    return true
  }
}
