package com.apollographql.apollo.gradle;

import com.google.common.base.Joiner;

import com.apollographql.apollo.compiler.GraphQLCompiler;
import com.apollographql.apollo.compiler.NullableValueType;

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
  @Input private NullableValueType nullValueType;
  @Input private boolean generateAccessors;
  @OutputDirectory private File outputDir;
  private Boolean useSemanticNaming;

  public void init(String buildVariant, Map<String, String> typeMapping, String nullableValueType, boolean accessors,
      boolean semanticNaming) {
    variant = buildVariant;
    customTypeMapping = typeMapping;
    nullValueType = nullableValueType == null ? NullableValueType.ANNOTATED
        : NullableValueType.Companion.findByValue(nullableValueType);
    generateAccessors = accessors;
    outputDir = new File(getProject().getBuildDir() + "/" + Joiner.on(File.separator).join(GraphQLCompiler.Companion
        .getOUTPUT_DIRECTORY()));
    useSemanticNaming = semanticNaming;
  }

  @TaskAction
  void generateClasses(IncrementalTaskInputs inputs) {
    inputs.outOfDate(new Action<InputFileDetails>() {
      @Override
      public void execute(InputFileDetails inputFileDetails) {
        GraphQLCompiler.Arguments args = new GraphQLCompiler.Arguments(inputFileDetails.getFile(), outputDir,
            customTypeMapping, nullValueType, generateAccessors, useSemanticNaming);
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

  public NullableValueType getNullValueType() {
    return nullValueType;
  }

  public void setNullValueType(NullableValueType nullValueType) {
    this.nullValueType = nullValueType;
  }

  public boolean shouldGenerateAccessors() {
    return generateAccessors;
  }

  public void setGenerateAccessors(boolean generateAccessors) {
    this.generateAccessors = generateAccessors;
  }
}
