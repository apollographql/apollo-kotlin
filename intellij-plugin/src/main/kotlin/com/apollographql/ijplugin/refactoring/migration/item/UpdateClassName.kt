package com.apollographql.ijplugin.refactoring.migration.item

import com.apollographql.ijplugin.refactoring.findClassReferences
import com.apollographql.ijplugin.refactoring.findOrCreateClass
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.ImportPath

class UpdateClassName(
    private val oldName: String,
    private val newName: String,
) : MigrationItem() {
  override fun prepare(project: Project, migration: PsiMigration) {
    findOrCreateClass(project, migration, newName)
  }

  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    return findClassReferences(project, oldName)
        .toMigrationItemUsageInfo()
        .map {
          val element = it.element
          val importDirective = element.parentOfType<KtImportDirective>()
          if (importDirective != null) {
            // Reference is an import
            ReplaceImportUsageInfo(this@UpdateClassName, importDirective)
          } else {
            it
          }
        }
  }

  private class ReplaceImportUsageInfo(migrationItem: MigrationItem, element: KtImportDirective) :
      MigrationItemUsageInfo(migrationItem, element)


  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val element = usage.element
    val psiFactory = KtPsiFactory(project)
    when (usage) {
      is ReplaceImportUsageInfo -> {
        element.replace(psiFactory.createImportDirective(ImportPath.fromString(newName)))
      }

      else -> {
        element.replace(psiFactory.createExpression(newName.split('.').last()))
      }
    }
  }
}
