package com.apollographql.apollo.gradle


import com.moowork.gradle.node.task.NodeTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory

class ApolloLocalCodegenGenerationTask extends NodeTask {
  static final String CLI_PATH = "apollo-codegen" +
      File.separator + "node_modules" +
      File.separator + "apollo-codegen" +
      File.separator + "lib" +
      File.separator + "cli.js"

  @Input final Property<String> variant = project.objects.property(String.class)
  @Input final Property<List> sourceSetNames = project.objects.property(List.class)
  @Input @Optional final Property<String> schemaFilePath = project.objects.property(String.class)
  @Input @Optional final Property<String> outputPackageName = project.objects.property(String.class)
  @OutputDirectory final DirectoryProperty outputDir = project.layout.directoryProperty()

  @Override
  void exec() {
    setScript(new File(project.buildDir, CLI_PATH))

    List<CodegenGenerationTaskCommandArgsBuilder.CommandArgs> args = new CodegenGenerationTaskCommandArgsBuilder(
        this, schemaFilePath.get(), outputPackageName.get(), outputDir.get().asFile, variant.get(),
        sourceSetNames.get().collect { it.toString() }).build()

    for (CodegenGenerationTaskCommandArgsBuilder.CommandArgs commandArgs : args) {
      setArgs(commandArgs.taskArguments)
      super.exec()
    }
  }
}
