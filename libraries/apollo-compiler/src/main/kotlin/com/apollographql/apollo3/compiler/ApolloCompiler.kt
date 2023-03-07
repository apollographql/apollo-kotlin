package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.GQLDefinition
import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.GQLScalarTypeDefinition
import com.apollographql.apollo3.ast.GQLSchemaDefinition
import com.apollographql.apollo3.ast.GQLTypeDefinition
import com.apollographql.apollo3.ast.Issue
import com.apollographql.apollo3.ast.QueryDocumentMinifier
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.checkKeyFields
import com.apollographql.apollo3.ast.checkNoErrors
import com.apollographql.apollo3.ast.introspection.toSchemaGQLDocument
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.transformation.addRequiredFields
import com.apollographql.apollo3.ast.validateAsExecutable
import com.apollographql.apollo3.ast.validateAsSchemaAndAddApolloDefinition
import com.apollographql.apollo3.compiler.codegen.java.JavaCodeGen
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinCodeGen
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerJavaHooks
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerKotlinHooks
import com.apollographql.apollo3.compiler.ir.DefaultIrOperations
import com.apollographql.apollo3.compiler.ir.IrOperations
import com.apollographql.apollo3.compiler.ir.IrOperationsBuilder
import com.apollographql.apollo3.compiler.ir.IrSchema
import com.apollographql.apollo3.compiler.ir.IrSchemaBuilder
import com.apollographql.apollo3.compiler.ir.toIrOperations
import com.apollographql.apollo3.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
import com.apollographql.apollo3.compiler.operationoutput.writeTo
import okio.buffer
import okio.source
import java.io.File

@ApolloExperimental
object ApolloCompiler {

  interface Logger {
    fun warning(message: String)
  }

  fun buildCodegenSchema(
      schemaFiles: Iterable<File>,
      logger: Logger,
      packageNameGenerator: PackageNameGenerator,
      scalarMapping: Map<String, ScalarInfo>,
      codegenModels: String,
      targetLanguage: TargetLanguage,
      generateDataBuilders: Boolean,
  ): CodegenSchema {

    val schemaDocuments = schemaFiles.map {
      it.toSchemaGQLDocument()
    }

    // Locate the mainSchemaDocument. It's the one that contains the operation roots
    val mainSchemaDocuments = schemaDocuments.filter {
      it.definitions.filterIsInstance<GQLSchemaDefinition>().isNotEmpty()
          || it.definitions.filterIsInstance<GQLTypeDefinition>().any { it.name == "Query" }
    }

    if (mainSchemaDocuments.size > 1) {
      error("Multiple schemas found:\n${mainSchemaDocuments.map { it.filePath }.joinToString("\n")}\n" +
          "Use different services for different schemas")
    } else if (mainSchemaDocuments.isEmpty()) {
      error("Schema(s) found:\n${schemaFiles.map { it.absolutePath }.joinToString("\n")}\n" +
          "But none of them contain type definitions.")
    }
    val mainSchemaDocument = mainSchemaDocuments.single()

    val schemaDefinitions = schemaDocuments.flatMap { it.definitions }
    val schemaDocument = GQLDocument(
        definitions = schemaDefinitions,
        filePath = null
    )

    /**
     * TODO: use `validateAsSchema` to not automatically add the apollo definitions
     */
    val result = schemaDocument.validateAsSchemaAndAddApolloDefinition()

    result.issues.filter { it.severity == Issue.Severity.WARNING }.forEach {
      // Using this format, IntelliJ will parse the warning and display it in the 'run' panel
      logger.warning("w: ${it.sourceLocation.pretty()}: Apollo: ${it.message}")
    }

    val schema = result.getOrThrow()

    checkCustomScalars(schema, scalarMapping)
    return CodegenSchema(
        schema = schema,
        packageName = packageNameGenerator.packageName(mainSchemaDocument.filePath!!),
        codegenModels = codegenModels,
        scalarMapping = scalarMapping,
        targetLanguage = targetLanguage,
        generateDataBuilders = generateDataBuilders
    )
  }

  /**
   * Parses the given files. Throws if there are parsing errors
   */
  private fun Collection<File>.definitions(): List<GQLDefinition> {
    val definitions = mutableListOf<GQLDefinition>()
    val parseIssues = mutableListOf<Issue>()
    map { file ->
      val parseResult = file.source().buffer().parseAsGQLDocument(file.path)
      if (parseResult.issues.isNotEmpty()) {
        parseIssues.addAll(parseResult.issues)
      } else {
        // We can force cast here because we're guaranteed the parsing step will produce either issues
        // or a value
        definitions.addAll(parseResult.value!!.definitions)
      }
    }
    // Parsing issues are fatal
    parseIssues.checkNoErrors()

    return definitions
  }

