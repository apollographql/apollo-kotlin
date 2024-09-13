package com.apollographql.ijplugin.refactoring.migration.v2tov3.item

import com.apollographql.ijplugin.refactoring.findClassReferences
import com.apollographql.ijplugin.refactoring.findFieldReferences
import com.apollographql.ijplugin.refactoring.findInheritorsOfClass
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.apollographql.ijplugin.util.ktClassOrObject
import com.apollographql.ijplugin.util.unquoted
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry

object UpdateEnumValueUpperCase : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    val usageInfo = mutableListOf<MigrationItemUsageInfo>()
    val enumValueInheritors = findInheritorsOfClass(project, "com.apollographql.apollo.api.EnumValue")
    for (enumValueInheritor in enumValueInheritors) {
      val kotlinOrigin = enumValueInheritor.ktClassOrObject ?: continue
      when {
        kotlinOrigin is KtObjectDeclaration -> {
          // Sealed subclass
          val superTypeCallEntry = kotlinOrigin.superTypeListEntries.firstOrNull() as? KtSuperTypeCallEntry ?: continue
          val sealedClassOldName = kotlinOrigin.name!!
          val sealedClassNewName = superTypeCallEntry.valueArguments.firstOrNull()?.getArgumentExpression()?.text?.unquoted() ?: continue
          if (sealedClassNewName == sealedClassOldName) {
            // Enum is upper case in the schema: no need to update
            continue
          }
          val references = findClassReferences(project, kotlinOrigin.fqName.toString())
              // Exclude references in generated code
              .filterNot { it.element.parentOfType<KtNamedFunction>()?.name == "safeValueOf" }
          for (reference in references) {
            usageInfo.add(MigrationItemUsageInfo(this@UpdateEnumValueUpperCase, reference, sealedClassNewName))
          }
        }

        kotlinOrigin is KtClass && kotlinOrigin.isEnum() -> {
          // Enum: look for references to enum values
          for (enumEntry in kotlinOrigin.body?.enumEntries ?: emptyList()) {
            val superTypeCallEntry = enumEntry.initializerList?.initializers?.firstOrNull() as? KtSuperTypeCallEntry ?: continue
            val enumOldName = enumEntry.name!!
            val enumNewName = superTypeCallEntry.valueArguments.firstOrNull()?.getArgumentExpression()?.text?.unquoted() ?: continue
            if (enumNewName == enumOldName) {
              // Enum is upper case in the schema: no need to update
              continue
            }
            val references = findFieldReferences(project, enumValueInheritor.qualifiedName!!, enumOldName)
            for (reference in references) {
              usageInfo.add(MigrationItemUsageInfo(this@UpdateEnumValueUpperCase, reference, enumNewName))
            }
          }

        }
      }
    }

    return usageInfo
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    usage.element.replace(KtPsiFactory(project).createExpression(usage.attachedData()))
  }
}
