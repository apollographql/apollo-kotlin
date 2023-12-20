package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.gradle.gradleToolingModelService
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.telemetry.TelemetryEvent
import com.apollographql.ijplugin.telemetry.telemetryService
import com.apollographql.ijplugin.util.cast
import com.apollographql.ijplugin.util.findChildrenOfType
import com.apollographql.ijplugin.util.findPsiFileByUrl
import com.apollographql.ijplugin.util.quoted
import com.apollographql.ijplugin.util.unquoted
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.jsgraphql.ide.config.GraphQLConfigProvider
import com.intellij.lang.jsgraphql.psi.GraphQLArrayValue
import com.intellij.lang.jsgraphql.psi.GraphQLDirective
import com.intellij.lang.jsgraphql.psi.GraphQLElement
import com.intellij.lang.jsgraphql.psi.GraphQLElementFactory
import com.intellij.lang.jsgraphql.psi.GraphQLFile
import com.intellij.lang.jsgraphql.psi.GraphQLNamedElement
import com.intellij.lang.jsgraphql.psi.GraphQLSchemaDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLSchemaExtension
import com.intellij.lang.jsgraphql.psi.GraphQLVisitor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor

private const val URL_NULLABILITY = "https://specs.apollo.dev/nullability/v0.1"
private val DIRECTIVES_TO_CHECK = setOf("semanticNonNull", "catch", "ignoreErrors")

class ApolloMissingGraphQLDefinitionImportInspection : LocalInspectionTool() {
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

private fun GraphQLElement.schemaFiles(): List<GraphQLFile> {
  val containingFile = containingFile ?: return emptyList()
  val projectConfig = GraphQLConfigProvider.getInstance(project).resolveProjectConfig(containingFile) ?: return emptyList()
//  return projectConfig.schema.mapNotNull { schemaPointer ->
//    schemaPointer.outputPath?.let { path -> project.findPsiFileByPath(path) } as? GraphQLFile
//  }

  return projectConfig.schema.mapNotNull { schema ->
    schema.filePath?.let { path -> project.findPsiFileByUrl(schema.dir.url + "/" + path) } as? GraphQLFile
  }
}

private fun GraphQLNamedElement.isImported(): Boolean {
  for (schemaFile in schemaFiles()) {
    if (schemaFile.hasImportFor(this)) return true
  }
  return false
}

private fun GraphQLFile.linkDirectives(): List<GraphQLDirective> {
  val schemaDirectives = typeDefinitions.filterIsInstance<GraphQLSchemaExtension>().flatMap { it.directives } +
      typeDefinitions.filterIsInstance<GraphQLSchemaDefinition>().flatMap { it.directives }
  return schemaDirectives.filter {
    it.name == "link" &&
        it.arguments?.argumentList.orEmpty().any { it.name == "url" && it.value?.text == URL_NULLABILITY.quoted() }
  }
}

private fun GraphQLFile.hasImportFor(element: GraphQLNamedElement): Boolean {
  for (directive in linkDirectives()) {
    val importArgValue = directive.arguments?.argumentList.orEmpty().firstOrNull { it.name == "import" }?.value as? GraphQLArrayValue
        ?: continue
    if (importArgValue.valueList.any { it.text == element.nameForImport.quoted() }) {
      return true
    }
  }
  return false
}

private val GraphQLNamedElement.nameForImport get() = if (this is GraphQLDirective) "@" + name!! else name!!

private class ImportDefinitionQuickFix(val typeName: String) : LocalQuickFix {
  override fun getName() = ApolloBundle.message("inspection.missingGraphQLDefinitionImport.quickFix", typeName)
  override fun getFamilyName() = name

  override fun availableInBatchMode() = false
  override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo = IntentionPreviewInfo.EMPTY

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    if (!IntentionPreviewUtils.isIntentionPreviewActive()) project.telemetryService.logEvent(TelemetryEvent.ApolloIjMissingGraphQLDefinitionImportQuickFix())

    val element = descriptor.psiElement as GraphQLNamedElement
    val schemaFiles = element.schemaFiles()
    val linkDirective = schemaFiles.flatMap { it.linkDirectives() }.firstOrNull()

    // Also add a @catch directive to the schema if we're importing @catch
    val catchDirectiveSchemaExtension = if (element.name == "catch" && schemaFiles.flatMap { it.schemaCatchDirectives() }.isEmpty()) {
      createCatchDirectiveSchemaExtension(project)
    } else {
      null
    }

    if (linkDirective == null) {
      val linkDirectiveSchemaExtension = createLinkDirectiveSchemaExtension(project, listOf(element.nameForImport))
      val extraSchemaFile = schemaFiles.firstOrNull { it.name == "extra.graphqls" }
      if (extraSchemaFile == null) {
        val fileText = linkDirectiveSchemaExtension.text + catchDirectiveSchemaExtension?.let { "\n\n" + it.text }
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
          extraSchemaFile.add(GraphQLElementFactory.createNewLine(project))
          extraSchemaFile.add(GraphQLElementFactory.createNewLine(project))
          extraSchemaFile.add(it)
        }
      }
    } else {
      val extraSchemaFile = linkDirective.containingFile
      val importedNames = buildList {
        addAll(linkDirective.arguments!!.argumentList.firstOrNull { it.name == "import" }?.value?.cast<GraphQLArrayValue>()?.valueList.orEmpty().map { it.text.unquoted() })
        add(element.nameForImport)
      }
      linkDirective.replace(createLinkDirective(project, importedNames))
      catchDirectiveSchemaExtension?.let {
        extraSchemaFile.add(GraphQLElementFactory.createNewLine(project))
        extraSchemaFile.add(GraphQLElementFactory.createNewLine(project))
        extraSchemaFile.add(it)
      }
    }
  }
}

private fun createLinkDirectiveSchemaExtension(project: Project, importedNames: List<String>): GraphQLSchemaExtension {
  val names = if ("@catch" in importedNames && "CatchTo" !in importedNames) importedNames + "CatchTo" else importedNames
  return GraphQLElementFactory.createFile(
      project,
      """
        extend schema
        @link(
          url: "$URL_NULLABILITY",
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

private fun createLinkDirective(project: Project, importedNames: List<String>): GraphQLDirective {
  return createLinkDirectiveSchemaExtension(project, importedNames).directives.single()
}

private fun GraphQLFile.schemaCatchDirectives(): List<GraphQLDirective> {
  val schemaDirectives = typeDefinitions.filterIsInstance<GraphQLSchemaExtension>().flatMap { it.directives } +
      typeDefinitions.filterIsInstance<GraphQLSchemaDefinition>().flatMap { it.directives }
  return schemaDirectives.filter { it.name == "catch" }
}