  fun buildIrOperations(
      options: IrOptions,
  ): IrOperations {
    val executableFiles = options.executableFiles
    val schema = options.codegenSchema.schema

    /**
     * Step 1: parse the documents
     */
    val definitions = executableFiles.definitions()

    val incomingFragments = options.incomingFragments

    /**
     * Step 2, GraphQL validation
     */
    val validationResult = GQLDocument(
        definitions = definitions + incomingFragments,
        filePath = null
    ).validateAsExecutable(schema, options.fieldsOnDisjointTypesMustMerge)

    validationResult.issues.checkNoErrors()

    val codegenModels = options.codegenSchema.codegenModels
    if (codegenModels == MODELS_RESPONSE_BASED || codegenModels == MODELS_OPERATION_BASED_WITH_INTERFACES) {
      checkConditionalFragments(definitions).checkNoErrors()
    }

    checkApolloReservedEnumValueNames(schema).checkNoErrors()
    checkApolloTargetNameClashes(schema).checkNoErrors()

    if (!options.decapitalizeFields) {
      // When flattenModels is true, we still must check capitalized fields inside fragment spreads
      checkCapitalizedFields(definitions, checkFragmentsOnly = options.flattenModels).checkNoErrors()
    }

    val warnings = validationResult.issues.filter {
      it.severity == Issue.Severity.WARNING && (it !is Issue.DeprecatedUsage || options.warnOnDeprecatedUsages)
    }

    warnings.forEach {
      // Using this format, IntelliJ will parse the warning and display it in the 'run' panel
      options.logger.warning("w: ${it.sourceLocation.pretty()}: Apollo: ${it.message}")
    }
    if (options.failOnWarnings && warnings.isNotEmpty()) {
      throw IllegalStateException("Apollo: Warnings found and 'failOnWarnings' is true, aborting.")
    }

    /**
     * Step 3, Modify the AST to add typename and key fields
     */
    val fragmentDefinitions = (definitions.filterIsInstance<GQLFragmentDefinition>() + incomingFragments).associateBy { it.name }
    val fragments = definitions.filterIsInstance<GQLFragmentDefinition>().map {
      addRequiredFields(it, options.addTypename, schema, fragmentDefinitions)
    }

    val operations = definitions.filterIsInstance<GQLOperationDefinition>().map {
      addRequiredFields(it, options.addTypename, schema, fragmentDefinitions)
    }

    // Remember the fragments with the possibly updated fragments
    val allFragmentDefinitions = (fragments + incomingFragments).associateBy { it.name }

    // Check if all the key fields are present in operations and fragments
    // (do this only if there are key fields as it may be costly)
    if (schema.hasTypeWithTypePolicy()) {
      operations.forEach {
        checkKeyFields(it, schema, allFragmentDefinitions)
      }
      fragments.forEach {
        checkKeyFields(it, schema, allFragmentDefinitions)
      }
    }

    /**
     * Build the IR
     */
    return IrOperationsBuilder(
        schema = schema,
        operationDefinitions = operations,
        fragmentDefinitions = fragments,
        allFragmentDefinitions = allFragmentDefinitions,
        codegenModels = codegenModels,
        generateOptionalOperationVariables = options.generateOptionalOperationVariables,
        fieldsOnDisjointTypesMustMerge = options.fieldsOnDisjointTypesMustMerge,
        flattenModels = options.flattenModels,
        decapitalizeFields = options.decapitalizeFields,
        alwaysGenerateTypesMatching = options.alwaysGenerateTypesMatching,
        generateDataBuilders = options.codegenSchema.generateDataBuilders
    ).build()
  }

  fun buildUsedCoordinates(irOperations: IrOperations): UsedCoordinates {
    return irOperations.usedFields
  }

  fun buildUsedCoordinates(irOperationsFiles: List<File>): UsedCoordinates {
    val irOperations = irOperationsFiles.map { it.toIrOperations() }

    val duplicates = irOperations.flatMapIndexed { index, irOperations1 ->
      irOperations1.fragmentDefinitions.map { it.name to irOperationsFiles.get(index).path }
    }.groupBy { it.first }.mapValues { it.value.map { it.second } }
        .entries
        .filter { it.value.size > 1 }
        .flatMap { entry ->
          entry.value.map {
            "- ${entry.key}: $it"
          }
        }

    if (duplicates.isNotEmpty()) {
      error("Apollo: duplicate fragments:\n${duplicates.joinToString("\n")}")
    }

    return irOperations.fold(emptyMap()) { acc, irOperations1 ->
      acc.mergeWith(irOperations1.usedFields)
    }
  }

