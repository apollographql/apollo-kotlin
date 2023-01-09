package com.apollographql.ijplugin.refactoring.migration

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.refactoring.migration.item.AddUseVersion2Compat
import com.apollographql.ijplugin.refactoring.migration.item.CommentDependenciesInToml
import com.apollographql.ijplugin.refactoring.migration.item.DeletesElements
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
import com.apollographql.ijplugin.refactoring.migration.item.RemoveDependenciesInBuildKts
import com.apollographql.ijplugin.refactoring.migration.item.RemoveMethodCall
import com.apollographql.ijplugin.refactoring.migration.item.RemoveMethodImport
import com.apollographql.ijplugin.refactoring.migration.item.UpdateAddCustomTypeAdapter
import com.apollographql.ijplugin.refactoring.migration.item.UpdateClassName
import com.apollographql.ijplugin.refactoring.migration.item.UpdateCustomTypeMappingInBuildKts
import com.apollographql.ijplugin.refactoring.migration.item.UpdateEnumValueUpperCase
import com.apollographql.ijplugin.refactoring.migration.item.UpdateFieldName
import com.apollographql.ijplugin.refactoring.migration.item.UpdateFileUpload
import com.apollographql.ijplugin.refactoring.migration.item.UpdateGradleDependenciesBuildKts
import com.apollographql.ijplugin.refactoring.migration.item.UpdateGradleDependenciesInToml
import com.apollographql.ijplugin.refactoring.migration.item.UpdateGradlePluginInBuildKts
import com.apollographql.ijplugin.refactoring.migration.item.UpdateGraphqlSourceDirectorySet
import com.apollographql.ijplugin.refactoring.migration.item.UpdateHttpCache
import com.apollographql.ijplugin.refactoring.migration.item.UpdateIdlingResource
import com.apollographql.ijplugin.refactoring.migration.item.UpdateInputAbsent
import com.apollographql.ijplugin.refactoring.migration.item.UpdateLruNormalizedCacheFactory
import com.apollographql.ijplugin.refactoring.migration.item.UpdateMethodName
import com.apollographql.ijplugin.refactoring.migration.item.UpdateOkHttpExecutionContext
import com.apollographql.ijplugin.refactoring.migration.item.UpdatePackageName
import com.apollographql.ijplugin.refactoring.migration.item.UpdateSqlNormalizedCacheFactory
import com.apollographql.ijplugin.util.containingKtFileImportList
import com.apollographql.ijplugin.util.logd
import com.apollographql.ijplugin.util.logw
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.history.LocalHistory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.GeneratedSourcesFilter
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMigration
import com.intellij.psi.impl.migration.PsiMigrationManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.ui.UsageViewDescriptorAdapter
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.idea.util.application.isUnitTestMode
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.plugins.gradle.util.GradleConstants

/**
 * Migrations of Apollo Android v2 to Apollo Kotlin v3.
 *
 * Implementation is based on [com.intellij.refactoring.migration.MigrationProcessor] and
 * [org.jetbrains.android.refactoring.MigrateToAndroidxProcessor].
 */
