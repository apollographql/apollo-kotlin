package com.apollographql.ijplugin.refactoring.migration.compattooperationbased

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.refactoring.migration.ApolloMigrationRefactoringProcessor
import com.apollographql.ijplugin.refactoring.migration.compattooperationbased.item.RemoveFragmentsField
import com.apollographql.ijplugin.refactoring.migration.compattooperationbased.item.ReworkInlineFragmentFields
import com.apollographql.ijplugin.refactoring.migration.compattooperationbased.item.UpdateCodegenInBuildKts
import com.intellij.openapi.project.Project

/**
 * Migrations of using the compat codegen to using the operationBased one.
 */
class CompatToOperationBasedCodegenMigrationProcessor(project: Project) : ApolloMigrationRefactoringProcessor(project) {
  override val refactoringName = ApolloBundle.message("CompatToOperationBasedCodegenMigrationProcessor.title")

  override val noUsageMessage = ApolloBundle.message("CompatToOperationBasedCodegenMigrationProcessor.noUsage")

  override val migrationItems = listOf(
      UpdateCodegenInBuildKts,
      ReworkInlineFragmentFields,
      RemoveFragmentsField,
  )
}
