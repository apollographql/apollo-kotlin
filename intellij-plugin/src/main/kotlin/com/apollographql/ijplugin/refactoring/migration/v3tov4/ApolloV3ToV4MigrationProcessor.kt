package com.apollographql.ijplugin.refactoring.migration.v3tov4

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.refactoring.migration.ApolloMigrationRefactoringProcessor
import com.apollographql.ijplugin.refactoring.migration.apollo3
import com.apollographql.ijplugin.refactoring.migration.item.RemoveMethodCall
import com.apollographql.ijplugin.refactoring.migration.item.UpdateFieldName
import com.apollographql.ijplugin.refactoring.migration.item.UpdateGradleDependenciesBuildKts
import com.apollographql.ijplugin.refactoring.migration.item.UpdateGradleDependenciesInToml
import com.apollographql.ijplugin.refactoring.migration.item.UpdateGradlePluginInBuildKts
import com.apollographql.ijplugin.refactoring.migration.item.UpdateMethodName
import com.apollographql.ijplugin.refactoring.migration.v3tov4.item.RemoveWatchMethodArguments
import com.intellij.openapi.project.Project

private const val apollo4LatestVersion = "4.0.0-alpha.2"

/**
 * Migrations of Apollo Kotlin v3 to v4.
 */
class ApolloV3ToV4MigrationProcessor(project: Project) : ApolloMigrationRefactoringProcessor(project) {
  override val refactoringName = ApolloBundle.message("ApolloV3ToV4MigrationProcessor.title")

  override val noUsageMessage = ApolloBundle.message("ApolloV3ToV4MigrationProcessor.noUsage")

  override val migrationItems = listOf(
      // Deprecations / renames
      UpdateFieldName("$apollo3.api.ApolloResponse", "dataAssertNoErrors", "dataOrThrow()"),
      UpdateFieldName("$apollo3.exception.ApolloCompositeException", "first", "suppressedExceptions.first()"),
      UpdateFieldName("$apollo3.exception.ApolloCompositeException", "second", "suppressedExceptions.getOrNull(1)"),
      UpdateMethodName("$apollo3.ast.GQLResult", "valueAssertNoErrors", "getOrThrow"),
      RemoveMethodCall("$apollo3.cache.normalized.NormalizedCache", "emitCacheMisses", extensionTargetClassName = "$apollo3.api.MutableExecutionOptions"),
      RemoveWatchMethodArguments(),

      // Gradle
      UpdateGradlePluginInBuildKts(apollo3, apollo3, apollo4LatestVersion),
      UpdateGradleDependenciesInToml(apollo3, apollo3, apollo4LatestVersion),
      UpdateGradleDependenciesBuildKts(apollo3, apollo3),
  )
}
