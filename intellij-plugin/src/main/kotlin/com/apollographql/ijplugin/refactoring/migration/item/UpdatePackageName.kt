package com.apollographql.ijplugin.refactoring.migration.item

import com.apollographql.ijplugin.refactoring.bindReferencesToElement
import com.apollographql.ijplugin.refactoring.findOrCreatePackage
import com.apollographql.ijplugin.refactoring.findPackageReferences
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope

class UpdatePackageName(
    private val oldName: String,
    private val newName: String,
) : MigrationItem() {
  override fun prepare(project: Project, migration: PsiMigration) {
    findOrCreatePackage(project, migration, newName)
  }

  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    return findPackageReferences(project, oldName).toMigrationItemUsageInfo()
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val newPackage = findOrCreatePackage(project, migration, newName)
    usage.element.bindReferencesToElement(newPackage)
  }
}
