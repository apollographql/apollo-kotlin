package com.apollographql.ijplugin.refactoring.migration.item

import com.apollographql.ijplugin.refactoring.findClassReferences
import com.apollographql.ijplugin.refactoring.findOrCreateClass
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.psi.KtImportDirective

class RemoveImport(
    private val import: String,
) : MigrationItem(), DeletesElements {

  override fun prepare(project: Project, migration: PsiMigration) {
    findOrCreateClass(project, migration, import)
  }

  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    return findClassReferences(project, import)
        .mapNotNull {
          it.element.parentOfType<KtImportDirective>()?.toMigrationItemUsageInfo()
        }
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    usage.element.delete()
  }
}
