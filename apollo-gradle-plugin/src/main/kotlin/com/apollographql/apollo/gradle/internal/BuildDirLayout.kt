package com.apollographql.apollo.gradle.internal

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

object BuildDirLayout {
  internal fun operationOuput(project: Project, compilationUnit: DefaultCompilationUnit): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/operationOutput/apollo/${compilationUnit.variantName}/${compilationUnit.serviceName}/operationOutput.json"
    )
  }

  internal fun metadata(project: Project, compilationUnit: DefaultCompilationUnit): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/metadata/apollo/${compilationUnit.variantName}/${compilationUnit.serviceName}/metadata.json"
    )
  }

  internal fun sources(project: Project, compilationUnit: DefaultCompilationUnit): Provider<Directory> {
    return project.layout.buildDirectory.dir(
        "generated/source/apollo/${compilationUnit.variantName}/${compilationUnit.serviceName}"
    )
  }

  internal fun versionCheck(project: Project): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/checks/apollo/versionCheck"
    )
  }

  internal fun duplicatesCheck(project: Project, compilationUnit: DefaultCompilationUnit): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/checks/apollo/${compilationUnit.variantName}/${compilationUnit.serviceName}/duplicatesCheck"
    )
  }
}