  fun buildIrSchema(
      codegenSchema: CodegenSchema,
      usedFields: Map<String, Set<String>>,
      incomingTypes: Set<String>,
  ): IrSchema {
    return IrSchemaBuilder.build(codegenSchema.schema, usedFields, incomingTypes)
  }

  fun buildOperationOutput(
      ir: IrOperations,
      operationOutputGenerator: OperationOutputGenerator,
      operationOutputFile: File?,
  ): OperationOutput {
    check(ir is DefaultIrOperations)


    val operationOutput = ir.operations.map {
      OperationDescriptor(
          name = it.name,
          source = QueryDocumentMinifier.minify(it.sourceWithFragments)
      )
    }.let {
      operationOutputGenerator.generate(it)
    }

    check(operationOutput.size == ir.operations.size) {
      """The number of operation IDs (${operationOutput.size}) should match the number of operations (${ir.operations.size}).
        |Check that all your IDs are unique.
      """.trimMargin()
    }

    if (operationOutputFile != null) {
      operationOutput.writeTo(operationOutputFile)
    }

    return operationOutput
  }

  private fun checkCustomScalars(schema: Schema, scalarMapping: Map<String, ScalarInfo>) {
    /**
     * Generate the mapping for all custom scalars
     *
     * If the user specified a mapping, use it, else fallback to [Any]
     */
    val schemaScalars = schema
        .typeDefinitions
        .values
        .filterIsInstance<GQLScalarTypeDefinition>()
        .map { type -> type.name }
        .toSet()
    val unknownScalars = scalarMapping.keys.subtract(schemaScalars)
    check(unknownScalars.isEmpty()) {
      "Apollo: unknown scalar(s): ${unknownScalars.joinToString(",")}"
    }
  }

  private fun codegenSetup(commonOptions: CommonCodegenOptions) {
    commonOptions.outputDir.deleteRecursively()
    commonOptions.outputDir.mkdirs()
  }

  fun writeJava(
      commonCodegenOptions: CommonCodegenOptions,
      javaCodegenOptions: JavaCodegenOptions,
  ): CodegenMetadata {
    val ir = commonCodegenOptions.ir
    val codegenModels = commonCodegenOptions.codegenSchema.codegenModels
    check(ir is DefaultIrOperations)

    codegenSetup(commonCodegenOptions)

    if (codegenModels != MODELS_OPERATION_BASED) {
      error("Java codegen does not support ${codegenModels}. Only $MODELS_OPERATION_BASED is supported.")
    }
    if (!ir.flattenModels) {
      error("Java codegen does not support nested models as it could trigger name clashes when a nested class has the same name as an " +
          "enclosing one.")
    }

    return CodegenMetadata(
        JavaCodeGen(
            commonCodegenOptions = commonCodegenOptions,
            javaCodegenOptions = javaCodegenOptions,
        ).write(outputDir = commonCodegenOptions.outputDir)
    )
  }


  fun writeKotlin(
      commonCodegenOptions: CommonCodegenOptions,
      kotlinCodegenOptions: KotlinCodegenOptions,
  ): CodegenMetadata {
    codegenSetup(commonCodegenOptions)

    return CodegenMetadata(
        KotlinCodeGen.write(
            commonCodegenOptions = commonCodegenOptions,
            kotlinCodegenOptions = kotlinCodegenOptions,
        )
    )
  }

