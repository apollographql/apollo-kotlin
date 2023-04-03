package com.apollographql.ijplugin.refactoring.migration

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.refactoring.migration.item.DeletesElements
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItem
import com.apollographql.ijplugin.refactoring.migration.item.MigrationItemUsageInfo
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
 * Generic processor for migrations.
 *
 * Implementation is based on [com.intellij.refactoring.migration.MigrationProcessor] and
 * `org.jetbrains.android.refactoring.MigrateToAndroidxProcessor`.
 */
abstract class ApolloMigrationRefactoringProcessor(project: Project) : BaseRefactoringProcessor(project) {
  abstract val refactoringName: String
  abstract val noUsageMessage: String
  abstract val migrationItems: List<MigrationItem>

  private val migrationManager = PsiMigrationManager.getInstance(myProject)
  private var migration: PsiMigration? = null
  private val searchScope = GlobalSearchScope.projectScope(project)

  override fun getCommandName() = refactoringName

  private val usageViewDescriptor = object : UsageViewDescriptorAdapter() {
    override fun getElements(): Array<PsiElement> = PsiElement.EMPTY_ARRAY

    override fun getProcessedElementsHeader() = ApolloBundle.message("ApolloMigrationRefactoringProcessor.codeReferences")
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
          noUsageMessage,
          refactoringName
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

