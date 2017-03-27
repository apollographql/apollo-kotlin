package com.apollographql.android.gradle;

import com.google.common.base.Joiner;

import com.apollographql.android.compiler.GraphQLCompiler;

import org.gradle.api.Action;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.api.tasks.incremental.InputFileDetails;

import java.io.File;
import java.util.Map;

public class ApolloClassGenTask extends SourceTask {
  static final String NAME = "generate%sApolloClasses";

  @Internal private String variant;
  @Input private Map<String, String> customTypeMapping;
  @Input private boolean useOptional;
  @Input private boolean hasGuavaDep;
  @Input private boolean generateAccessors;
  @OutputDirectory private File outputDir;

  public void init(String buildVariant, Map<String, String> typeMapping, boolean generateOptional, boolean hasGuava,
      boolean accessors) {
    variant = buildVariant;
    customTypeMapping = typeMapping;
    useOptional = generateOptional;
    hasGuavaDep = hasGuava;
    generateAccessors = accessors;
    outputDir = new File(getProject().getBuildDir() + "/" + Joiner.on(File.separator).join(GraphQLCompiler.Companion
        .getOUTPUT_DIRECTORY()));
  }

  @TaskAction
  void generateClasses(IncrementalTaskInputs inputs) {
    inputs.outOfDate(new Action<InputFileDetails>() {
      @Override
      public void execute(InputFileDetails inputFileDetails) {
        GraphQLCompiler.Arguments args = new GraphQLCompiler.Arguments(inputFileDetails.getFile(), outputDir,
            customTypeMapping, useOptional, hasGuavaDep, generateAccessors);
        new GraphQLCompiler().write(args);
      }
    });
  }

  public String getVariant() {
    return variant;
  }

  public void setVariant(String variant) {
    this.variant = variant;
  }

  public File getOutputDir() {
    return outputDir;
  }

  public void setOutputDir(File outputDir) {
    this.outputDir = outputDir;
  }

  public Map<String, String> getCustomTypeMapping() {
    return customTypeMapping;
  }

  public void setCustomTypeMapping(Map<String, String> customTypeMapping) {
    this.customTypeMapping = customTypeMapping;
  }

  public boolean isUseOptional() {
    return useOptional;
  }

  public void setUseOptional(boolean useOptional) {
    this.useOptional = useOptional;
  }

  public boolean shouldGenerateAccessors() {
    return generateAccessors;
  }

  public void setGenerateAccessors(boolean generateAccessors) {
    this.generateAccessors = generateAccessors;
  }
}
