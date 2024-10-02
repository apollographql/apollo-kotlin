package com.apollographql.ijplugin.refactoring.migration.item

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.usageView.UsageInfo

open class MigrationItemUsageInfo : UsageInfo {
  val migrationItem: MigrationItem
  private val attachedData: Any?

  constructor(migrationItem: MigrationItem, reference: PsiReference, attachedData: Any? = null) : super(reference) {
    this.migrationItem = migrationItem
    this.attachedData = attachedData
  }

  constructor(migrationItem: MigrationItem, element: PsiElement, attachedData: Any? = null) : super(element) {
    this.migrationItem = migrationItem
    this.attachedData = attachedData
  }

  constructor(migrationItem: MigrationItem, source: UsageInfo, attachedData: Any? = null) : super(
      source.element!!,
      source.rangeInElement!!.startOffset,
      source.rangeInElement!!.endOffset
  ) {
    this.migrationItem = migrationItem
    this.attachedData = attachedData
  }

  fun <T> attachedData(): T {
    @Suppress("UNCHECKED_CAST")
    return attachedData as T
  }

  override fun getElement(): PsiElement {
    return super.getElement()!!
  }

  override fun isValid(): Boolean {
    return super.getElement() != null && super.isValid()
  }

  /**
   * Taken from https://github.com/JetBrains/android/blob/b9cc41149734794ca6746711b1fb5a2fb6d06ec4/project-system-gradle/src/org/jetbrains/android/refactoring/MigrateToAndroidxProcessor.kt
   *
   * Verifies if one of the calls on the stack comes from the Optimize Imports Refactoring Helper.
   * We check the last 5 elements to allow for some future flow changes.
   *
   * This avoids the helper kicking as it can remove imports we're trying to add.
   */
  private fun isKotlinOptimizerCall(): Boolean = Thread.currentThread().stackTrace
      .take(5)
      .map { it.className }
      .any { it.contains("OptimizeImports", ignoreCase = true) }

  override fun getFile(): PsiFile? = if (isKotlinOptimizerCall()) {
    null
  } else {
    super.getFile()
  }
}
