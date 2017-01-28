package com.apollographql.android.gradle;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import org.gradle.api.Action;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.api.tasks.incremental.InputFileDetails;

import com.apollographql.android.compiler.GraphQLCompiler;

import com.google.common.base.Joiner;

public class ApolloClassGenTask extends SourceTask {
  static final String NAME = "generate%sApolloClasses";

  @Internal private List<GraphQLExtension> config;
  @Internal private String variant;
  @Internal private boolean generateClasses;
  @OutputDirectory private File outputDir;

  public void init(String variantName, List<GraphQLExtension> extensionsConfig, boolean genClasses) {
    variant = variantName;
    generateClasses = genClasses;
    config = extensionsConfig;
    // TODO: change to constant once ApolloPlugin is in java
    setGroup("apollo");
    setDescription("Generate Android classes for " + Utils.capitalize(variant) + " GraphQL queries");
    dependsOn(getProject().getTasks().findByName(String.format(ApolloIRGenTask.NAME, Utils.capitalize(variant))));
    outputDir = new File(getProject().getBuildDir() + "/" + Joiner.on(File.separator).join(GraphQLCompiler.Companion
        .getOUTPUT_DIRECTORY()));
  }

  @TaskAction
  void generateClasses(IncrementalTaskInputs inputs) {
    inputs.outOfDate(new Action<InputFileDetails>() {
      @Override
      public void execute(InputFileDetails inputFileDetails) {
        new GraphQLCompiler().write(inputFileDetails.getFile(), outputDir, generateClasses,
            new HashMap<String, String>());
      }
    });
  }

  public List<GraphQLExtension> getConfig() {
    return config;
  }

  public void setConfig(List<GraphQLExtension> config) {
    this.config = config;
  }

  public String getVariant() {
    return variant;
  }

  public void setVariant(String variant) {
    this.variant = variant;
  }

  public boolean isGenerateClasses() {
    return generateClasses;
  }

  public void setGenerateClasses(boolean generateClasses) {
    this.generateClasses = generateClasses;
  }

  public File getOutputDir() {
    return outputDir;
  }

  public void setOutputDir(File outputDir) {
    this.outputDir = outputDir;
  }
}
