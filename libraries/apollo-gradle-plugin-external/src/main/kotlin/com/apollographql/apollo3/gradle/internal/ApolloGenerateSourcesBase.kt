package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.CodegenSchema
import com.apollographql.apollo3.compiler.CommonCodegenOptions
import com.apollographql.apollo3.compiler.CodegenMetadata
import com.apollographql.apollo3.compiler.JavaCodegenOptions
import com.apollographql.apollo3.compiler.JavaNullable
import com.apollographql.apollo3.compiler.KotlinCodegenOptions
import com.apollographql.apollo3.compiler.OperationOutputGenerator
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.TargetLanguage
import com.apollographql.apollo3.compiler.UsedCoordinates
import com.apollographql.apollo3.compiler.defaultAddJvmOverloads
import com.apollographql.apollo3.compiler.defaultClassesForEnumsMatching
import com.apollographql.apollo3.compiler.defaultGenerateFilterNotNull
import com.apollographql.apollo3.compiler.defaultGenerateFragmentImplementations
import com.apollographql.apollo3.compiler.defaultGenerateModelBuilders
import com.apollographql.apollo3.compiler.defaultGeneratePrimitiveTypes
import com.apollographql.apollo3.compiler.defaultGenerateQueryDocument
import com.apollographql.apollo3.compiler.defaultGenerateResponseFields
import com.apollographql.apollo3.compiler.defaultGenerateSchema
import com.apollographql.apollo3.compiler.defaultGeneratedSchemaName
import com.apollographql.apollo3.compiler.defaultNullableFieldStyle
import com.apollographql.apollo3.compiler.defaultRequiresOptInAnnotation
import com.apollographql.apollo3.compiler.defaultSealedClassesForEnumsMatching
import com.apollographql.apollo3.compiler.defaultUseSemanticNaming
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerJavaHooks
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerKotlinHooks
import com.apollographql.apollo3.compiler.ir.IrOperations
import com.apollographql.apollo3.compiler.mergeWith
import com.apollographql.apollo3.compiler.schemaTypes
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile

@Suppress("UnstableApiUsage")
abstract class ApolloGenerateSourcesBase : DefaultTask() {
  @get:OutputFile
  @get:Optional
  abstract val operationOutputFile: RegularFileProperty

  @get:Internal
  lateinit var operationOutputGenerator: OperationOutputGenerator

  @Input
  fun getOperationOutputGeneratorVersion() = operationOutputGenerator.version

  @get:Internal
  lateinit var packageNameGenerator: PackageNameGenerator

  @Input
  fun getPackageNameGeneratorVersion() = packageNameGenerator.version

  @get:Input
  @get:Optional
  abstract val useSemanticNaming: Property<Boolean>


  @get:Input
  @get:Optional
  abstract val generateModelBuilders: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generateQueryDocument: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generateSchema: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generatedSchemaName: Property<String>

  @get:Input
  @get:Optional
  abstract val generateResponseFields: Property<Boolean>

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @get:Input
  @get:Optional
  abstract val generateFilterNotNull: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val generateFragmentImplementations: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val sealedClassesForEnumsMatching: ListProperty<String>

  @get:Input
  @get:Optional
  abstract val classesForEnumsMatching: ListProperty<String>

  @get:Input
  @get:Optional
  abstract val generateOptionalOperationVariables: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val addJvmOverloads: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val requiresOptInAnnotation: Property<String>

  @get:Input
  @get:Optional
  abstract val generatePrimitiveTypes: Property<Boolean>

  @get:Input
  @get:Optional
  abstract val nullableFieldStyle: Property<JavaNullable>

  @get:Internal
  lateinit var compilerKotlinHooks: ApolloCompilerKotlinHooks

  @Input
  fun getCompilerKotlinHooksVersion() = compilerKotlinHooks.version

  @get:Internal
  lateinit var compilerJavaHooks: ApolloCompilerJavaHooks

  @Input
  fun getCompilerJavaHooksVersion() = compilerJavaHooks.version

  fun runCodegen(
      codegenSchema: CodegenSchema,
      irOperations: IrOperations,
      usedCoordinates: UsedCoordinates?,
      upstreamMetadata: List<CodegenMetadata>,
  ): CodegenMetadata {

    val operationOutput = ApolloCompiler.buildOperationOutput(
        ir = irOperations,
        operationOutputFile = operationOutputFile.asFile.orNull,
        operationOutputGenerator = operationOutputGenerator,
    )

    val irSchema = usedCoordinates?.let {
      ApolloCompiler.buildIrSchema(
          codegenSchema = codegenSchema,
          usedFields = it.mergeWith((codegenSchema.scalarMapping.keys + setOf("Int", "Float", "String", "ID", "Boolean")).associateWith { emptySet() }),
          incomingTypes = upstreamMetadata.flatMap { it.schemaTypes() }.toSet()
      )
    }

    val commonCodegenOptions = CommonCodegenOptions(
        codegenSchema = codegenSchema,
        ir = irOperations,
        irSchema = irSchema,
        operationOutput = operationOutput,
        outputDir = outputDir.asFile.get(),
        useSemanticNaming = useSemanticNaming.getOrElse(defaultUseSemanticNaming),
        packageNameGenerator = packageNameGenerator,
        generateFragmentImplementations = generateFragmentImplementations.getOrElse(defaultGenerateFragmentImplementations),
        generateQueryDocument = generateQueryDocument.getOrElse(defaultGenerateQueryDocument),
        generateSchema = generateSchema.getOrElse(defaultGenerateSchema),
        generatedSchemaName = generatedSchemaName.getOrElse(defaultGeneratedSchemaName),
        generateResponseFields = generateResponseFields.getOrElse(defaultGenerateResponseFields),
        incomingCodegenMetadata = upstreamMetadata,
    )

    return when (codegenSchema.targetLanguage) {
      TargetLanguage.JAVA -> {
        val javaCodegenOptions = JavaCodegenOptions(
            nullableFieldStyle = nullableFieldStyle.getOrElse(defaultNullableFieldStyle),
            compilerJavaHooks = compilerJavaHooks,
            generateModelBuilders = generateModelBuilders.getOrElse(defaultGenerateModelBuilders),
            classesForEnumsMatching = classesForEnumsMatching.getOrElse(defaultClassesForEnumsMatching),
            generatePrimitiveTypes = generatePrimitiveTypes.getOrElse(defaultGeneratePrimitiveTypes),

            )
        ApolloCompiler.writeJava(
            commonCodegenOptions = commonCodegenOptions,
            javaCodegenOptions = javaCodegenOptions
        )
      }

      else -> {
        val kotlinCodegenOptions = KotlinCodegenOptions(
            generateAsInternal = false,
            generateFilterNotNull = generateFilterNotNull.getOrElse(defaultGenerateFilterNotNull),
            sealedClassesForEnumsMatching = sealedClassesForEnumsMatching.getOrElse(defaultSealedClassesForEnumsMatching),
            addJvmOverloads = addJvmOverloads.getOrElse(defaultAddJvmOverloads),
            requiresOptInAnnotation = requiresOptInAnnotation.getOrElse(defaultRequiresOptInAnnotation),
            compilerKotlinHooks = compilerKotlinHooks,
            languageVersion = codegenSchema.targetLanguage
        )
        ApolloCompiler.writeKotlin(
            commonCodegenOptions = commonCodegenOptions,
            kotlinCodegenOptions = kotlinCodegenOptions
        )
      }
    }
  }
}