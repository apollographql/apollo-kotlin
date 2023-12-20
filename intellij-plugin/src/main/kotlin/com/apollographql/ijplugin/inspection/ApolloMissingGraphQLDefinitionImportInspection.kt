package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.gradle.gradleToolingModelService
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.telemetry.TelemetryEvent
import com.apollographql.ijplugin.telemetry.telemetryService
import com.apollographql.ijplugin.util.cast
import com.apollographql.ijplugin.util.findChildrenOfType
import com.apollographql.ijplugin.util.quoted
import com.apollographql.ijplugin.util.unquoted
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.jsgraphql.psi.GraphQLArrayValue
import com.intellij.lang.jsgraphql.psi.GraphQLDirective
import com.intellij.lang.jsgraphql.psi.GraphQLElementFactory
import com.intellij.lang.jsgraphql.psi.GraphQLEnumValue
import com.intellij.lang.jsgraphql.psi.GraphQLFile
import com.intellij.lang.jsgraphql.psi.GraphQLSchemaDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLSchemaExtension
import com.intellij.lang.jsgraphql.psi.GraphQLVisitor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.parentOfType

class ApolloMissingGraphQLDefinitionImportInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : GraphQLVisitor() {
      override fun visitDirective(o: GraphQLDirective) {
        super.visitDirective(o)
        if (!o.project.apolloProjectService.apolloVersion.isAtLeastV4) return
        if (o.name !in NULLABILITY_DIRECTIVES.keys) return
        if (!o.isImported()) {
          holder.registerProblem(o, ApolloBundle.message("inspection.missingGraphQLDefinitionImport.reportText", ApolloBundle.message("inspection.missingGraphQLDefinitionImport.reportText.directive")), ImportDefinitionQuickFix(ApolloBundle.message("inspection.missingGraphQLDefinitionImport.reportText.directive")))
        } else if (o.isCatchAndCatchToNotImported()) {
          holder.registerProblem(o.argumentValue("to")!!, ApolloBundle.message("inspection.missingGraphQLDefinitionImport.reportText", ApolloBundle.message("inspection.missingGraphQLDefinitionImport.reportText.enum")), ImportDefinitionQuickFix(ApolloBundle.message("inspection.missingGraphQLDefinitionImport.reportText.enum")))
        }
      }
    }
  }
}

private fun GraphQLDirective.isCatchAndCatchToNotImported(): Boolean {
  if (name != "catch") return false
  if (argumentValue("to") as? GraphQLEnumValue == null) return false
  return !isEnumImported(this, "CatchTo")
}

private class ImportDefinitionQuickFix(val typeName: String) : LocalQuickFix {
  override fun getName() = ApolloBundle.message("inspection.missingGraphQLDefinitionImport.quickFix", typeName)
  override fun getFamilyName() = name

  override fun availableInBatchMode() = false
  override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo = IntentionPreviewInfo.EMPTY

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    if (!IntentionPreviewUtils.isIntentionPreviewActive()) project.telemetryService.logEvent(TelemetryEvent.ApolloIjMissingGraphQLDefinitionImportQuickFix())

    val element = descriptor.psiElement.parentOfType<GraphQLDirective>(withSelf = true)!!
    val schemaFiles = element.schemaFiles()
    val linkDirective = schemaFiles.flatMap { it.linkDirectives() }.firstOrNull()

    // Also add a @catch directive to the schema if we're importing @catch
    val catchDirectiveSchemaExtension = if (element.name == "catch" && schemaFiles.flatMap { it.schemaCatchDirectives() }.isEmpty()) {
      createCatchDirectiveSchemaExtension(project)
    } else {
      null
    }

    if (linkDirective == null) {
      val linkDirectiveSchemaExtension = createLinkDirectiveSchemaExtension(project, setOf(element.nameForImport))
      val extraSchemaFile = schemaFiles.firstOrNull { it.name == "extra.graphqls" }
      if (extraSchemaFile == null) {
        val fileText = linkDirectiveSchemaExtension.text + catchDirectiveSchemaExtension?.let { "\n\n" + it.text }.orEmpty()
        GraphQLElementFactory.createFile(project, fileText).also {
          // Save the file to the project
          it.name = "extra.graphqls"
          schemaFiles.first().containingDirectory!!.add(it)

          // There's a new schema file, reload the configuration
          project.gradleToolingModelService.triggerFetchToolingModels()
        }
      } else {
        extraSchemaFile.add(linkDirectiveSchemaExtension)
        catchDirectiveSchemaExtension?.let {
          extraSchemaFile.add(GraphQLElementFactory.createWhiteSpace(project, "\n\n"))
          extraSchemaFile.add(it)
        }
      }
    } else {
      val extraSchemaFile = linkDirective.containingFile
      val importedNames = buildSet {
        addAll(linkDirective.arguments!!.argumentList.firstOrNull { it.name == "import" }?.value?.cast<GraphQLArrayValue>()?.valueList.orEmpty().map { it.text.unquoted() })
        add(element.nameForImport)
      }
      linkDirective.replace(createLinkDirective(project, importedNames))
      catchDirectiveSchemaExtension?.let {
        extraSchemaFile.add(GraphQLElementFactory.createWhiteSpace(project, "\n\n"))
        extraSchemaFile.add(it)
      }
    }
  }
}

private fun createLinkDirectiveSchemaExtension(project: Project, importedNames: Set<String>): GraphQLSchemaExtension {
  val names = if ("@catch" in importedNames) importedNames + "CatchTo" else importedNames
  return GraphQLElementFactory.createFile(
      project,
      """
        extend schema
        @link(
          url: "$NULLABILITY_URL",
          import: [${names.joinToString { it.quoted() }}]
        )
      """.trimIndent()
  )
      .findChildrenOfType<GraphQLSchemaExtension>().single()
}

private fun createCatchDirectiveSchemaExtension(project: Project): GraphQLSchemaExtension {
  return GraphQLElementFactory.createFile(project, "extend schema @catch(to: THROW)")
      .findChildrenOfType<GraphQLSchemaExtension>().single()
}

private fun createLinkDirective(project: Project, importedNames: Set<String>): GraphQLDirective {
  return createLinkDirectiveSchemaExtension(project, importedNames).directives.single()
}

private fun GraphQLFile.schemaCatchDirectives(): List<GraphQLDirective> {
  val schemaDirectives = typeDefinitions.filterIsInstance<GraphQLSchemaExtension>().flatMap { it.directives } +
      typeDefinitions.filterIsInstance<GraphQLSchemaDefinition>().flatMap { it.directives }
  return schemaDirectives.filter { it.name == "catch" }
}
