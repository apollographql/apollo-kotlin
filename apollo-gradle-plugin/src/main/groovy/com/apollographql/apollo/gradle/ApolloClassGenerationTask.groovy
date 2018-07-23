package com.apollographql.apollo.gradle

import com.apollographql.apollo.compiler.GraphQLCompiler
import com.apollographql.apollo.compiler.NullableValueType
import com.google.common.base.Joiner
import org.gradle.api.Action
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails
import org.jetbrains.annotations.NotNull

class ApolloClassGenerationTask extends SourceTask {
  static final String NAME = "generate%sApolloClasses"

  @Input Property<Map> customTypeMapping = project.objects.property(Map.class)
  @Optional @Input Property<String> nullableValueType = project.objects.property(String.class)
  @Input Property<Boolean> useSemanticNaming = project.objects.property(Boolean.class)
  @Input Property<Boolean> generateModelBuilder = project.objects.property(Boolean.class)
  @Input Property<Boolean> useJavaBeansSemanticNaming = project.objects.property(Boolean.class)
  @Input Property<Boolean> suppressRawTypesWarning = project.objects.property(Boolean.class)
  @Optional @Input Property<String> outputPackageName = project.objects.property(String.class)
  @OutputDirectory DirectoryProperty outputDir

  ApolloClassGenerationTask() {
    outputDir = project.layout.directoryProperty()
    outputDir.set(new File(project.buildDir, Joiner.on(File.separator).join(GraphQLCompiler.OUTPUT_DIRECTORY)))
  }

  @TaskAction
  void generateClasses(IncrementalTaskInputs inputs) {
    String nullableValueTypeStr = this.nullableValueType.get()
    NullableValueType nullableValueType = null
    if (nullableValueTypeStr != null) {
      nullableValueType = NullableValueType.findByValue(nullableValueTypeStr)
    }
    inputs.outOfDate(new Action<InputFileDetails>() {
      @Override
      void execute(@NotNull InputFileDetails inputFileDetails) {
        outputDir.asFile.get().delete()

        String outputPackageName = outputPackageName.get()
        if (outputPackageName != null && outputPackageName.trim().isEmpty()) {
          outputPackageName = null
        }
        GraphQLCompiler.Arguments args = new GraphQLCompiler.Arguments(
            inputFileDetails.getFile(), outputDir.get().asFile, customTypeMapping.get(),
            nullableValueType != null ? nullableValueType : NullableValueType.ANNOTATED, useSemanticNaming.get(),
            generateModelBuilder.get(), useJavaBeansSemanticNaming.get(), outputPackageName,
            suppressRawTypesWarning.get()
        )
        new GraphQLCompiler().write(args)
      }
    })
  }
}
