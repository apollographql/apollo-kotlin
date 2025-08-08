package com.apollographql.apollo.gradle.task

import com.apollographql.apollo.compiler.ide.ProjectIdeModel
import com.apollographql.apollo.compiler.ide.writeTo
import gratatouille.tasks.GManuallyWired
import gratatouille.tasks.GOutputFile
import gratatouille.tasks.GTask

@GTask
internal fun apolloGenerateProjectIdeModel(
    // Inputs
    serviceNames: Set<String>,

    // Outputs
    @GManuallyWired
    projectIdeModelFile: GOutputFile,
) {
  ProjectIdeModel(
      serviceNames = serviceNames,
  )
      .writeTo(projectIdeModelFile)
}
