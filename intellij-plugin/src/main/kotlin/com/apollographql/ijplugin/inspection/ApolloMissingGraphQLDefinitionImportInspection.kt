package com.apollographql.ijplugin.inspection

import com.apollographql.apollo3.ast.GQLDefinition
import com.apollographql.apollo3.ast.GQLDirectiveDefinition
import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLNamed
import com.apollographql.apollo3.ast.GQLScalarTypeDefinition
import com.apollographql.apollo3.ast.rawType
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
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.jsgraphql.psi.GraphQLArrayValue
import com.intellij.lang.jsgraphql.psi.GraphQLDirective
import com.intellij.lang.jsgraphql.psi.GraphQLElementFactory
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
        visitDirective(o, holder, NULLABILITY_DEFINITIONS, NULLABILITY_URL, ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
        visitDirective(o, holder, KOTLIN_LABS_DEFINITIONS, KOTLIN_LABS_URL, ProblemHighlightType.WEAK_WARNING)
      }
    }
  }

  private fun visitDirective(
      directiveElement: GraphQLDirective,
      holder: ProblemsHolder,
      definitions: List<GQLDefinition>,
      definitionsUrl: String,
      highlightType: ProblemHighlightType,
  ) {
    if (directiveElement.name !in definitions.directives().map { it.name }) return
    val message = if (highlightType == ProblemHighlightType.WEAK_WARNING) "inspection.missingGraphQLDefinitionImport.reportText.warning" else "inspection.missingGraphQLDefinitionImport.reportText.error"
    if (!directiveElement.isImported(definitionsUrl)) {
      val typeKind = ApolloBundle.message("inspection.missingGraphQLDefinitionImport.reportText.directive")
      holder.registerProblem(
          directiveElement,
          ApolloBundle.message(message, typeKind, directiveElement.name!!),
          highlightType,
          ImportDefinitionQuickFix(typeKind = typeKind, elementName = directiveElement.name!!, definitions = definitions, definitionsUrl = definitionsUrl),
      )
    } else {
      val directiveDefinition = definitions.directives().firstOrNull { it.name == directiveElement.name } ?: return
      val knownDefinitionNames = definitions.filterIsInstance<GQLNamed>().map { it.name }
      val arguments = directiveElement.arguments?.argumentList.orEmpty()
      for (argument in arguments) {
        val argumentDefinition = directiveDefinition.arguments.firstOrNull { it.name == argument.name } ?: continue
        val argumentTypeToImport = argumentDefinition.type.rawType().name.takeIf { it in knownDefinitionNames } ?: continue
        if (!isImported(directiveElement, argumentTypeToImport, definitionsUrl)) {
          val typeKind = getTypeKind(argumentTypeToImport)
          holder.registerProblem(
              argument,
              ApolloBundle.message(message, typeKind, argumentTypeToImport),
              highlightType,
              ImportDefinitionQuickFix(typeKind = typeKind, elementName = argumentTypeToImport, definitions = definitions, definitionsUrl = definitionsUrl),
          )
        }
      }
    }
  }
}

private fun getTypeKind(typeName: String): String {
  val typeDefinition = NULLABILITY_DEFINITIONS.firstOrNull { it is GQLNamed && it.name == typeName } ?: return "unknown"
  return ApolloBundle.message(
      when (typeDefinition) {
        is GQLDirectiveDefinition -> "inspection.missingGraphQLDefinitionImport.reportText.directive"
        is GQLEnumTypeDefinition -> "inspection.missingGraphQLDefinitionImport.reportText.enum"
        is GQLInputObjectTypeDefinition -> "inspection.missingGraphQLDefinitionImport.reportText.input"
        is GQLScalarTypeDefinition -> "inspection.missingGraphQLDefinitionImport.reportText.scalar"
        else -> return "unknown"
      }
  )
}

private class ImportDefinitionQuickFix(
    val typeKind: String,
    val elementName: String,
    private val definitions: List<GQLDefinition>,
    private val definitionsUrl: String,
) : LocalQuickFix {
  override fun getName() = ApolloBundle.message("inspection.missingGraphQLDefinitionImport.quickFix", typeKind, "'$elementName'")
  override fun getFamilyName() = name

  override fun availableInBatchMode() = false
  override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo = IntentionPreviewInfo.EMPTY

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    if (!IntentionPreviewUtils.isIntentionPreviewActive()) project.telemetryService.logEvent(TelemetryEvent.ApolloIjMissingGraphQLDefinitionImportQuickFix())

    val element = descriptor.psiElement.parentOfType<GraphQLDirective>(withSelf = true)!!
    val schemaFiles = element.schemaFiles()
    val linkDirective = schemaFiles.flatMap { it.linkDirectives(definitionsUrl) }.firstOrNull()

    // Special case: also add a @catch directive to the schema if we're importing @catch
    val catchDirectiveSchemaExtension = if (element.name == CATCH && schemaFiles.flatMap { it.schemaCatchDirectives() }.isEmpty()) {
      createCatchDirectiveSchemaExtension(project)
    } else {
      null
    }

    if (linkDirective == null) {
      val linkDirectiveSchemaExtension = createLinkDirectiveSchemaExtension(project, setOf(element.nameForImport), definitions, definitionsUrl)
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
        extraSchemaFile.add(GraphQLElementFactory.createWhiteSpace(project, "\n\n"))
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
      linkDirective.replace(createLinkDirective(project, importedNames, definitions, definitionsUrl))
      catchDirectiveSchemaExtension?.let {
        extraSchemaFile.add(GraphQLElementFactory.createWhiteSpace(project, "\n\n"))
        extraSchemaFile.add(it)
      }
    }
  }
}

private fun createLinkDirectiveSchemaExtension(
    project: Project,
    importedNames: Set<String>,
    definitions: List<GQLDefinition>,
    definitionsUrl: String,
): GraphQLSchemaExtension {
  // If any of the imported name is a directive, add its argument types to the import list
  val knownDefinitionNames = definitions.filterIsInstance<GQLNamed>().map { it.name }
  val additionalNames = importedNames.flatMap { importedName ->
    definitions.directives().firstOrNull { "@${it.name}" == importedName }
        ?.arguments
        ?.map { it.type.rawType().name }
        ?.filter { it in knownDefinitionNames }.orEmpty()
  }.toSet()

  return GraphQLElementFactory.createFile(
      project,
      """
        extend schema
        @link(
          url: "$definitionsUrl",
          import: [${(importedNames + additionalNames).joinToString { it.quoted() }}]
        )
      """.trimIndent()
  )
      .findChildrenOfType<GraphQLSchemaExtension>().single()
}

private fun createCatchDirectiveSchemaExtension(project: Project): GraphQLSchemaExtension {
  return GraphQLElementFactory.createFile(project, "extend schema @catch(to: THROW)")
      .findChildrenOfType<GraphQLSchemaExtension>().single()
}

private fun createLinkDirective(
    project: Project,
    importedNames: Set<String>,
    definitions: List<GQLDefinition>,
    definitionsUrl: String,
): GraphQLDirective {
  return createLinkDirectiveSchemaExtension(project, importedNames, definitions, definitionsUrl).directives.single()
}

private fun GraphQLFile.schemaCatchDirectives(): List<GraphQLDirective> {
  val schemaDirectives = typeDefinitions.filterIsInstance<GraphQLSchemaExtension>().flatMap { it.directives } +
      typeDefinitions.filterIsInstance<GraphQLSchemaDefinition>().flatMap { it.directives }
  return schemaDirectives.filter { it.name == CATCH }
}
