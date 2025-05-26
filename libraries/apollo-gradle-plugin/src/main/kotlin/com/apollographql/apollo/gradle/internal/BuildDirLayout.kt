package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.Service
import gratatouille.capitalizeFirstLetter
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

object BuildDirLayout {
  internal fun outputDir(project: Project, service: Service): Provider<Directory> {
    // Warning: this has to match exactly what the implementation is doing
    return project.layout.buildDirectory.dir(
        "gtask/generate${service.name.capitalizeFirstLetter()}ApolloSources/outputDirectory"
    )
  }

  internal fun dataBuildersOutputDir(project: Project, service: Service): Provider<Directory> {
    return project.layout.buildDirectory.dir(
        "generated/dataBuildersSource/apollo/${service.name}"
    )
  }

  internal fun versionCheck(project: Project): Provider<RegularFile> {
    return project.layout.buildDirectory.file(
        "generated/checks/apollo/versionCheck"
    )
  }
}
