package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.DeprecatedUsage
import com.apollographql.apollo3.ast.DifferentShape
import com.apollographql.apollo3.ast.DirectiveRedefinition
import com.apollographql.apollo3.ast.GQLDefinition
import com.apollographql.apollo3.ast.GQLDirectiveDefinition
import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.GQLScalarTypeDefinition
import com.apollographql.apollo3.ast.GQLSchemaDefinition
import com.apollographql.apollo3.ast.GQLTypeDefinition
import com.apollographql.apollo3.ast.Issue
import com.apollographql.apollo3.ast.ParserOptions
import com.apollographql.apollo3.ast.QueryDocumentMinifier
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.UnknownDirective
import com.apollographql.apollo3.ast.UnusedFragment
import com.apollographql.apollo3.ast.UnusedVariable
import com.apollographql.apollo3.ast.apolloDefinitions
import com.apollographql.apollo3.ast.checkEmpty
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.pretty
import com.apollographql.apollo3.ast.toGQLDocument
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
import com.apollographql.apollo3.compiler.ir.IrTargetObject
import com.apollographql.apollo3.compiler.ir.toIrOperations
import com.apollographql.apollo3.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
import com.apollographql.apollo3.compiler.operationoutput.writeTo
import com.apollographql.apollo3.compiler.pqm.toPersistedQueryManifest
import com.apollographql.apollo3.compiler.pqm.writeTo
import com.squareup.kotlinpoet.FileSpec
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
      it.toGQLDocument(allowJson = true)
    }

    if (schemaDocuments.isEmpty()) {
      error("No schema found. Apollo needs a `.graphqls` or a `.json` schema.")
    }

    // Locate the mainSchemaDocument. It's the one that contains the operation roots
    val mainSchemaDocuments = mutableListOf<GQLDocument>()
    var otherSchemaDocuments = mutableListOf<GQLDocument>()
    schemaDocuments.forEach {
      if (
          it.definitions.filterIsInstance<GQLSchemaDefinition>().isNotEmpty()
          || it.definitions.filterIsInstance<GQLTypeDefinition>().any { it.name == "Query" }
      ) {
        mainSchemaDocuments.add(it)
      } else {
        otherSchemaDocuments.add(it)
      }
    }

    if (mainSchemaDocuments.size > 1) {
      error("Multiple schemas found:\n${mainSchemaDocuments.map { it.sourceLocation?.filePath }.joinToString("\n")}\n" +
          "Use different services for different schemas")
    } else if (mainSchemaDocuments.isEmpty()) {
      error("Schema(s) found:\n${schemaFiles.map { it.absolutePath }.joinToString("\n")}\n" +
          "But none of them contain type definitions.")
    }
    val mainSchemaDocument = mainSchemaDocuments.single()

    // Sort the other schema document as type extensions are order sensitive
    val otherSchemaDocumentSorted = otherSchemaDocuments.sortedBy { it.sourceLocation?.filePath?.substringAfterLast(File.pathSeparator) }
    val schemaDefinitions = (listOf(mainSchemaDocument) + otherSchemaDocumentSorted).flatMap { it.definitions }
    val schemaDocument = GQLDocument(
        definitions = schemaDefinitions,
        sourceLocation = null
    )

    /**
     * TODO: use `validateAsSchema` to not automatically add the apollo definitions
     */
    val result = schemaDocument.validateAsSchemaAndAddApolloDefinition()

    val issueGroup = result.issues.group(true, true)

    issueGroup.errors.checkEmpty()
    issueGroup.warnings.forEach {
      // Using this format, IntelliJ will parse the warning and display it in the 'run' panel
      logger.warning("w: ${it.sourceLocation.pretty()}: Apollo: ${it.message}")
    }

    val schema = result.value!!

    checkScalars(schema, scalarMapping)
    return CodegenSchema(
        schema = schema,
        packageName = packageNameGenerator.packageName(mainSchemaDocument.sourceLocation?.filePath!!),
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
      val parseResult = file.parseAsGQLDocument(options = ParserOptions.Builder().build())
      if (parseResult.issues.isNotEmpty()) {
        parseIssues.addAll(parseResult.issues)
      } else {
        // We can force cast here because we're guaranteed the parsing step will produce either issues
        // or a value
        definitions.addAll(parseResult.value!!.definitions)
      }
    }

    // Parsing issues are fatal
    parseIssues.checkEmpty()

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
        sourceLocation = null
    ).validateAsExecutable(schema)

    val allIssues = mutableListOf<Issue>()
    allIssues.addAll(validationResult.issues)

    val codegenModels = options.codegenSchema.codegenModels
    if (codegenModels == MODELS_RESPONSE_BASED || codegenModels == MODELS_OPERATION_BASED_WITH_INTERFACES) {
      allIssues.addAll(checkConditionalFragments(definitions))
    }

    allIssues.addAll(checkApolloReservedEnumValueNames(schema))
    allIssues.addAll(checkApolloTargetNameClashes(schema))
    allIssues.addAll(checkApolloInlineFragmentsHaveTypeCondition(definitions))

    if (!options.decapitalizeFields) {
      // When flattenModels is true, we still must check capitalized fields inside fragment spreads
      allIssues.addAll(checkCapitalizedFields(definitions, checkFragmentsOnly = options.flattenModels))
    }

    val issueGroup = allIssues.group(
        options.warnOnDeprecatedUsages,
        options.fieldsOnDisjointTypesMustMerge,
    )

    issueGroup.errors.checkEmpty()

    issueGroup.warnings.forEach {
      // Using this format, IntelliJ will parse the warning and display it in the 'run' panel
      options.logger.warning("w: ${it.sourceLocation.pretty()}: Apollo: ${it.message}")
    }
    if (options.failOnWarnings && issueGroup.warnings.isNotEmpty()) {
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
        flattenModels = options.flattenModels,
        decapitalizeFields = options.decapitalizeFields,
        alwaysGenerateTypesMatching = options.alwaysGenerateTypesMatching,
        generateDataBuilders = options.codegenSchema.generateDataBuilders,
        fragmentVariableUsages = validationResult.fragmentVariableUsages
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
      operationManifestFile: File?,
      operationManifestFormat: String,
  ): OperationOutput {
    check(ir is DefaultIrOperations)


    val operationOutput = ir.operations.map {
      OperationDescriptor(
          name = it.name,
          source = QueryDocumentMinifier.minify(it.sourceWithFragments),
          type = it.operationType.name.lowercase()
      )
    }.let {
      operationOutputGenerator.generate(it)
    }

    check(operationOutput.size == ir.operations.size) {
      """The number of operation IDs (${operationOutput.size}) should match the number of operations (${ir.operations.size}).
        |Check that all your IDs are unique.
      """.trimMargin()
    }

    if (operationManifestFormat != MANIFEST_NONE) {
      check(operationManifestFile != null) {
        "Apollo: $operationManifestFormat requires a manifest file"
      }
    }
    when (operationManifestFormat) {
      MANIFEST_OPERATION_OUTPUT -> operationOutput.writeTo(operationManifestFile!!)
      MANIFEST_PERSISTED_QUERY -> operationOutput.toPersistedQueryManifest().writeTo(operationManifestFile!!)
    }
    return operationOutput
  }

  private fun checkScalars(schema: Schema, scalarMapping: Map<String, ScalarInfo>) {
    /**
     * Generate the mapping for all scalars
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

  fun schemaFileSpecs(
      codegenSchema: CodegenSchema,
      packageName: String,
  ): Pair<CodegenMetadata, List<FileSpec>> {
    return KotlinCodeGen.schemaFileSpecs(codegenSchema, packageName)
  }

  fun resolverFileSpecs(
      codegenSchema: CodegenSchema,
      codegenMetadata: CodegenMetadata,
      irTargetObjects: List<IrTargetObject>,
      packageName: String,
      serviceName: String,
  ): List<FileSpec> {
    return KotlinCodeGen.resolverFileSpecs(codegenSchema, codegenMetadata, irTargetObjects, packageName = packageName, serviceName = serviceName)
  }

  fun writeKotlin(
      commonCodegenOptions: CommonCodegenOptions,
      kotlinCodegenOptions: KotlinCodegenOptions,
  ): CodegenMetadata {
    codegenSetup(commonCodegenOptions)

    return CodegenMetadata(
        KotlinCodeGen.writeOperations(
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
      generateMethods: List<GeneratedMethod> = defaultGenerateMethodsKotlin,
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
      generateInputBuilders: Boolean = false
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
        operationOutputGenerator = operationOutputGenerator,
        operationManifestFile = null,
        operationManifestFormat = MANIFEST_NONE,
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
        generateMethods = generateMethods,
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
            languageVersion = targetLanguage,
            generateInputBuilders = generateInputBuilders
        )
        writeKotlin(
            commonCodegenOptions = commonCodegenOptions,
            kotlinCodegenOptions = kotlinCodegenOptions
        )
      }
    }
  }
}

private enum class Severity {
  None,
  Warning,
  Error
}

internal class IssueGroup(
    val ignored: List<Issue>,
    val warnings: List<Issue>,
    val errors: List<Issue>
)

internal fun List<Issue>.group(warnOnDeprecatedUsages: Boolean,
                              fieldsOnDisjointTypesMustMerge: Boolean,
): IssueGroup {
  val ignored= mutableListOf<Issue>()
  val warnings= mutableListOf<Issue>()
  val errors= mutableListOf<Issue>()
  val apolloDirectives = apolloDefinitions("v0.1").mapNotNull { (it as? GQLDirectiveDefinition)?.name }.toSet()

  forEach {
    val severity = when (it) {
      is DeprecatedUsage -> if (warnOnDeprecatedUsages) Severity.Warning else Severity.None
      is DifferentShape -> if (fieldsOnDisjointTypesMustMerge) Severity.Error else Severity.Warning
      is UnusedVariable -> Severity.Warning
      is UnusedFragment -> Severity.None
      is UnknownDirective -> Severity.Warning
      /**
       * Because some users might have added the apollo directive to their schema, we just let that through for now
       */
      is DirectiveRedefinition -> if (it.name in apolloDirectives) Severity.None else Severity.Warning
      else -> Severity.Error
    }

    when(severity) {
      Severity.None -> ignored.add(it)
      Severity.Warning -> warnings.add(it)
      Severity.Error -> errors.add(it)
    }
  }

  return IssueGroup(ignored, warnings, errors)
}
