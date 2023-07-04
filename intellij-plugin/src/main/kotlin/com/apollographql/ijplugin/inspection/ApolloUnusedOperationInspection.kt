package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.navigation.compat.KotlinFindUsagesHandlerFactoryCompat
import com.apollographql.ijplugin.navigation.findKotlinOperationDefinitions
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.util.isGenerated
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.jsgraphql.psi.GraphQLTypedOperationDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLVisitor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor

class ApolloUnusedOperationInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : GraphQLVisitor() {
      override fun visitTypedOperationDefinition(o: GraphQLTypedOperationDefinition) {
        if (!o.project.apolloProjectService.apolloVersion.isAtLeastV3) return
        val ktClasses = findKotlinOperationDefinitions(o).ifEmpty {
          // Didn't find any generated class: maybe in the middle of writing a new operation, let's not report an error yet.
          return@visitTypedOperationDefinition
        }
        val kotlinFindUsagesHandlerFactory = KotlinFindUsagesHandlerFactoryCompat(o.project)
        val hasUsageProcessor = HasUsageProcessor()
        for (kotlinDefinition in ktClasses) {
          if (kotlinFindUsagesHandlerFactory.canFindUsages(kotlinDefinition)) {
            val kotlinFindUsagesHandler = kotlinFindUsagesHandlerFactory.createFindUsagesHandler(kotlinDefinition, false)
                ?: return
            val findUsageOptions = kotlinFindUsagesHandlerFactory.findClassOptions ?: return
            kotlinFindUsagesHandler.processElementUsages(kotlinDefinition, hasUsageProcessor, findUsageOptions)
            if (hasUsageProcessor.foundUsage) return
          }
        }
        holder.registerProblem(o, ApolloBundle.message("inspection.unusedOperation.reportText"), DeleteElementQuickFix("inspection.unusedOperation.quickFix"))
      }
    }
  }

  private class HasUsageProcessor : Processor<UsageInfo> {
    var foundUsage = false
      private set

    override fun process(usageInfo: UsageInfo): Boolean {
      if (usageInfo.virtualFile?.isGenerated(usageInfo.project) == false) {
        foundUsage = true
        return false
      }
      return true
    }
  }

  private class DeleteElementQuickFix(private val label: String) : LocalQuickFix {
    override fun getName() = ApolloBundle.message(label)

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      descriptor.psiElement.delete()
    }
  }
}
