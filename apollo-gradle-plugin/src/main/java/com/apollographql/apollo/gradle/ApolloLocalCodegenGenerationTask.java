package com.apollographql.apollo.gradle;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import com.apollographql.apollo.compiler.GraphQLCompiler;
import com.moowork.gradle.node.task.NodeTask;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;

import java.io.File;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class ApolloLocalCodegenGenerationTask extends NodeTask {
  static final String APOLLO_CODEGEN_EXEC_FILE = "lib/cli.js";
  static final String APOLLO_CODEGEN = "apollo-codegen/node_modules/apollo-codegen/" + APOLLO_CODEGEN_EXEC_FILE;

  String variant;
  ImmutableList<String> sourceSets;
  String schemaFilePath;
  String outputPackageName;
  File outputFolder;

  public void init(String variant, ImmutableList<String> sourceSets) {
    this.variant = variant;
    this.sourceSets = sourceSets;
    outputFolder = new File(getProject().getBuildDir() + File.separator +
        Joiner.on(File.separator).join(GraphQLCompiler.OUTPUT_DIRECTORY) + "/generatedIR/" + variant);
  }

  @Override
  public void exec() {
    File apolloScript = new File(getProject().getBuildDir(), APOLLO_CODEGEN);
    if (!apolloScript.isFile()) {
      throw new GradleException("Apollo-codegen was not found in node_modules. Please run the installApolloCodegen task.");
    }
    setScript(apolloScript);

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
