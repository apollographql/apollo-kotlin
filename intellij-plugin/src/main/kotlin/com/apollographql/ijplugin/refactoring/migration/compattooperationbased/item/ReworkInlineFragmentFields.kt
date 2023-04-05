package com.apollographql.ijplugin.refactoring.migration.compattooperationbased.item

import com.apollographql.ijplugin.refactoring.findClassReferences
import com.apollographql.ijplugin.refactoring.findInheritorsOfClass
import com.apollographql.ijplugin.refactoring.findReferences
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.apollographql.ijplugin.refactoring.migration.item.toMigrationItemUsageInfo
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.classes.KtLightClassBase
import org.jetbrains.kotlin.idea.base.utils.fqname.fqName
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPsiFactory

object ReworkInlineFragmentFields : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    val usageInfo = mutableListOf<MigrationItemUsageInfo>()

    val operationInheritors = findInheritorsOfClass(project, "com.apollographql.apollo3.api.Operation").filterIsInstance<KtLightClassBase>()
    val fragmentDataInheritors = findInheritorsOfClass(project, "com.apollographql.apollo3.api.Fragment.Data").filterIsInstance<KtLightClassBase>()
    val allModels = (operationInheritors + fragmentDataInheritors).flatMap {
      it.kotlinOrigin?.body?.declarations.orEmpty().filterIsInstance<KtClass>()
    }
    val inlineFragmentProperties = allModels.flatMap { model ->
      model.inlineFragmentProperties()
    }
    val propertyReferences = inlineFragmentProperties.flatMap { property ->
      findReferences(property, project)
    }
    usageInfo.addAll(propertyReferences.map { PropertyUsageInfo(this, it) })

    val inlineFragmentTypes = inlineFragmentProperties.mapNotNull {
      it.type()?.fqName?.asString()
    }
    val typeReferences = inlineFragmentTypes.flatMap { type ->
      findClassReferences(project, type)
    }
    typeReferences.toMigrationItemUsageInfo()
    usageInfo.addAll(typeReferences.map { TypeUsageInfo(this, it) })

    return usageInfo
  }

  private class PropertyUsageInfo(migrationItem: MigrationItem, reference: PsiReference) :
      MigrationItemUsageInfo(migrationItem, reference)

  private class TypeUsageInfo(migrationItem: MigrationItem, reference: PsiReference) :
      MigrationItemUsageInfo(migrationItem, reference)


  private fun KtClassOrObject.inlineFragmentProperties(): List<KtParameter> {
    return primaryConstructorParameters.filter {
      it.hasValOrVar() &&
          it.name?.isInlineFragmentPropertyName() == true &&
          it.docComment?.text?.contains("Synthetic field for inline fragment") == true
    }
  }

  private fun String.isInlineFragmentPropertyName(): Boolean {
    return startsWith("as") && this.getOrNull(2)?.isUpperCase() == true
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val psiFactory = KtPsiFactory(project)
    when (usage) {
      is PropertyUsageInfo -> {
        usage.element.replace(psiFactory.createExpression(usage.element.text.replaceFirst("as", "on")))
      }

      is TypeUsageInfo -> {
        usage.element.replace(psiFactory.createExpression(usage.element.text.replaceFirst("As", "On")))
      }
    }
  }
}
