package com.apollographql.ijplugin.refactoring.migration.compattooperationbased.item

import com.apollographql.ijplugin.refactoring.findReferences
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.childrenOfType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.KtValueArgumentName
import org.jetbrains.kotlin.psi.psiUtil.findPropertyByName

object RemoveFragmentsField : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    val allModels: List<KtClass> = findAllModels(project)
    val fragmentsProperties = allModels.mapNotNull { model ->
      model.findPropertyByName("fragments")
    }
    val references = fragmentsProperties.flatMap { property ->
      findReferences(property, project).map { it.element }
    }
    return references
        .mapNotNull {
          when (val parent = it.parent) {
            is KtQualifiedExpression -> {
              when {
                // fragments.x
                parent.receiverExpression.text == "fragments" -> {
                  parent.toMigrationItemUsageInfo(true)
                }
                // x.fragments
                parent.selectorExpression?.text == "fragments" -> {
                  parent.toMigrationItemUsageInfo(false)
                }

                else -> null
              }
            }

            is KtValueArgumentName -> {
              // fragments = ...
              (parent.parent as? KtValueArgument)?.toMigrationItemUsageInfo()
            }

            else -> null
          }
        }
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    when (val element = usage.element) {
      is KtQualifiedExpression -> {
        if (usage.attachedData()) {
          // fragments.x -> x
          element.replace(element.selectorExpression!!)
        } else {
          // x.fragments -> x
          element.replace(element.receiverExpression)
        }
      }

      // fragments = Xxx.Fragments(yyy = ..., xxx = ...) -> yyy = ..., xxx = ...
      is KtValueArgument -> {
        // Xxx.Fragments(yyy = ..., xxx = ...)
        val callExpression = element.getArgumentExpression()?.childrenOfType<KtCallExpression>()?.firstOrNull()
        // yyy = ..., xxx = ...
        val enclosedArguments = callExpression?.valueArgumentList ?: return
        val parentArgumentList = element.parent as KtValueArgumentList
        for (enclosedArgument in enclosedArguments.arguments) {
          parentArgumentList.addArgument(enclosedArgument)
        }
        parentArgumentList.removeArgument(element)
      }

      else -> {}
    }
  }
}
