package com.apollographql.ijplugin.util

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module

fun Module.getGradleName(): String? {
  val projectId = ExternalSystemApiUtil.getExternalProjectId(this) ?: return null
  // "MyProject:main" -> ""
  // "MyProject:MyModule:main" -> "MyModule"
  // "MyProject:MyModule:MySubModule:main" -> "MyModule:MySubModule"
  return projectId.split(":").drop(1).dropLast(1).joinToString(":")
}
