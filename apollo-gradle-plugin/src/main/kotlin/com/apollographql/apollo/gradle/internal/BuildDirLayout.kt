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

  internal fun metadataZip(project: Project, compilationUnit: DefaultCompilationUnit): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/zip/apollo/${compilationUnit.variantName}/${compilationUnit.serviceName}/apolloMetadata.zip"
    )
  }

  internal fun metadata(project: Project, compilationUnit: DefaultCompilationUnit): Provider<Directory> {
    return project.layout.buildDirectory.dir(
        "generated/metadata/apollo/${compilationUnit.variantName}/${compilationUnit.serviceName}"
    )
  }

  internal fun sources(project: Project, compilationUnit: DefaultCompilationUnit): Provider<Directory> {
    return project.layout.buildDirectory.dir(
        "generated/source/apollo/${compilationUnit.variantName}/${compilationUnit.serviceName}"
    )
  }

  internal fun versionCheck(project: Project): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/versionCheck/apollo/versionCheck"
    )
  }
}