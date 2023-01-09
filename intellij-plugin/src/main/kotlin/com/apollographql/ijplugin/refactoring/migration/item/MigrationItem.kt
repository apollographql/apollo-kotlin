package com.apollographql.ijplugin.refactoring.migration.item

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope

abstract class MigrationItem {
  open fun prepare(project: Project, migration: PsiMigration) {}
  abstract fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo>
  abstract fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo)
  open fun importsToAdd(): Set<String> = emptySet()
}
