package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.navigation.compat.KotlinFindUsagesHandlerFactoryCompat
import com.apollographql.ijplugin.navigation.findKotlinFieldDefinitions
import com.apollographql.ijplugin.navigation.findKotlinFragmentSpreadDefinitions
import com.apollographql.ijplugin.navigation.findKotlinInlineFragmentDefinitions
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.telemetry.TelemetryEvent
import com.apollographql.ijplugin.telemetry.telemetryService
import com.apollographql.ijplugin.util.isProcessCanceled
import com.apollographql.ijplugin.util.matchingFieldCoordinates
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ui.InspectionOptionsPanel
import com.intellij.codeInspection.ui.ListEditForm
import com.intellij.lang.jsgraphql.psi.GraphQLField
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentSpread
import com.intellij.lang.jsgraphql.psi.GraphQLIdentifier
import com.intellij.lang.jsgraphql.psi.GraphQLInlineFragment
import com.intellij.lang.jsgraphql.psi.GraphQLSelection
import com.intellij.lang.jsgraphql.psi.GraphQLTypeName
import com.intellij.lang.jsgraphql.psi.GraphQLTypedOperationDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLVisitor
import com.intellij.openapi.project.Project
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.findParentOfType
import javax.swing.JComponent

class ApolloUnusedFieldInspection : LocalInspectionTool() {
  @JvmField
  var fieldsToIgnore: MutableList<String> = mutableListOf()

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    var isUnusedOperation = false
    return object : GraphQLVisitor() {
      override fun visitIdentifier(o: GraphQLIdentifier) {
        if (isProcessCanceled()) return
        if (!o.project.apolloProjectService.apolloVersion.isAtLeastV3) return
        if (isUnusedOperation) return
        val operation = o.findParentOfType<GraphQLTypedOperationDefinition>()
        if (operation != null && isUnusedOperation(operation)) {
          // The whole operation is unused, no need to check the fields
          isUnusedOperation = true
          return
        }

        var isFragment = false
        val parent = o.parent
        val ktDefinitions = when (parent) {
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

        val matchingFieldCoordinates: Collection<String> = if (parent is GraphQLField) {
          matchingFieldCoordinates(o)
        } else {
          emptySet()
        }
        val shouldIgnoreField = fieldsToIgnore.any { fieldToIgnore ->
          matchingFieldCoordinates.any match@{ fieldCoordinates ->
            val regex = runCatching { Regex(fieldToIgnore) }.getOrNull() ?: return@match false
            fieldCoordinates.matches(regex)
          }
        }
        if (shouldIgnoreField) return

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
            *buildList {
              add(DeleteElementQuickFix(label = "inspection.unusedField.quickFix.deleteField", telemetryEvent = { TelemetryEvent.ApolloIjUnusedFieldDeleteFieldQuickFix() }) { it.findParentOfType<GraphQLSelection>(strict = false)!! })
              for (matchingFieldCoordinate in matchingFieldCoordinates) {
                add(IgnoreFieldQuickFix(matchingFieldCoordinate))
              }
            }.toTypedArray()
        )
      }
    }
  }

  // In a future version of the platform we should use `AddToInspectionOptionListFix` instead.
  private inner class IgnoreFieldQuickFix(private val fieldCoordinates: String) : LocalQuickFix {
    override fun getName() = ApolloBundle.message("inspection.unusedField.quickFix.ignoreField", fieldCoordinates)
    override fun getFamilyName() = name

    override fun availableInBatchMode() = false
    override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo = IntentionPreviewInfo.EMPTY

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
      if (!IntentionPreviewUtils.isIntentionPreviewActive()) project.telemetryService.logEvent(TelemetryEvent.ApolloIjUnusedFieldIgnoreFieldQuickFix())
      fieldsToIgnore += fieldCoordinates.replace(".", "\\.")

      // Save the inspection settings
      ProjectInspectionProfileManager.getInstance(project).fireProfileChanged()
    }
  }

  override fun createOptionsPanel(): JComponent {
    val form = ListEditForm(ApolloBundle.message("inspection.unusedField.options.fieldsToIgnore.title"), ApolloBundle.message("inspection.unusedField.options.fieldsToIgnore.label"), fieldsToIgnore)
    form.contentPanel.minimumSize = InspectionOptionsPanel.getMinimumListSize()
    return form.contentPanel
  }
}
