package com.apollographql.ijplugin.refactoring.migration.v3tov4

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.refactoring.migration.ApolloMigrationRefactoringProcessor
import com.apollographql.ijplugin.refactoring.migration.apollo3
import com.apollographql.ijplugin.refactoring.migration.item.UpdateGradleDependenciesBuildKts
import com.apollographql.ijplugin.refactoring.migration.item.UpdateGradleDependenciesInToml
import com.apollographql.ijplugin.refactoring.migration.item.UpdateGradlePluginInBuildKts
import com.intellij.openapi.project.Project

private const val apollo4LatestVersion = "4.0.0-alpha.2"

/**
 * Migrations of Apollo Kotlin v3 to v4.
 */
class ApolloV3ToV4MigrationProcessor(project: Project) : ApolloMigrationRefactoringProcessor(project) {
  override val refactoringName = ApolloBundle.message("ApolloV3ToV4MigrationProcessor.title")

  override val noUsageMessage = ApolloBundle.message("ApolloV3ToV4MigrationProcessor.noUsage")

  override val migrationItems = listOf(
      // Gradle
      UpdateGradlePluginInBuildKts(apollo3, apollo3, apollo4LatestVersion),
      UpdateGradleDependenciesInToml(apollo3, apollo3, apollo4LatestVersion),
      UpdateGradleDependenciesBuildKts(apollo3, apollo3),
  )
}