class ApolloV2ToV3MigrationProcessor(project: Project) : BaseRefactoringProcessor(project) {
  private companion object {
    private const val apollo2 = "com.apollographql.apollo"
    private const val apollo3 = "com.apollographql.apollo3"
    private const val apollo3LatestVersion = "3.7.1"

    private val migrationItems = arrayOf(
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

    private fun getRefactoringName() = ApolloBundle.message("ApolloV2ToV3MigrationProcessor.title")
  }

  private val migrationManager = PsiMigrationManager.getInstance(myProject)
  private var migration: PsiMigration? = null
  private val searchScope = GlobalSearchScope.projectScope(project)

  override fun getCommandName() = getRefactoringName()

  private val usageViewDescriptor = object : UsageViewDescriptorAdapter() {
    override fun getElements(): Array<PsiElement> = PsiElement.EMPTY_ARRAY

    override fun getProcessedElementsHeader() = ApolloBundle.message("ApolloV2ToV3MigrationProcessor.codeReferences")
  }

  override fun createUsageViewDescriptor(usages: Array<out UsageInfo>) = usageViewDescriptor

  private fun startMigration(): PsiMigration {
    return migrationManager.startMigration()
  }

  private fun finishMigration() {
    migrationManager?.currentMigration?.finish()
  }

  override fun doRun() {
    logd()
    migration = startMigration()
    // This will create classes / packages that we're finding references to in case they don't exist.
    // It must be done in doRun() as this is called from the EDT whereas findUsages() is not.
    for (migrationItem in migrationItems) {
      migrationItem.prepare(myProject, migration!!)
    }
    super.doRun()
  }

  override fun findUsages(): Array<UsageInfo> {
    logd()
    try {
      val usageInfos = migrationItems
          .flatMap { migrationItem ->
            migrationItem.findUsages(myProject, migration!!, searchScope)
                .filterNot { usageInfo ->
                  // Filter out all generated code usages. We don't want generated code to come up in findUsages.
                  // TODO: how to mark Apollo generated code as generated per this method?
                  usageInfo.virtualFile?.let {
                    GeneratedSourcesFilter.isGeneratedSourceByAnyFilter(it, myProject)
                  } == true
                }
          }
          .toMutableList()
      // If an element must be deleted, make sure we keep the UsageInfo and remove any other pointing to the same element.
      val iterator = usageInfos.listIterator()
      while (iterator.hasNext()) {
        val usageInfo = iterator.next()
        if (usageInfo.migrationItem !is DeletesElements) {
          if (usageInfos.any { it !== usageInfo && it.migrationItem is DeletesElements && it.smartPointer == usageInfo.smartPointer }) {
            iterator.remove()
          }
        }
      }
      return usageInfos.toTypedArray()
    } finally {
      ApplicationManager.getApplication().invokeLater({ WriteAction.run<Throwable>(::finishMigration) }, myProject.disposed)
    }
  }

  override fun preprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean {
    logd()
    if (refUsages.get().isEmpty()) {
      Messages.showInfoMessage(
          myProject,
          ApolloBundle.message("ApolloV2ToV3MigrationProcessor.noUsage"),
          getRefactoringName()
      )
      return false
    }
    // Set to true to see the "preview usages" UI prior to refactoring.
    // isPreviewUsages = true
    return true
  }

  override fun performRefactoring(usages: Array<UsageInfo>) {
    logd()
    finishMigration()
    migration = startMigration()
    val action = LocalHistory.getInstance().startAction(commandName)
    try {
      for (usage in usages) {
        val migrationItem = (usage as MigrationItemUsageInfo).migrationItem
        try {
          if (!usage.isValid) continue
          maybeAddImports(usage, migrationItem)
          migrationItem.performRefactoring(myProject, migration!!, usage)
        } catch (t: Throwable) {
          logw(t, "Error while performing refactoring for $migrationItem")
        }
      }
      postRefactoring()
    } finally {
      action.finish()
      finishMigration()
    }
  }

  private fun maybeAddImports(
      usage: MigrationItemUsageInfo,
      migrationItem: MigrationItem,
  ) {
    val importsToAdd = migrationItem.importsToAdd()
    if (importsToAdd.isNotEmpty()) {
      val psiFactory = KtPsiFactory(myProject)
      usage.element.containingKtFileImportList()?.let { importList ->
        importsToAdd.forEach { importToAdd ->
          if (importList.imports.none { it.importPath?.pathStr == importToAdd }) {
            importList.add(psiFactory.createImportDirective(ImportPath.fromString(importToAdd)))
          }
        }
      }
    }
  }

  private fun postRefactoring() {
    logd()
    PsiDocumentManager.getInstance(myProject).performLaterWhenAllCommitted {
      // Not sure if this is actually useful but IJ's editor sometimes has a hard time after the files have been touched
      PsiManager.getInstance(myProject).apply {
        dropResolveCaches()
        dropPsiCaches()
      }
      DaemonCodeAnalyzer.getInstance(myProject).restart()

      // Sync gradle
      if (!isUnitTestMode()) {
        ExternalSystemUtil.refreshProject(
            myProject,
            GradleConstants.SYSTEM_ID,
            myProject.basePath!!,
            false,
            ProgressExecutionMode.IN_BACKGROUND_ASYNC
        )
      }
    }
  }
}

