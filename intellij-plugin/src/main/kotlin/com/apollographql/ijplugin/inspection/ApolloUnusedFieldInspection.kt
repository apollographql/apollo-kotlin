package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.navigation.compat.KotlinFindUsagesHandlerFactoryCompat
import com.apollographql.ijplugin.navigation.findKotlinFieldDefinitions
import com.apollographql.ijplugin.project.apolloProjectService
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.jsgraphql.psi.GraphQLField
import com.intellij.lang.jsgraphql.psi.GraphQLIdentifier
import com.intellij.lang.jsgraphql.psi.GraphQLTypedOperationDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.findParentOfType

class ApolloUnusedFieldInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    var isUnusedOperation = false
    return object : GraphQLVisitor() {
      override fun visitIdentifier(o: GraphQLIdentifier) {
        if (!o.project.apolloProjectService.apolloVersion.isAtLeastV3) return
        if (isUnusedOperation) return
        val operation = o.findParentOfType<GraphQLTypedOperationDefinition>() ?: return
        if (ApolloUnusedOperationInspection.isUnusedOperation(operation)) {
          // The whole operation is unused, no need to check the fields
          isUnusedOperation = true
          return
        }
        val field = o.parent as? GraphQLField ?: return
        val ktClasses = findKotlinFieldDefinitions(field).ifEmpty {
          // Didn't find any generated class: maybe in the middle of writing a new operation, let's not report an error yet.
          return@visitIdentifier
        }
        val kotlinFindUsagesHandlerFactory = KotlinFindUsagesHandlerFactoryCompat(o.project)
        val hasUsageProcessor = HasUsageProcessor()
        for (kotlinDefinition in ktClasses) {
          if (kotlinFindUsagesHandlerFactory.canFindUsages(kotlinDefinition)) {
            val kotlinFindUsagesHandler = kotlinFindUsagesHandlerFactory.createFindUsagesHandler(kotlinDefinition, false)
                ?: return
            val findUsageOptions = kotlinFindUsagesHandlerFactory.findPropertyOptions ?: return
            kotlinFindUsagesHandler.processElementUsages(kotlinDefinition, hasUsageProcessor, findUsageOptions)
            if (hasUsageProcessor.foundUsage) return
          }
        }
        holder.registerProblem(
            o,
            ApolloBundle.message("inspection.unusedField.reportText"),
            DeleteElementQuickFix("inspection.unusedField.quickFix") { it.parent.parent },
        )
      }
    }
  }
}
