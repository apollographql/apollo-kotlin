package com.apollographql.apollo.gradle;

import com.google.common.base.Joiner;

import com.apollographql.apollo.compiler.GraphQLCompiler;
import com.apollographql.apollo.compiler.NullableValueType;

import org.gradle.api.Action;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.api.tasks.incremental.InputFileDetails;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class ApolloClassGenerationTask extends SourceTask {
  static final String NAME = "generate%sApolloClasses";

  Map<String, String> customTypeMapping;
  String nullableValueType;
  boolean useSemanticNaming;
  boolean generateModelBuilder;
  boolean useJavaBeansSemanticNaming;
  boolean useRawTypesWarningSuppression;
  String outputPackageName;
  final File outputDir;

  public ApolloClassGenerationTask() {
    outputDir = new File(getProject().getBuildDir() + File.separator + Joiner.on(File.separator)
        .join(GraphQLCompiler.OUTPUT_DIRECTORY));
  }

  @TaskAction
  void generateClasses(IncrementalTaskInputs inputs) {
    final NullableValueType nullableValueType = this.nullableValueType == null ? NullableValueType.ANNOTATED
        : NullableValueType.Companion.findByValue(this.nullableValueType);
    inputs.outOfDate(new Action<InputFileDetails>() {
      @Override
      public void execute(@NotNull InputFileDetails inputFileDetails) {
        GraphQLCompiler.Arguments args = new GraphQLCompiler.Arguments(inputFileDetails.getFile(), outputDir,
            customTypeMapping, nullableValueType, useSemanticNaming, generateModelBuilder, useJavaBeansSemanticNaming,
            outputPackageName, useRawTypesWarningSuppression);
        new GraphQLCompiler().write(args);
      }
    });
  }

  @OutputDirectory
  public File getOutputDir() {
    return outputDir;
  }

  @Input
  public Map<String, String> getCustomTypeMapping() {
    return customTypeMapping;
  }

  @Input
  @Optional
  public String getNullableValueType() {
    return nullableValueType;
  }

  @Input
  public boolean isUseSemanticNaming() {
    return useSemanticNaming;
  }

  @Input
  public boolean isGenerateModelBuilder() {
    return generateModelBuilder;
  }

  @Input
  public boolean isUseJavaBeansSemanticNaming() {
    return useJavaBeansSemanticNaming;
  }

  @Input
  public boolean isUseRawTypesWarningSuppression() { return useRawTypesWarningSuppression; }

  @Input
  @Optional
  public String getOutputPackageName() {
    return outputPackageName;
  }
}
