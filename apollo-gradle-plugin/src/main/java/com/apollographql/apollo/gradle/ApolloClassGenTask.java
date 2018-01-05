package com.apollographql.apollo.gradle;

import com.google.common.base.Joiner;

import com.apollographql.apollo.compiler.GraphQLCompiler;
import com.apollographql.apollo.compiler.NullableValueType;

import org.gradle.api.Action;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.api.tasks.incremental.InputFileDetails;

import java.io.File;

public class ApolloClassGenTask extends SourceTask {
  static final String NAME = "generate%sApolloClasses";

  @Internal private String variant;
  @Internal ApolloExtension apolloExtension;
  @OutputDirectory File outputDir;
  NullableValueType nullableValueType;

  public void init(String variant, ApolloExtension apolloExtension) {
    this.variant = variant;
    this.apolloExtension = apolloExtension;
    nullableValueType = apolloExtension.getNullableValueType() == null
        ? NullableValueType.ANNOTATED
        : NullableValueType.Companion.findByValue(apolloExtension.getNullableValueType());
    outputDir = new File(getProject().getBuildDir() + File.separator + Joiner.on(File.separator).join(GraphQLCompiler.Companion
        .getOUTPUT_DIRECTORY()));
  }

  @TaskAction
  void generateClasses(IncrementalTaskInputs inputs) {
    inputs.outOfDate(new Action<InputFileDetails>() {
      @Override
      public void execute(InputFileDetails inputFileDetails) {
        GraphQLCompiler.Arguments args = new GraphQLCompiler.Arguments(inputFileDetails.getFile(), outputDir,
            apolloExtension.getCustomTypeMapping(), nullableValueType, apolloExtension.isUseSemanticNaming(),
            apolloExtension.isGenerateModelBuilder(), apolloExtension.isUseJavaBeansSemanticNaming(), apolloExtension
            .getOutputPackageName());
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

  public NullableValueType getNullableValueType() {
    return nullableValueType;
  }

  public void setNullableValueType(NullableValueType nullableValueType) {
    this.nullableValueType = nullableValueType;
  }

  public ApolloExtension getApolloExtension() {
    return apolloExtension;
  }

  public void setApolloExtension(ApolloExtension apolloExtension) {
    this.apolloExtension = apolloExtension;
  }
}
