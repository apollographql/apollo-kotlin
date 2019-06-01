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

@CacheableTask
class ApolloClassGenerationTask extends SourceTask {
  static final String NAME = "generate%sApolloClasses"

  @Input final Property<Map> customTypeMapping = project.objects.property(Map.class)
  @Optional @Input final Property<String> nullableValueType = project.objects.property(String.class)
  @Input final Property<Boolean> useSemanticNaming = project.objects.property(Boolean.class)
  @Input final Property<Boolean> generateModelBuilder = project.objects.property(Boolean.class)
  @Input final Property<Boolean> useJavaBeansSemanticNaming = project.objects.property(Boolean.class)
  @Input final Property<Boolean> suppressRawTypesWarning = project.objects.property(Boolean.class)
  @Input final Property<Boolean> generateKotlinModels = project.objects.property(Boolean.class)
  @Optional @Input final Property<String> outputPackageName = project.objects.property(String.class)
  @OutputDirectory final DirectoryProperty outputDir = project.layout.directoryProperty()

  ApolloClassGenerationTask() {
    outputDir.set(new File(project.buildDir, Joiner.on(File.separator).join(GraphQLCompiler.OUTPUT_DIRECTORY)))
  }

  @TaskAction
  void generateClasses(IncrementalTaskInputs inputs) {
    String nullableValueTypeStr = this.nullableValueType.get()
    NullableValueType nullableValueType = null
    if (nullableValueTypeStr != null) {
      nullableValueType = NullableValueType.values().find { it.value == nullableValueTypeStr }
    }
    inputs.outOfDate(new Action<InputFileDetails>() {
      @Override
      void execute(@NotNull InputFileDetails inputFileDetails) {
        File inputFile = inputFileDetails.getFile()
        if (!inputFile.isFile()) {
          // skip if input is not a file
          return
        }
        outputDir.asFile.get().delete()

        String outputPackageName = outputPackageName.get()
        if (outputPackageName != null && outputPackageName.trim().isEmpty()) {
          outputPackageName = null
        }
        GraphQLCompiler.Arguments args = new GraphQLCompiler.Arguments(
            inputFile, outputDir.get().asFile, customTypeMapping.get(),
            nullableValueType != null ? nullableValueType : NullableValueType.ANNOTATED, useSemanticNaming.get(),
            generateModelBuilder.get(), useJavaBeansSemanticNaming.get(), outputPackageName,
            suppressRawTypesWarning.get(), generateKotlinModels.get()
        )
        new GraphQLCompiler().write(args)
      }
    })
  }
}
