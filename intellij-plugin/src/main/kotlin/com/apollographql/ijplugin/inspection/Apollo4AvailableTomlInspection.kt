package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.action.ApolloV3ToV4MigrationAction
import com.apollographql.ijplugin.util.unquoted
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.toml.lang.psi.TomlInlineTable
import org.toml.lang.psi.TomlLiteral
import org.toml.lang.psi.TomlTable
import org.toml.lang.psi.TomlVisitor
import org.toml.lang.psi.ext.TomlLiteralKind
import org.toml.lang.psi.ext.kind

private const val apollo3 = "com.apollographql.apollo3"

class Apollo4AvailableTomlInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : TomlVisitor() {
      override fun visitLiteral(element: TomlLiteral) {
        if (!element.containingFile.name.endsWith(".versions.toml")) return
        super.visitLiteral(element)
        if (element.kind !is TomlLiteralKind.String) return
        val dependencyText = element.text.unquoted()
        if (dependencyText == apollo3 || dependencyText.startsWith("$apollo3:")) {
          // Find the associated version
          val versionEntry = (element.parent.parent as? TomlInlineTable)?.entries
              ?.first { it.key.text == "version" || it.key.text == "version.ref" } ?: return
          val version = if (versionEntry.key.text == "version") {
            versionEntry.value?.firstChild?.text?.unquoted() ?: return
          } else {
            // Resolve the reference
            val versionsTable = element.containingFile.children.filterIsInstance<TomlTable>()
                .firstOrNull { it.header.key?.text == "versions" } ?: return
            val versionRefKey = versionEntry.value?.text?.unquoted()
            val refTarget = versionsTable.entries.firstOrNull { it.key.text == versionRefKey } ?: return
            refTarget.value?.firstChild?.text?.unquoted() ?: return
          }
          if (!version.startsWith("4")) {
            holder.registerProblem(element.parent.parent.parent, ApolloBundle.message("inspection.apollo4AvailableToml.reportText"), Apollo4AvailableTomlQuickFix)
          }
        }
      }
    }
  }
}

object Apollo4AvailableTomlQuickFix : LocalQuickFix {
  override fun getName() = ApolloBundle.message("inspection.apollo4AvailableToml.quickFix")

  override fun getFamilyName() = name

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    val action = ActionManager.getInstance().getAction(ApolloV3ToV4MigrationAction.ACTION_ID)
    ActionManager.getInstance().tryToExecute(action, null, null, null, false)
  }
}
