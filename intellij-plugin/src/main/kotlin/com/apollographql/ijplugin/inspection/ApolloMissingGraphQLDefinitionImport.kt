package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.util.findPsiFileByPath
import com.apollographql.ijplugin.util.quoted
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.jsgraphql.ide.config.GraphQLConfigProvider
import com.intellij.lang.jsgraphql.psi.GraphQLArrayValue
import com.intellij.lang.jsgraphql.psi.GraphQLDirective
import com.intellij.lang.jsgraphql.psi.GraphQLFile
import com.intellij.lang.jsgraphql.psi.GraphQLNamedElement
import com.intellij.lang.jsgraphql.psi.GraphQLSchemaDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLSchemaExtension
import com.intellij.lang.jsgraphql.psi.GraphQLVisitor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor

private val DIRECTIVES_TO_CHECK = setOf("semanticNonNull", "catch", "ignoreErrors")

class ApolloMissingGraphQLDefinitionImport : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : GraphQLVisitor() {
      override fun visitDirective(o: GraphQLDirective) {
        super.visitDirective(o)
        if (!o.project.apolloProjectService.apolloVersion.isAtLeastV4) return
        if (o.name !in DIRECTIVES_TO_CHECK) return
        if (!o.isImported()) {
          holder.registerProblem(o, ApolloBundle.message("inspection.missingGraphQLDefinitionImport.reportText", ApolloBundle.message("inspection.missingGraphQLDefinitionImport.reportText.directive")), ImportDefinitionQuickFix(ApolloBundle.message("inspection.missingGraphQLDefinitionImport.reportText.directive")))
        }
      }
    }
  }
}

private fun GraphQLNamedElement.isImported(): Boolean {
  val containingFile = containingFile ?: return true
  val projectConfig = GraphQLConfigProvider.getInstance(project).resolveProjectConfig(containingFile) ?: return true
  val schemaFiles = projectConfig.schema.mapNotNull { schemaPointer ->
    schemaPointer.outputPath?.let { path -> project.findPsiFileByPath(path) } as? GraphQLFile
  }
  for (schemaFile in schemaFiles) {
    if (schemaFile.hasImportFor(this)) return true
  }
  return false
}

private fun GraphQLFile.hasImportFor(element: GraphQLNamedElement): Boolean {
  val schemaDirectives = typeDefinitions.filterIsInstance<GraphQLSchemaExtension>().flatMap { it.directives } +
      typeDefinitions.filterIsInstance<GraphQLSchemaDefinition>().flatMap { it.directives }
  for (directive in schemaDirectives) {
    if (directive.name != "link") continue
    val importArgValue = directive.arguments?.argumentList.orEmpty().firstOrNull { it.name == "import" }?.value as? GraphQLArrayValue
        ?: continue
    val name = if (element is GraphQLDirective) "@" + element.name!! else element.name!!
    if (importArgValue.valueList.any { it.text == name.quoted() }) {
      return true
    }
  }
  return false
}

private class ImportDefinitionQuickFix(val typeName: String) : LocalQuickFix {
  override fun getName() = ApolloBundle.message("inspection.missingGraphQLDefinitionImport.quickFix", typeName)
  override fun getFamilyName() = name

  override fun availableInBatchMode() = false
  override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo = IntentionPreviewInfo.EMPTY

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    //TODO
    //if (!IntentionPreviewUtils.isIntentionPreviewActive()) project.telemetryService.logEvent(TelemetryEvent.ApolloIjUnusedFieldIgnoreFieldQuickFix())

    // - for all schema files
    // - find the first schema file that has a link import
    // - add the missing import
    // - if no schema file has a link import
    // - find the first schema file named 'extra.graphqls'
    // - if no schema file is named 'extra.graphqls' create one
    // - add the missing import to 'extra.graphqls'
  }
}
