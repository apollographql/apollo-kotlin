package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.navigation.compat.KotlinFindUsagesHandlerFactoryCompat
import com.apollographql.ijplugin.navigation.findKotlinFieldDefinitions
import com.apollographql.ijplugin.navigation.findKotlinFragmentSpreadDefinitions
import com.apollographql.ijplugin.navigation.findKotlinInlineFragmentDefinitions
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.util.isProcessCanceled
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.jsgraphql.psi.GraphQLField
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentSpread
import com.intellij.lang.jsgraphql.psi.GraphQLIdentifier
import com.intellij.lang.jsgraphql.psi.GraphQLInlineFragment
import com.intellij.lang.jsgraphql.psi.GraphQLSelection
import com.intellij.lang.jsgraphql.psi.GraphQLTypeName
import com.intellij.lang.jsgraphql.psi.GraphQLTypedOperationDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.findParentOfType

class ApolloUnusedFieldInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    var isUnusedOperation = false
    return object : GraphQLVisitor() {
      override fun visitIdentifier(o: GraphQLIdentifier) {
        if (isProcessCanceled()) return
        if (!o.project.apolloProjectService.apolloVersion.isAtLeastV3) return
        if (isUnusedOperation) return
        val operation = o.findParentOfType<GraphQLTypedOperationDefinition>()
        if (operation != null && ApolloUnusedOperationInspection.isUnusedOperation(operation)) {
          // The whole operation is unused, no need to check the fields
          isUnusedOperation = true
          return
        }

        var isFragment = false
        val ktDefinitions  = when (val parent = o.parent) {
          is GraphQLField -> findKotlinFieldDefinitions(parent)
          is GraphQLFragmentSpread -> {
            isFragment = true
            findKotlinFragmentSpreadDefinitions(parent)
          }
          is GraphQLTypeName -> {
            val inlineFragment = parent.parent?.parent as? GraphQLInlineFragment ?: return
            isFragment = true
            findKotlinInlineFragmentDefinitions(inlineFragment)
          }
          else -> return
        }.ifEmpty { return }

        val kotlinFindUsagesHandlerFactory = KotlinFindUsagesHandlerFactoryCompat(o.project)
        val hasUsageProcessor = HasUsageProcessor()
        for (kotlinDefinition in ktDefinitions) {
          if (kotlinFindUsagesHandlerFactory.canFindUsages(kotlinDefinition)) {
            val kotlinFindUsagesHandler = kotlinFindUsagesHandlerFactory.createFindUsagesHandler(kotlinDefinition, false)
                ?: return
            val findUsageOptions = kotlinFindUsagesHandlerFactory.findPropertyOptions ?: return
            kotlinFindUsagesHandler.processElementUsages(kotlinDefinition, hasUsageProcessor, findUsageOptions)
            if (hasUsageProcessor.foundUsage) return
          }
        }
        holder.registerProblem(
            if (isFragment) o.findParentOfType<GraphQLSelection>()!! else o,
            ApolloBundle.message("inspection.unusedField.reportText"),
            DeleteElementQuickFix("inspection.unusedField.quickFix") { it.findParentOfType<GraphQLSelection>(strict = false)!! },
        )
      }
    }
  }
}
