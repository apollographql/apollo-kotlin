package com.apollographql.ijplugin.gradle

import com.apollographql.ijplugin.util.logw
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project

const val CODEGEN_GRADLE_TASK_NAME = "generateApolloSources"

fun Module.getGradleName(): String? {
  val projectId = ExternalSystemApiUtil.getExternalProjectId(this) ?: return null
  // "MyProject:main" -> ""
  // "MyProject:MyModule:main" -> "MyModule"
  // "MyProject:MyModule:MySubModule:main" -> "MyModule:MySubModule"
  return projectId.split(":").drop(1).dropLast(1).joinToString(":")
}

fun Project.getGradleRootPath(): String? {
  val rootProjectPath = ModuleManager.getInstance(this).modules.firstNotNullOfOrNull {
    ExternalSystemApiUtil.getExternalRootProjectPath(it)
  }
  if (rootProjectPath == null) logw("Could not get Gradle root project path")
  return rootProjectPath
}
