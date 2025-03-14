package com.apollographql.ijplugin.gradle

import com.apollographql.ijplugin.util.logw
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import org.gradle.tooling.model.GradleProject

const val CODEGEN_GRADLE_TASK_NAME = "generateApolloSources"

fun Project.getGradleRootPath(): String? {
  val rootProjectPath = ModuleManager.getInstance(this).modules.firstNotNullOfOrNull {
    ExternalSystemApiUtil.getExternalRootProjectPath(it)
  }
  if (rootProjectPath == null) logw("Could not get Gradle root project path")
  return rootProjectPath
}

fun GradleProject.allChildrenRecursively(): List<GradleProject> {
  return listOf(this) + children.flatMap { it.allChildrenRecursively() }
}
