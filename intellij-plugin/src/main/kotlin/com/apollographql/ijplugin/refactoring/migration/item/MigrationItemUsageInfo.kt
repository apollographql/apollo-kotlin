package com.apollographql.ijplugin.refactoring.migration.item

import com.intellij.psi.PsiElement
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
}

context(MigrationItem)
fun UsageInfo.toMigrationItemUsageInfo() = MigrationItemUsageInfo(migrationItem = this@MigrationItem, source = this)

context(MigrationItem)
fun Array<UsageInfo>.toMigrationItemUsageInfo(): List<MigrationItemUsageInfo> {
  return map { it.toMigrationItemUsageInfo() }
}

context(MigrationItem)
fun PsiReference.toMigrationItemUsageInfo() = MigrationItemUsageInfo(migrationItem = this@MigrationItem, reference = this)

context(MigrationItem)
fun Collection<PsiReference>.toMigrationItemUsageInfo(): List<MigrationItemUsageInfo> {
  return map { it.toMigrationItemUsageInfo() }
}

context(MigrationItem)
fun PsiElement.toMigrationItemUsageInfo(attachedData: Any? = null) =
    MigrationItemUsageInfo(migrationItem = this@MigrationItem, element = this, attachedData = attachedData)
