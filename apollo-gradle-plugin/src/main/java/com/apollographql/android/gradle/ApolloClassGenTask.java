package com.apollographql.android.gradle;

import com.google.common.base.Joiner;

import com.apollographql.android.compiler.GraphQLCompiler;

import org.gradle.api.Action;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.api.tasks.incremental.InputFileDetails;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ApolloClassGenTask extends SourceTask {
  static final String NAME = "generate%sApolloClasses";

  @Internal private String variant;
  @Internal private Map<String, String> customTypeMapping;
  @OutputDirectory private File outputDir;

  public void init(String buildVariant, Map<String, String> typeMapping) {
    variant = buildVariant;
    customTypeMapping = typeMapping;
    outputDir = new File(getProject().getBuildDir() + "/" + Joiner.on(File.separator).join(GraphQLCompiler.Companion
        .getOUTPUT_DIRECTORY()));
  }

  @TaskAction
  void generateClasses(IncrementalTaskInputs inputs) {
    inputs.outOfDate(new Action<InputFileDetails>() {
      @Override
      public void execute(InputFileDetails inputFileDetails) {
        new GraphQLCompiler().write(inputFileDetails.getFile(), outputDir, customTypeMapping);
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
}
