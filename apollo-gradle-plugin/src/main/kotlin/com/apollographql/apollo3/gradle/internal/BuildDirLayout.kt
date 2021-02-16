package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.gradle.api.Service
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

object BuildDirLayout {
  internal fun operationOuput(project: Project, service: Service): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/operationOutput/apollo/${service.name}/operationOutput.json"
    )
  }

  internal fun metadata(project: Project, service: Service): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/metadata/apollo/${service.name}/metadata.json"
    )
  }

  internal fun sources(project: Project, service: Service): Provider<Directory> {
    return project.layout.buildDirectory.dir(
        "generated/source/apollo/${service.name}"
    )
  }

  internal fun versionCheck(project: Project): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/checks/apollo/versionCheck"
    )
  }

  internal fun duplicatesCheck(project: Project, service: Service): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/checks/apollo/${service.name}/duplicatesCheck"
    )
  }
}