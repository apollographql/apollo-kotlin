package com.apollographql.ijplugin.refactoring.migration.item

import com.apollographql.ijplugin.util.addSiblingAfter
import com.apollographql.ijplugin.util.findPsiFilesByName
import com.apollographql.ijplugin.util.unquoted
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType

object UpdateCustomTypeMappingInBuildKts : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    val buildGradleKtsFiles: List<PsiFile> = project.findPsiFilesByName("build.gradle.kts", searchScope)
    val usages = mutableListOf<MigrationItemUsageInfo>()
    for (file in buildGradleKtsFiles) {
      if (file !is KtFile) continue
      file.accept(object : KtTreeVisitorVoid() {
        override fun visitReferenceExpression(expression: KtReferenceExpression) {
          super.visitReferenceExpression(expression)
          if (expression.text == "customTypeMapping" || expression.text == "customScalarsMapping") {
            // `customTypeMapping.set(mapOf(...))`
            val qualifiedExpression = expression.parent as? KtQualifiedExpression ?: return
            // `set(mapOf(...))`
            val setCallExpression = qualifiedExpression.selectorExpression
                ?.findDescendantOfType<KtCallExpression> { it.calleeExpression?.text == "set" }
            if (setCallExpression != null) {
              // `mapOf(...)`
              val mapOfCallExpression = setCallExpression.valueArguments.firstOrNull()
                  ?.findDescendantOfType<KtCallExpression> { it.calleeExpression?.text == "mapOf" } ?: return

              @Suppress("UNCHECKED_CAST")
              val map: Map<String, String> = mapOfCallExpression.valueArguments.associate { argument ->
                val binaryExpression = argument.getArgumentExpression() as? KtBinaryExpression ?: return@associate null to null
                val key = binaryExpression.left?.text?.unquoted()
                val value = binaryExpression.right?.text?.unquoted()
                key to value
              }
                  .filterNot { it.key == null || it.value == null } as Map<String, String>
              usages.add(MigrationItemUsageInfo(this@UpdateCustomTypeMappingInBuildKts, qualifiedExpression, map))
              return
            }

            // `put("LocalDate", "java.time.LocalDate")`
            val putCallExpression = qualifiedExpression.selectorExpression
                ?.findDescendantOfType<KtCallExpression> { it.calleeExpression?.text == "put" } ?: return
            val key = putCallExpression.valueArguments.getOrNull(0)?.text?.unquoted()?: return
            val value = putCallExpression.valueArguments.getOrNull(1)?.text?.unquoted()?: return
            usages.add(MigrationItemUsageInfo(this@UpdateCustomTypeMappingInBuildKts, qualifiedExpression, mapOf(key to  value)))
          }
        }
      })
    }
    return usages
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val element = usage.element
    val map: Map<String, String> = usage.attachedData()
    val psiFactory = KtPsiFactory(project)
    map.entries.toList().reversed().forEach { (scalar, kotlinType) ->
      val text = when (kotlinType) {
        "kotlin.String" -> "mapScalarToKotlinString(\"$scalar\")"
        "kotlin.Int" -> "mapScalarToKotlinInt(\"$scalar\")"
        "kotlin.Double" -> "mapScalarToKotlinDouble(\"$scalar\")"
        "kotlin.Float" -> "mapScalarToKotlinFloat(\"$scalar\")"
        "kotlin.Long" -> "mapScalarToKotlinLong(\"$scalar\")"
        "kotlin.Boolean" -> "mapScalarToKotlinBoolean(\"$scalar\")"
        "kotlin.Any" -> "mapScalarToKotlinAny(\"$scalar\")"
        "java.lang.String" -> "mapScalarToJavaString(\"$scalar\")"
        "java.lang.Integer" -> "mapScalarToJavaInteger(\"$scalar\")"
        "java.lang.Double" -> "mapScalarToJavaDouble(\"$scalar\")"
        "java.lang.Float" -> "mapScalarToJavaFloat(\"$scalar\")"
        "java.lang.Long" -> "mapScalarToJavaLong(\"$scalar\")"
        "java.lang.Boolean" -> "mapScalarToJavaBoolean(\"$scalar\")"
        "java.lang.Object" -> "mapScalarToJavaObject(\"$scalar\")"
        "com.apollographql.apollo.api.FileUpload" -> "mapScalarToUpload(\"$scalar\")"
        else -> "mapScalar(\"$scalar\", \"$kotlinType\")"
      }
      element.addSiblingAfter(psiFactory.createExpression(text))
      element.addSiblingAfter(psiFactory.createNewLine())
    }
    element.delete()
  }
}
