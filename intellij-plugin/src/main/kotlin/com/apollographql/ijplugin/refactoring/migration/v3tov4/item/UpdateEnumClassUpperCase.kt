package com.apollographql.ijplugin.refactoring.migration.v3tov4.item

import com.apollographql.ijplugin.navigation.isApolloEnumClass
import com.apollographql.ijplugin.refactoring.findAllClasses
import com.apollographql.ijplugin.refactoring.findClassReferences
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.apollographql.ijplugin.util.capitalizeFirstLetter
import com.apollographql.ijplugin.util.isGenerated
import com.apollographql.ijplugin.util.ktClass
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtPsiFactory

object UpdateEnumClassUpperCase : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    val allClasses = findAllClasses(project)
    val allApolloGeneratedEnums: List<KtClass> = allClasses
        .filter { it.isEnum }
        .mapNotNull { it.ktClass }
        .filter { it.isApolloEnumClass() && it.name!![0].isLowerCase() }
    return allApolloGeneratedEnums.flatMap {
      findClassReferences(project, it.fqName!!.asString())
          // Exclude references in generated code
          .filterNot { it.element.containingFile?.virtualFile?.isGenerated(project) == true }

    }.toMigrationItemUsageInfo()
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    usage.element.replace(KtPsiFactory(project).createExpression(usage.element.text.capitalizeFirstLetter()))
  }
}
