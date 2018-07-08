package com.apollographql.apollo.gradle

import com.google.common.collect.ImmutableList
import com.moowork.gradle.node.task.NodeTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory

class ApolloLocalCodegenGenerationTask extends NodeTask {
  public static final String APOLLO_CODEGEN_EXEC_FILE = "lib/cli.js"
  public static final String APOLLO_CODEGEN = "apollo-codegen/node_modules/apollo-codegen/" + APOLLO_CODEGEN_EXEC_FILE

  String variant
  ImmutableList<String> sourceSetNames
  @Input @Optional Property<String> schemaFilePath = project.objects.property(String.class)
  @Input @Optional Property<String> outputPackageName = project.objects.property(String.class)
  @OutputDirectory final DirectoryProperty outputDir = project.layout.directoryProperty()

  @Override
  void exec() {
    File apolloScript = new File(getProject().getBuildDir(), APOLLO_CODEGEN)
    if (!apolloScript.isFile()) {
      throw new GradleException(
          "Apollo-codegen was not found in node_modules. Please run the installApolloCodegen task.")
    }
    setScript(apolloScript)

    List<CodegenGenerationTaskCommandArgsBuilder.CommandArgs> args = new CodegenGenerationTaskCommandArgsBuilder(
        this, schemaFilePath.get(), outputPackageName.get(), outputDir.get().asFile, variant, sourceSetNames
    ).build()

    for (CodegenGenerationTaskCommandArgsBuilder.CommandArgs commandArgs : args) {
      setArgs(commandArgs.taskArguments)
      super.exec()
    }
  }
}
