package com.apollographql.apollo.gradle

import com.google.common.collect.ImmutableList
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory

class ApolloSystemCodegenGenerationTask extends AbstractExecTask<ApolloSystemCodegenGenerationTask> {
  String variant
  ImmutableList<String> sourceSetNames
  @Input @Optional Property<String> schemaFilePath = project.objects.property(String.class)
  @Input @Optional Property<String> outputPackageName = project.objects.property(String.class)
  @OutputDirectory final DirectoryProperty outputDir = project.layout.directoryProperty()

  ApolloSystemCodegenGenerationTask() {
    super(ApolloSystemCodegenGenerationTask.class)
  }

  @Override
  void exec() {
    setCommandLine("apollo-codegen")

    List<CodegenGenerationTaskCommandArgsBuilder.CommandArgs> args = new CodegenGenerationTaskCommandArgsBuilder(
        this, schemaFilePath.get(), outputPackageName.get(), outputDir.get().asFile, variant, sourceSetNames
    ).build()

    for (CodegenGenerationTaskCommandArgsBuilder.CommandArgs commandArgs : args) {
      setArgs(commandArgs.taskArguments)
      super.exec()
    }
  }
}
