package com.apollographql.ijplugin.refactoring.migration.compattooperationbased.item

import com.apollographql.ijplugin.refactoring.findInheritorsOfClass
import com.apollographql.ijplugin.refactoring.findReferences
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.apollographql.ijplugin.refactoring.migration.item.toMigrationItemUsageInfo
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.classes.KtLightClassBase
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.findPropertyByName

object RemoveFragmentsField : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    val operationInheritors = findInheritorsOfClass(project, "com.apollographql.apollo3.api.Operation").filterIsInstance<KtLightClassBase>()
    val fragmentDataInheritors = findInheritorsOfClass(project, "com.apollographql.apollo3.api.Fragment.Data").filterIsInstance<KtLightClassBase>()
    val allModels = (operationInheritors + fragmentDataInheritors).flatMap {
      it.kotlinOrigin?.body?.declarations.orEmpty().filterIsInstance<KtClass>()
    }
    val fragmentsProperties = allModels.mapNotNull { model ->
      model.findPropertyByName("fragments")
    }
    val references = fragmentsProperties.flatMap { property ->
      findReferences(property, project).map { it.element }
    }
    return references
        .mapNotNull {
          val parent = it.parent as? KtDotQualifiedExpression ?: return@mapNotNull null
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
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val element = usage.element as KtDotQualifiedExpression
    if (usage.attachedData()) {
      // fragments.x -> x
      element.replace(element.selectorExpression!!)
    } else {
      // x.fragments -> x
      element.replace(element.receiverExpression)
    }
  }
}