  fun writeSimple(
      schema: Schema,
      executableFiles: Set<File>,
      outputDir: File,
      packageNameGenerator: PackageNameGenerator,
      schemaPackageName: String,
      warnOnDeprecatedUsages: Boolean = defaultWarnOnDeprecatedUsages,
      failOnWarnings: Boolean = defaultFailOnWarnings,
      logger: Logger = defaultLogger,
      flattenModels: Boolean = defaultFlattenModels,
      codegenModels: String = defaultCodegenModels,
      addTypename: String = defaultAddTypename,
      decapitalizeFields: Boolean = defaultDecapitalizeFields,
      fieldsOnDisjointTypesMustMerge: Boolean = defaultFieldsOnDisjointTypesMustMerge,
      generateOptionalOperationVariables: Boolean = defaultGenerateOptionalOperationVariables,
      alwaysGenerateTypesMatching: Set<String> = defaultAlwaysGenerateTypesMatching,
      operationOutputGenerator: OperationOutputGenerator = defaultOperationOutputGenerator,
      useSemanticNaming: Boolean = defaultUseSemanticNaming,
      generateFragmentImplementations: Boolean = defaultGenerateFragmentImplementations,
      generateQueryDocument: Boolean = defaultGenerateQueryDocument,
      generateSchema: Boolean = defaultGenerateSchema,
      generatedSchemaName: String = defaultGeneratedSchemaName,
      generateResponseFields: Boolean = defaultGenerateResponseFields,
      scalarMapping: Map<String, ScalarInfo> = defaultScalarMapping,
      generateDataBuilders: Boolean = defaultGenerateDataBuilders,
      targetLanguage: TargetLanguage = defaultTargetLanguage,
      // Java
      nullableFieldStyle: JavaNullable = defaultNullableFieldStyle,
      compilerJavaHooks: ApolloCompilerJavaHooks = defaultCompilerJavaHooks,
      generateModelBuilders: Boolean = defaultGenerateModelBuilders,
      classesForEnumsMatching: List<String> = defaultClassesForEnumsMatching,
      generatePrimitiveTypes: Boolean = defaultGeneratePrimitiveTypes,
      // Kotlin
      generateAsInternal: Boolean = defaultGenerateAsInternal,
      generateFilterNotNull: Boolean = defaultGenerateFilterNotNull,
      sealedClassesForEnumsMatching: List<String> = defaultClassesForEnumsMatching,
      addJvmOverloads: Boolean = defaultAddJvmOverloads,
      requiresOptInAnnotation: String = defaultRequiresOptInAnnotation,
      compilerKotlinHooks: ApolloCompilerKotlinHooks = defaultCompilerKotlinHooks,
  ): CodegenMetadata {
    /**
     * Inject all built-in scalars
     * I think these are always needed
     * And custom scalars specified in the Gradle configuration
     * These are always needed by the user to register their adapters
     */
    @Suppress("NAME_SHADOWING")
    val alwaysGenerateTypesMatching = alwaysGenerateTypesMatching +
        scalarMapping.keys +
        setOf("String", "Boolean", "Int", "Float", "ID")

    val irOptions = IrOptions(
        codegenSchema = CodegenSchema(
            schema = schema,
            packageName = schemaPackageName,
            codegenModels = codegenModels,
            scalarMapping = scalarMapping,
            targetLanguage = targetLanguage,
            generateDataBuilders = generateDataBuilders,
        ),
        executableFiles = executableFiles,
        warnOnDeprecatedUsages = warnOnDeprecatedUsages,
        failOnWarnings = failOnWarnings,
        logger = logger,
        flattenModels = flattenModels,
        incomingFragments = emptyList(),
        addTypename = addTypename,
        decapitalizeFields = decapitalizeFields,
        fieldsOnDisjointTypesMustMerge = fieldsOnDisjointTypesMustMerge,
        generateOptionalOperationVariables = generateOptionalOperationVariables,
        alwaysGenerateTypesMatching = alwaysGenerateTypesMatching,
    )

    val irOperations = buildIrOperations(irOptions)

    val operationOutput = buildOperationOutput(
        ir = irOperations,
        operationOutputFile = null,
        operationOutputGenerator = operationOutputGenerator,
    )

    val irSchema = IrSchemaBuilder.build(
        schema = irOptions.codegenSchema.schema,
        usedFields = irOperations.usedFields,
        incomingTypes = emptySet(),
    )

    val commonCodegenOptions = CommonCodegenOptions(
        codegenSchema = irOptions.codegenSchema,
        ir = irOperations,
        irSchema = irSchema,
        operationOutput = operationOutput,
        outputDir = outputDir,
        useSemanticNaming = useSemanticNaming,
        packageNameGenerator = packageNameGenerator,
        generateFragmentImplementations = generateFragmentImplementations,
        generateQueryDocument = generateQueryDocument,
        generateSchema = generateSchema,
        generatedSchemaName = generatedSchemaName,
        generateResponseFields = generateResponseFields,
        incomingCodegenMetadata = emptyList(),
    )

    return when (targetLanguage) {
      TargetLanguage.JAVA -> {
        val javaCodegenOptions = JavaCodegenOptions(
            nullableFieldStyle = nullableFieldStyle,
            compilerJavaHooks = compilerJavaHooks,
            generateModelBuilders = generateModelBuilders,
            classesForEnumsMatching = classesForEnumsMatching,
            generatePrimitiveTypes = generatePrimitiveTypes,

            )
        writeJava(
            commonCodegenOptions = commonCodegenOptions,
            javaCodegenOptions = javaCodegenOptions
        )
      }

      else -> {
        val kotlinCodegenOptions = KotlinCodegenOptions(
            generateAsInternal = generateAsInternal,
            generateFilterNotNull = generateFilterNotNull,
            sealedClassesForEnumsMatching = sealedClassesForEnumsMatching,
            addJvmOverloads = addJvmOverloads,
            requiresOptInAnnotation = requiresOptInAnnotation,
            compilerKotlinHooks = compilerKotlinHooks,
            languageVersion = targetLanguage
        )
        writeKotlin(
            commonCodegenOptions = commonCodegenOptions,
            kotlinCodegenOptions = kotlinCodegenOptions
        )
      }
    }
  }
}
