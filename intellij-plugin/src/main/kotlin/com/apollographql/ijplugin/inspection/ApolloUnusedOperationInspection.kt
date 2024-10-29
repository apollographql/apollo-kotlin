package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.navigation.compat.KotlinFindUsagesHandlerFactoryCompat
import com.apollographql.ijplugin.navigation.findKotlinOperationDefinitions
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.telemetry.TelemetryEvent
import com.apollographql.ijplugin.util.isProcessCanceled
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.jsgraphql.psi.GraphQLIdentifier
import com.intellij.lang.jsgraphql.psi.GraphQLTypedOperationDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.childrenOfType

class ApolloUnusedOperationInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : GraphQLVisitor() {
      override fun visitTypedOperationDefinition(o: GraphQLTypedOperationDefinition) {
        if (isUnusedOperation(o)) {
          val nameElement = o.childrenOfType<GraphQLIdentifier>().firstOrNull() ?: return
          holder.registerProblem(
              nameElement,
              ApolloBundle.message("inspection.unusedOperation.reportText"),
              DeleteElementQuickFix(label = "inspection.unusedOperation.quickFix", telemetryEvent = { TelemetryEvent.ApolloIjUnusedOperationQuickFix() }) { it.parent }
          )
        }
      }
    }
  }
}

fun isUnusedOperation(operationDefinition: GraphQLTypedOperationDefinition): Boolean {
  if (isProcessCanceled()) return false
  if (!operationDefinition.project.apolloProjectService.apolloVersion.isAtLeastV3) return false
  val ktClasses = findKotlinOperationDefinitions(operationDefinition).ifEmpty {
    // Didn't find any generated class: maybe in the middle of writing a new operation, let's not report an error yet.
    return false
  }
  val kotlinFindUsagesHandlerFactory = KotlinFindUsagesHandlerFactoryCompat(operationDefinition.project)
  val hasUsageProcessor = HasUsageProcessor()
  for (kotlinDefinition in ktClasses) {
    if (kotlinFindUsagesHandlerFactory.canFindUsages(kotlinDefinition)) {
      val kotlinFindUsagesHandler = kotlinFindUsagesHandlerFactory.createFindUsagesHandler(kotlinDefinition, false)
          ?: return false
      val findUsageOptions = kotlinFindUsagesHandlerFactory.findClassOptions ?: return false
      kotlinFindUsagesHandler.processElementUsages(kotlinDefinition, hasUsageProcessor, findUsageOptions)
      if (hasUsageProcessor.foundUsage) return false
    }
  }
  return true
}
