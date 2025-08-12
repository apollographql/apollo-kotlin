package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.ide.ProjectModel
import com.apollographql.apollo.compiler.ide.writeTo
import gratatouille.tasks.GManuallyWired
import gratatouille.tasks.GOutputFile
import gratatouille.tasks.GTask

@GTask
internal fun apolloGenerateProjectModel(
    // Inputs
    serviceNames: Set<String>,

    // Outputs
    @GManuallyWired
    projectModelFile: GOutputFile,
) {
  ProjectModel(
      serviceNames = serviceNames,
  )
      .writeTo(projectModelFile)
}
