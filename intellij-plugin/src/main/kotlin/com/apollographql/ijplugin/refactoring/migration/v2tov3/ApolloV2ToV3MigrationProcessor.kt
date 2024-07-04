package com.apollographql.ijplugin.refactoring.migration.v2tov3

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.refactoring.migration.ApolloMigrationRefactoringProcessor
import com.apollographql.ijplugin.refactoring.migration.item.CommentDependenciesInToml
import com.apollographql.ijplugin.refactoring.migration.item.RemoveImport
import com.apollographql.ijplugin.refactoring.migration.item.RemoveMethodCall
import com.apollographql.ijplugin.refactoring.migration.item.RemoveMethodImport
import com.apollographql.ijplugin.refactoring.migration.item.UpdateClassName
import com.apollographql.ijplugin.refactoring.migration.item.UpdateCustomTypeMappingInBuildKts
import com.apollographql.ijplugin.refactoring.migration.item.UpdateFieldName
import com.apollographql.ijplugin.refactoring.migration.item.UpdateGradleDependenciesBuildKts
import com.apollographql.ijplugin.refactoring.migration.item.UpdateGradleDependenciesInToml
import com.apollographql.ijplugin.refactoring.migration.item.UpdateGradlePluginInBuildKts
import com.apollographql.ijplugin.refactoring.migration.item.UpdateMethodName
import com.apollographql.ijplugin.refactoring.migration.item.UpdatePackageName
import com.apollographql.ijplugin.refactoring.migration.v2tov3.item.AddUseVersion2Compat
import com.apollographql.ijplugin.refactoring.migration.v2tov3.item.RemoveDependenciesInBuildKts
import com.apollographql.ijplugin.refactoring.migration.v2tov3.item.UpdateAddCustomTypeAdapter
import com.apollographql.ijplugin.refactoring.migration.v2tov3.item.UpdateEnumValueUpperCase
import com.apollographql.ijplugin.refactoring.migration.v2tov3.item.UpdateFileUpload
import com.apollographql.ijplugin.refactoring.migration.v2tov3.item.UpdateGraphqlSourceDirectorySet
import com.apollographql.ijplugin.refactoring.migration.v2tov3.item.UpdateHttpCache
import com.apollographql.ijplugin.refactoring.migration.v2tov3.item.UpdateIdlingResource
import com.apollographql.ijplugin.refactoring.migration.v2tov3.item.UpdateInputAbsent
import com.apollographql.ijplugin.refactoring.migration.v2tov3.item.UpdateLruNormalizedCacheFactory
import com.apollographql.ijplugin.refactoring.migration.v2tov3.item.UpdateOkHttpExecutionContext
import com.apollographql.ijplugin.refactoring.migration.v2tov3.item.UpdateSqlNormalizedCacheFactory
import com.apollographql.ijplugin.util.apollo2
import com.apollographql.ijplugin.util.apollo3
import com.intellij.openapi.project.Project

private const val apollo3LatestVersion = "3.8.2"

/**
 * Migrations of Apollo Android v2 to Apollo Kotlin v3.
 *
 * Implementation is based on [com.intellij.refactoring.migration.MigrationProcessor] and
 * [org.jetbrains.android.refactoring.MigrateToAndroidxProcessor].
 */
class ApolloV2ToV3MigrationProcessor(project: Project) : ApolloMigrationRefactoringProcessor(project) {
  override val refactoringName = ApolloBundle.message("ApolloV2ToV3MigrationProcessor.title")

  override val noUsageMessage = ApolloBundle.message("ApolloV2ToV3MigrationProcessor.noUsage")

