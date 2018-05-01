package com.apollographql.apollo.gradle;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import com.apollographql.apollo.compiler.GraphQLCompiler;

import org.gradle.api.tasks.AbstractExecTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;

import java.io.File;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class ApolloSystemCodegenGenerationTask extends AbstractExecTask<ApolloSystemCodegenGenerationTask> {
  String variant;
  ImmutableList<String> sourceSets;
  String schemaFilePath;
  String outputPackageName;
  File outputFolder;

  public ApolloSystemCodegenGenerationTask() {
    super(ApolloSystemCodegenGenerationTask.class);
  }

  public void init(String variant, ImmutableList<String> sourceSets) {
    this.variant = variant;
    this.sourceSets = sourceSets;
    outputFolder = new File(getProject().getBuildDir() + File.separator +
        Joiner.on(File.separator).join(GraphQLCompiler.OUTPUT_DIRECTORY) + "/generatedIR/" + variant);
  }

  @Override
  public void exec() {
    setCommandLine("apollo-codegen");

    List<CodegenGenerationTaskCommandArgsBuilder.CommandArgs> args = new CodegenGenerationTaskCommandArgsBuilder(
        this, schemaFilePath, outputPackageName, outputFolder, variant, sourceSets
    ).build();

    for (CodegenGenerationTaskCommandArgsBuilder.CommandArgs commandArgs : args) {
      setArgs(commandArgs.taskArguments);
      super.exec();
    }
  }

  @OutputDirectory
  public File getOutputFolder() {
    return outputFolder;
  }

  @Input
  @Optional
  public String getSchemaFilePath() {
    return schemaFilePath;
  }

  @Input
  @Optional
  public String getOutputPackageName() {
    return outputPackageName;
  }
}
