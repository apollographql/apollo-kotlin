package com.apollographql.ijplugin.inspection

import com.apollographql.ijplugin.util.isGenerated
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor

class HasUsageProcessor : Processor<UsageInfo> {
  var foundUsage = false
    private set

  override fun process(usageInfo: UsageInfo): Boolean {
    if (usageInfo.virtualFile?.isGenerated(usageInfo.project) == false) {
      foundUsage = true
      return false
    }
    return true
  }
}
