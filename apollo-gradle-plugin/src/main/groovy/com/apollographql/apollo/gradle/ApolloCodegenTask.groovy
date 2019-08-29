package com.apollographql.apollo.gradle

import com.apollographql.apollo.compiler.GraphQLCompiler
import com.apollographql.apollo.compiler.InflectorKt
import com.apollographql.apollo.compiler.NullableValueType
import com.apollographql.apollo.compiler.ir.CodeGenerationIR
import com.apollographql.apollo.compiler.parser.GraphQLDocumentParser
import com.apollographql.apollo.compiler.parser.Schema
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@CacheableTask
class ApolloCodegenTask extends SourceTask {
  static final String NAME = "generate%sApolloClasses"

  @Input Property<String> variant = project.objects.property(String.class)
  @Input ListProperty<String> sourceSetNames = project.objects.listProperty(String.class)
  @Input @Optional Property<String> schemaFilePath = project.objects.property(String.class)
  @Input @Optional Property<String> outputPackageName = project.objects.property(String.class)
  @OutputDirectory DirectoryProperty outputDir = project.objects.directoryProperty()
  @Input MapProperty<String, String> customTypeMapping = project.objects.mapProperty(String.class, String.class)
  @Optional @Input Property<String> nullableValueType = project.objects.property(String.class)
  @Input Property<Boolean> useSemanticNaming = project.objects.property(Boolean.class)
  @Input Property<Boolean> generateModelBuilder = project.objects.property(Boolean.class)
  @Input Property<Boolean> useJavaBeansSemanticNaming = project.objects.property(Boolean.class)
  @Input Property<Boolean> suppressRawTypesWarning = project.objects.property(Boolean.class)
  @Input Property<Boolean> generateKotlinModels = project.objects.property(Boolean.class)
  @Input Property<Boolean> generateVisitorForPolymorphicDatatypes = project.objects.property(Boolean.class)
  @Input ListProperty<String> excludeFiles = project.objects.listProperty(String.class)

  @TaskAction
  void generateClasses() {
    exclude(excludeFiles.get())

    String nullableValueTypeStr = this.nullableValueType.get()
    NullableValueType nullableValueType = (nullableValueTypeStr != null) ?
        NullableValueType.values().find { it.value == nullableValueTypeStr } : null

    List<String> sourceSets = sourceSetNames.get().collect { it.toString() }
    List<CodegenGenerationTaskCommandArgsBuilder.ApolloCodegenArgs> codegenArgs = new CodegenGenerationTaskCommandArgsBuilder(
        this, schemaFilePath.get(), outputPackageName.get(), outputDir.get().asFile, variant.get(), sourceSets
    ).buildCodegenArgs()

    outputDir.asFile.get().delete()

    for (CodegenGenerationTaskCommandArgsBuilder.ApolloCodegenArgs codegenArg : codegenArgs) {
      Schema schema = Schema.parse(codegenArg.schemaFile)
      CodeGenerationIR codeGenerationIR = new GraphQLDocumentParser(schema).parse(codegenArg.queryFilePaths.toList().collect { new File(it) })
      String outputPackageName = outputPackageName.get()
      if (outputPackageName != null && outputPackageName.trim().isEmpty()) {
        outputPackageName = null
      }
      String irPackageName =  InflectorKt.formatPackageName(codegenArg.outputFolder.absolutePath, false)

      GraphQLCompiler.Arguments args = new GraphQLCompiler.Arguments(
          codeGenerationIR, outputDir.get().asFile, customTypeMapping.get(),
          nullableValueType != null ? nullableValueType : NullableValueType.ANNOTATED, useSemanticNaming.get(),
          generateModelBuilder.get(), useJavaBeansSemanticNaming.get(), irPackageName, outputPackageName,
          suppressRawTypesWarning.get(), generateKotlinModels.get(), generateVisitorForPolymorphicDatatypes.get()
      )
      new GraphQLCompiler().write(args)
    }
  }
}
