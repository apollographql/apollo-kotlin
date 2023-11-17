package com.apollographql.ijplugin.refactoring.migration.v2tov3.item

import com.apollographql.ijplugin.refactoring.findMethodReferences
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.apollographql.ijplugin.util.resolve
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMigration
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

object UpdateAddCustomTypeAdapter : MigrationItem() {
  override fun findUsages(project: Project, migration: PsiMigration, searchScope: GlobalSearchScope): List<MigrationItemUsageInfo> {
    return findMethodReferences(
        project = project,
        className = "com.apollographql.apollo.ApolloClient.Builder",
        methodName = "addCustomTypeAdapter"
    )
        .mapNotNull {
          val callExpression = it.element.parent as? KtCallExpression ?: return@mapNotNull null
          // 1st argument is (generally) a reference to the generated custom type, e.g. `CustomType.DATETIME`
          val firstArgument = callExpression.valueArguments.firstOrNull() ?: return@mapNotNull null
          val argumentDotQualifiedExpression = firstArgument.getArgumentExpression() as? KtDotQualifiedExpression ?: return@mapNotNull null
          // e.g. `DATETIME`
          val typeReference = argumentDotQualifiedExpression.selectorExpression as? KtNameReferenceExpression ?: return@mapNotNull null
          val enumEntry = typeReference.resolve() as? KtEnumEntry ?: return@mapNotNull null
          val classBody = enumEntry.body ?: return@mapNotNull null
          // 1st function looks like `override fun typeName(): String = "DateTime"`
          val typeNameFunction = classBody.functions.firstOrNull() ?: return@mapNotNull null
          // `"DateTime"`
          val bodyExpression = typeNameFunction.bodyExpression as? KtStringTemplateExpression ?: return@mapNotNull null
          val typeName = bodyExpression.entries.firstOrNull()?.text ?: return@mapNotNull null
          MigrationItemUsageInfo(this, firstArgument, attachedData = typeName)
        }
  }

  override fun performRefactoring(project: Project, migration: PsiMigration, usage: MigrationItemUsageInfo) {
    val psiFactory = KtPsiFactory(project)
    val callExpression = usage.element.parentOfType<KtCallExpression>() ?: return
    callExpression.prevSibling.parent.addBefore(
        psiFactory.createComment("// TODO: Use addCustomScalarAdapter instead. See https://www.apollographql.com/docs/kotlin/migration/3.0/#custom-scalar-adapters"),
        callExpression.prevSibling
    )
    usage.element.replace(psiFactory.createExpression("${usage.attachedData<String>()}.type"))
  }
}