  override val migrationItems = listOf(
      // Apollo API / Runtime
      RemoveMethodImport("$apollo2.coroutines.CoroutinesExtensionsKt", "toFlow"),
      UpdateClassName("$apollo2.api.Response", "$apollo3.api.ApolloResponse"),
      UpdateClassName("$apollo2.ApolloQueryCall", "$apollo3.ApolloCall"),
      UpdateClassName("$apollo2.ApolloMutationCall", "$apollo3.ApolloCall"),
      UpdateClassName("$apollo2.ApolloSubscriptionCall", "$apollo3.ApolloCall"),
      UpdateMethodName("$apollo2.ApolloClient", "mutate", "mutation"),
      UpdateMethodName("$apollo2.ApolloClient", "subscribe", "subscription"),
      UpdateMethodName("$apollo2.ApolloClient", "builder", "Builder"),
      UpdateMethodName("$apollo2.coroutines.CoroutinesExtensionsKt", "await", "execute"),
      RemoveMethodImport("$apollo2.coroutines.CoroutinesExtensionsKt", "await"),
      UpdateMethodName("$apollo2.ApolloQueryCall", "watcher", "watch", importToAdd = "$apollo3.cache.normalized.watch"),
      RemoveMethodCall("$apollo2.coroutines.CoroutinesExtensionsKt", "toFlow", extensionTargetClassName = "$apollo2.ApolloQueryWatcher"),
      UpdateClassName("$apollo2.api.Input", "$apollo3.api.Optional"),
      UpdateMethodName("$apollo2.api.Input.Companion", "fromNullable", "Present"),
      UpdateMethodName("$apollo2.api.Input.Companion", "optional", "presentIfNotNull"),
      UpdateOkHttpExecutionContext,
      UpdateInputAbsent,
      RemoveMethodCall("$apollo2.api.OperationName", "name"),
      UpdateClassName("$apollo2.api.FileUpload", "$apollo3.api.Upload"),

      // Http cache
      UpdateMethodName(
          "$apollo2.ApolloQueryCall.Builder",
          "httpCachePolicy",
          "httpFetchPolicy",
          importToAdd = "$apollo3.cache.http.httpFetchPolicy"
      ),
      UpdateClassName("$apollo2.api.cache.http.HttpCachePolicy", "$apollo3.cache.http.HttpFetchPolicy"),
      UpdateFieldName("$apollo2.api.cache.http.HttpCachePolicy", "CACHE_ONLY", "CacheOnly"),
      UpdateFieldName("$apollo2.api.cache.http.HttpCachePolicy", "NETWORK_ONLY", "NetworkOnly"),
      UpdateFieldName("$apollo2.api.cache.http.HttpCachePolicy", "CACHE_FIRST", "CacheFirst"),
      UpdateFieldName("$apollo2.api.cache.http.HttpCachePolicy", "NETWORK_FIRST", "NetworkFirst"),
      UpdateHttpCache,
      UpdateMethodName("$apollo2.ApolloClient", "clearHttpCache", "httpCache.clearAll", importToAdd = "$apollo3.cache.http.httpCache"),

      // Normalized cache
      UpdateMethodName(
          "$apollo2.ApolloQueryCall.Builder",
          "responseFetcher",
          "fetchPolicy",
          importToAdd = "$apollo3.cache.normalized.fetchPolicy"
      ),
      UpdateClassName("$apollo2.fetcher.ApolloResponseFetchers", "$apollo3.cache.normalized.FetchPolicy"),
      UpdateFieldName("$apollo2.fetcher.ApolloResponseFetchers", "CACHE_ONLY", "CacheOnly"),
      UpdateFieldName("$apollo2.fetcher.ApolloResponseFetchers", "NETWORK_ONLY", "NetworkOnly"),
      UpdateFieldName("$apollo2.fetcher.ApolloResponseFetchers", "CACHE_FIRST", "CacheFirst"),
      UpdateFieldName("$apollo2.fetcher.ApolloResponseFetchers", "NETWORK_FIRST", "NetworkFirst"),
      UpdateMethodName(
          "$apollo2.cache.normalized.ApolloStore",
          "read",
          "readOperation",
          importToAdd = "$apollo3.cache.normalized.apolloStore"
      ),
      UpdateMethodName(
          "$apollo2.cache.normalized.ApolloStore",
          "writeAndPublish",
          "writeOperation",
          importToAdd = "$apollo3.cache.normalized.apolloStore"
      ),
      RemoveMethodCall("$apollo2.cache.normalized.ApolloStoreOperation", "execute"),
      UpdateLruNormalizedCacheFactory,
      UpdateSqlNormalizedCacheFactory,
      UpdateMethodName(
          "$apollo2.ApolloClient",
          "clearNormalizedCache",
          "apolloStore.clearAll",
          importToAdd = "$apollo3.cache.normalized.apolloStore"
      ),

      RemoveMethodCall("$apollo2.ApolloQueryCall", "toBuilder"),

      RemoveMethodCall("$apollo2.ApolloQueryCall.Builder", "build"),
      UpdatePackageName(apollo2, apollo3),

      RemoveImport("$apollo2.cache.normalized.lru.EvictionPolicy"),
      RemoveImport("$apollo2.cache.http.DiskLruHttpCacheStore"),
      RemoveImport("$apollo2.cache.http.internal.FileSystem"),
      RemoveImport("$apollo2.coroutines.await"),

      // Gradle
      UpdateGradlePluginInBuildKts(apollo2, apollo3, apollo3LatestVersion),
      CommentDependenciesInToml("apollo-coroutines-support", "apollo-android-support"),
      UpdateGradleDependenciesInToml(apollo2, apollo3, apollo3LatestVersion),
      UpdateGradleDependenciesBuildKts(apollo2, apollo3),
      RemoveDependenciesInBuildKts("$apollo2:apollo-coroutines-support", "$apollo2:apollo-android-support"),
      AddUseVersion2Compat,
      UpdateGraphqlSourceDirectorySet,

      // Custom scalars
      UpdateCustomTypeMappingInBuildKts,
      UpdateAddCustomTypeAdapter,

      // Enums
      UpdateEnumValueUpperCase,

      // Upload
      UpdateFileUpload,

      // Idling resource
      UpdateClassName("$apollo2.test.espresso.ApolloIdlingResource", "$apollo3.android.ApolloIdlingResource"),
      UpdateIdlingResource,
  )
}
