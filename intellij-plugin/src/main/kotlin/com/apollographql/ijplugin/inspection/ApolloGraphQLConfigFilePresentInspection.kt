package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.settings.projectSettingsState
import com.apollographql.ijplugin.telemetry.TelemetryEvent
import com.apollographql.ijplugin.telemetry.telemetryService
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile

private val graphQLConfigFileNames = setOf(
    "graphql.config.json",
    "graphql.config.js",
    "graphql.config.cjs",
    "graphql.config.ts",
    "graphql.config.yaml",
    "graphql.config.yml",
    ".graphqlrc",
    ".graphqlrc.json",
    ".graphqlrc.yaml",
    ".graphqlrc.yml",
    ".graphqlrc.js",
    ".graphqlrc.ts"
)

class ApolloGraphQLConfigFilePresentInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : PsiElementVisitor() {
      override fun visitFile(file: PsiFile) {
        if (!file.project.apolloProjectService.apolloVersion.isAtLeastV4 || !file.project.projectSettingsState.contributeConfigurationToGraphqlPlugin) return
        if (file.name in graphQLConfigFileNames) {
          holder.registerProblem(file, ApolloBundle.message("inspection.graphQLConfigFilePresent.reportText"), ApolloGraphQLConfigFilePresentQuickFix(file.name))
        }
      }
    }
  }
}

private class ApolloGraphQLConfigFilePresentQuickFix(private val fileName: String) : LocalQuickFix {
  override fun getName(): String {
    return ApolloBundle.message("inspection.graphQLConfigFilePresent.quickFix", fileName)
  }

  override fun getFamilyName() = name

  override fun availableInBatchMode() = false

  override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo = IntentionPreviewInfo.EMPTY

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    if (!IntentionPreviewUtils.isIntentionPreviewActive()) project.telemetryService.logEvent(TelemetryEvent.ApolloIjGraphQLConfigFilePresentQuickFix())
    val psiFile = descriptor.psiElement.containingFile
    psiFile.virtualFile.delete(this)
  }
}

class ApolloGraphQLConfigFilePresentAnnotator : Annotator {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element !is PsiFile) return
    if (!element.project.apolloProjectService.apolloVersion.isAtLeastV4 || !element.project.projectSettingsState.contributeConfigurationToGraphqlPlugin) return
    if (element.containingFile.name in graphQLConfigFileNames) {
      holder.newAnnotation(HighlightSeverity.WARNING, ApolloBundle.message("inspection.graphQLConfigFilePresent.reportText"))
          .range(element)
          .create()
    }
  }
}

class GraphQLConfigFileFilter : Condition<VirtualFile> {
  override fun value(virtualFile: VirtualFile): Boolean {
    return virtualFile.name in graphQLConfigFileNames
  }
}
