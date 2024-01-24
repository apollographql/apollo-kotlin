package com.apollographql.apollo3.compiler

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
import com.apollographql.apollo3.ast.IncompatibleDefinition
import com.apollographql.apollo3.ast.Issue
import com.apollographql.apollo3.ast.KOTLIN_LABS_VERSION
import com.apollographql.apollo3.ast.ParserOptions
import com.apollographql.apollo3.ast.QueryDocumentMinifier
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.UnknownDirective
import com.apollographql.apollo3.ast.UnusedFragment
import com.apollographql.apollo3.ast.UnusedVariable
import com.apollographql.apollo3.ast.checkEmpty
import com.apollographql.apollo3.ast.kotlinLabsDefinitions
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.pretty
import com.apollographql.apollo3.ast.toGQLDocument
import com.apollographql.apollo3.ast.validateAsExecutable
import com.apollographql.apollo3.ast.validateAsSchemaAndAddApolloDefinition
import com.apollographql.apollo3.compiler.codegen.java.JavaCodeGen
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinCodegen
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerJavaHooks
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerKotlinHooks
import com.apollographql.apollo3.compiler.internal.addRequiredFields
import com.apollographql.apollo3.compiler.internal.checkApolloInlineFragmentsHaveTypeCondition
import com.apollographql.apollo3.compiler.internal.checkApolloReservedEnumValueNames
import com.apollographql.apollo3.compiler.internal.checkApolloTargetNameClashes
import com.apollographql.apollo3.compiler.internal.checkCapitalizedFields
import com.apollographql.apollo3.compiler.internal.checkConditionalFragments
import com.apollographql.apollo3.compiler.internal.checkKeyFields
import com.apollographql.apollo3.compiler.ir.DefaultIrOperations
import com.apollographql.apollo3.compiler.ir.IrOperations
import com.apollographql.apollo3.compiler.ir.IrOperationsBuilder
import com.apollographql.apollo3.compiler.ir.IrSchema
import com.apollographql.apollo3.compiler.ir.IrSchemaBuilder
import com.apollographql.apollo3.compiler.ir.IrTargetObject
import com.apollographql.apollo3.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo3.compiler.pqm.toPersistedQueryManifest
import com.squareup.kotlinpoet.FileSpec
import java.io.File

object ApolloCompiler {
  interface Logger {
    fun warning(message: String)
  }

  fun buildCodegenSchema(
      schemaFiles: Set<File>,
      logger: Logger = defaultLogger,
      codegenSchemaOptionsFile: File,
      codegenSchemaFile: File,
  ) {
    buildCodegenSchema(
        schemaFiles,
        logger,
        codegenSchemaOptionsFile.toCodegenSchemaOptions(),
    ).writeTo(codegenSchemaFile)
  }

  private fun buildCodegenSchema(
      schemaFiles: Set<File>,
      logger: Logger = defaultLogger,
      codegenSchemaOptions: CodegenSchemaOptions,
  ): CodegenSchema {
    val schemaDocuments = schemaFiles.map {
      it.toGQLDocument(allowJson = true)
    }

    if (schemaDocuments.isEmpty()) {
      error("No schema found. Apollo needs a `.graphqls` or a `.json` schema.")
    }

    // Locate the mainSchemaDocument. It's the one that contains the operation roots
    val mainSchemaDocuments = mutableListOf<GQLDocument>()
    val otherSchemaDocuments = mutableListOf<GQLDocument>()
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

    val issueGroup = result.issues.group(warnOnDeprecatedUsages = true, fieldsOnDisjointTypesMustMerge = true)

    issueGroup.errors.checkEmpty()
    issueGroup.warnings.forEach {
      // Using this format, IntelliJ will parse the warning and display it in the 'run' panel
      logger.warning("w: ${it.sourceLocation.pretty()}: Apollo: ${it.message}")
    }

    val schema = result.value!!

    val scalarMapping = codegenSchemaOptions.scalarMapping
    checkScalars(schema, scalarMapping)

    val codegenModels = codegenModels(codegenModels = codegenSchemaOptions.codegenModels, codegenSchemaOptions.targetLanguage)

    val generateDataBuilders = codegenSchemaOptions.generateDataBuilders ?: defaultGenerateDataBuilders
    return CodegenSchema(
        schema = schema,
        filePath = mainSchemaDocument.sourceLocation?.filePath,
        codegenModels = codegenModels,
        scalarMapping = scalarMapping,
        targetLanguage = codegenSchemaOptions.targetLanguage,
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
      codegenSchemaFile: File,
      executableFiles: Set<File>,
      upstreamIrFiles: Set<File>,
      irOptionsFile: File,
      logger: Logger = defaultLogger,
      irOperationsFile: File,
  ) {
    buildIrOperations(
        codegenSchemaFile.toCodegenSchema(),
        executableFiles,
        upstreamIrFiles.flatMap { it.toIrOperations().fragmentDefinitions },
        irOptionsFile.toIrOptions(),
        logger
    ).writeTo(irOperationsFile)
  }

  private fun buildIrOperations(
      codegenSchema: CodegenSchema,
      executableFiles: Set<File>,
      upstreamFragmentDefinitions: List<GQLFragmentDefinition>,
      options: IrOptions = IrOptions(),
      logger: Logger = defaultLogger,
  ): IrOperations {
    val schema = codegenSchema.schema

    /**
     * Step 1: parse the documents
     */
    val definitions = executableFiles.definitions()

    /**
     * Step 2, GraphQL validation
     */
    val validationResult = GQLDocument(
        definitions = definitions + upstreamFragmentDefinitions,
        sourceLocation = null
    ).validateAsExecutable(schema)

    val allIssues = mutableListOf<Issue>()
    allIssues.addAll(validationResult.issues)

    val codegenModels = codegenSchema.codegenModels
    if (codegenModels == MODELS_RESPONSE_BASED || codegenModels == MODELS_OPERATION_BASED_WITH_INTERFACES) {
      allIssues.addAll(checkConditionalFragments(definitions))
    }

    allIssues.addAll(checkApolloReservedEnumValueNames(schema))
    allIssues.addAll(checkApolloTargetNameClashes(schema))
    allIssues.addAll(checkApolloInlineFragmentsHaveTypeCondition(definitions))

    val flattenModels = options.flattenModels ?: flattenModels(codegenModels)
    val decapitalizeFields = options.decapitalizeFields ?: defaultDecapitalizeFields
    val warnOnDeprecatedUsages = options.warnOnDeprecatedUsages ?: defaultWarnOnDeprecatedUsages
    val failOnWarnings = options.failOnWarnings ?: defaultFailOnWarnings
    val fieldsOnDisjointTypesMustMerge = options.fieldsOnDisjointTypesMustMerge ?: defaultFieldsOnDisjointTypesMustMerge
    val addTypename = options.addTypename ?: defaultAddTypename
    val generateOptionalOperationVariables = options.generateOptionalOperationVariables ?: defaultGenerateOptionalOperationVariables
    val alwaysGenerateTypesMatching = options.alwaysGenerateTypesMatching ?: defaultAlwaysGenerateTypesMatching

    if (!decapitalizeFields) {
      // When flattenModels is true, we still must check capitalized fields inside fragment spreads
      allIssues.addAll(checkCapitalizedFields(definitions, checkFragmentsOnly = flattenModels))
    }

    val issueGroup = allIssues.group(
        warnOnDeprecatedUsages,
        fieldsOnDisjointTypesMustMerge,
    )

    issueGroup.errors.checkEmpty()

    issueGroup.warnings.forEach {
      // Using this format, IntelliJ will parse the warning and display it in the 'run' panel
      logger.warning("w: ${it.sourceLocation.pretty()}: Apollo: ${it.message}")
    }

    if (failOnWarnings && issueGroup.warnings.isNotEmpty()) {
      throw IllegalStateException("Apollo: Warnings found and 'failOnWarnings' is true, aborting.")
    }

    /**
     * Step 3, Modify the AST to add typename and key fields
     */
    val fragmentDefinitions = (definitions.filterIsInstance<GQLFragmentDefinition>() + upstreamFragmentDefinitions).associateBy { it.name }
    val fragments = definitions.filterIsInstance<GQLFragmentDefinition>().map {
      addRequiredFields(it, addTypename, schema, fragmentDefinitions)
    }

    val operations = definitions.filterIsInstance<GQLOperationDefinition>().map {
      addRequiredFields(it, addTypename, schema, fragmentDefinitions)
    }

    // Remember the fragments with the possibly updated fragments
    val allFragmentDefinitions = (fragments + upstreamFragmentDefinitions).associateBy { it.name }

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
        generateOptionalOperationVariables = generateOptionalOperationVariables,
        flattenModels = flattenModels,
        decapitalizeFields = decapitalizeFields,
        alwaysGenerateTypesMatching = alwaysGenerateTypesMatching,
        generateDataBuilders = codegenSchema.generateDataBuilders,
        fragmentVariableUsages = validationResult.fragmentVariableUsages
    ).build()
  }

  fun buildIrSchema(
      codegenSchemaFile: File,
      irOperationsFiles: Set<File>,
      irSchemaFile: File,
  ) {
    buildIrSchema(
        codegenSchemaFile.toCodegenSchema(),
        irOperationsFiles.map { it.toIrOperations() },
    ).writeTo(irSchemaFile)
  }

  private fun buildIrSchema(
      codegenSchema: CodegenSchema,
      irOperations: List<IrOperations>,
  ): IrSchema {

    val usedCoordinates = irOperations.fold(emptyMap<String, Set<String>>()) { acc, irOperations1 ->
      acc.mergeWith(irOperations1.usedFields)
    }

    return IrSchemaBuilder.build(
        codegenSchema.schema,
        usedCoordinates.mergeWith((codegenSchema.scalarMapping.keys + setOf("Int", "Float", "String", "ID", "Boolean")).associateWith { emptySet() }),
        emptySet() // We generate everything in the schema module
    )
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

  internal fun File.clearContents() = apply {
    deleteRecursively()
    mkdirs()
  }

  fun buildSchemaAndOperationSources(
      codegenSchemaFile: File,
      irOperationsFile: File,
      irSchemaFile: File? = null,
      upstreamCodegenMetadataFiles: Set<File>,
      codegenOptionsFile: File,
      packageNameGenerator: PackageNameGenerator? = null,
      packageNameRoots: Set<String>? = null,
      compilerKotlinHooks: List<ApolloCompilerKotlinHooks>? = null,
      compilerJavaHooks: List<ApolloCompilerJavaHooks>? = null,
      sourcesDir: File,
      operationManifestFile: File? = null,
      codegenMetadataFile: File? = null,
  ) {
    buildSchemaAndOperationSources(
        codegenSchema = codegenSchemaFile.toCodegenSchema(),
        irOperations = irOperationsFile.toIrOperations(),
        irSchema = irSchemaFile?.toIrSchema(),
        upstreamCodegenMetadata = upstreamCodegenMetadataFiles.map { it.toCodegenMetadata() },
        codegenOptions = codegenOptionsFile.toCodegenOptions(),
        packageNameGenerator = packageNameGenerator,
        packageNameRoots = packageNameRoots,
        compilerKotlinHooks = compilerKotlinHooks,
        compilerJavaHooks = compilerJavaHooks,
        outputDir = sourcesDir,
        operationManifestFile = operationManifestFile,
        codegenMetadataFile = codegenMetadataFile
    )
  }

  private fun buildSchemaAndOperationSources(
      codegenSchema: CodegenSchema,
      irOperations: IrOperations,
      irSchema: IrSchema? = null,
      upstreamCodegenMetadata: List<CodegenMetadata> = emptyList(),
      codegenOptions: CodegenOptions = CodegenOptions(),
      logger: Logger = defaultLogger,
      packageNameGenerator: PackageNameGenerator? = null,
      packageNameRoots: Set<String>? = null,
      operationOutputGenerator: OperationOutputGenerator? = null,
      compilerKotlinHooks: List<ApolloCompilerKotlinHooks>? = null,
      compilerJavaHooks: List<ApolloCompilerJavaHooks>? = null,
      outputDir: File,
      operationManifestFile: File? = null,
      codegenMetadataFile: File? = null,
  ) {

    check(irOperations is DefaultIrOperations)
    val operationOutput = irOperations.operations.map {
      OperationDescriptor(
          name = it.name,
          source = QueryDocumentMinifier.minify(it.sourceWithFragments),
          type = it.operationType.name.lowercase()
      )
    }.let {
      (operationOutputGenerator ?: defaultOperationOutputGenerator).generate(it)
    }

    check(operationOutput.size == irOperations.operations.size) {
      """The number of operation IDs (${operationOutput.size}) should match the number of operations (${irOperations.operations.size}).
        |Check that all your IDs are unique.
      """.trimMargin()
    }

    val operationManifestFormat = codegenOptions.common.operationManifestFormat
    if ((operationManifestFormat ?: defaultOperationManifestFormat) != MANIFEST_NONE) {
      check(operationManifestFile != null) {
        "Apollo: no operationManifestFile set to output '$operationManifestFormat' operation manifest"
      }
      when (operationManifestFormat) {
        MANIFEST_OPERATION_OUTPUT -> operationOutput.writeTo(operationManifestFile)
        MANIFEST_PERSISTED_QUERY -> operationOutput.toPersistedQueryManifest().writeTo(operationManifestFile)
      }
    }

    @Suppress("NAME_SHADOWING")
    val packageNameGenerator = packageNameGenerator ?: packageNameGenerator(
        codegenOptions.common.packageName,
        codegenOptions.common.packageNamesFromFilePaths,
        packageNameRoots
    )

    if (codegenSchema.targetLanguage == TargetLanguage.JAVA) {
      if (compilerKotlinHooks != null) {
        logger.warning("Apollo: compilerKotlinHooks is not used in Java")
      }
      if (codegenOptions.kotlin.generateAsInternal != null) {
        logger.warning("Apollo: generateAsInternal is not used in Java")
      }
      if (codegenOptions.kotlin.generateFilterNotNull != null) {
        logger.warning("Apollo: generateFilterNotNull is not used in Java")
      }
      if (codegenOptions.kotlin.sealedClassesForEnumsMatching != null) {
        logger.warning("Apollo: sealedClassesForEnumsMatching is not used in Java")
      }
      if (codegenOptions.kotlin.addJvmOverloads != null) {
        logger.warning("Apollo: addJvmOverloads is not used in Java")
      }
      if (codegenOptions.kotlin.requiresOptInAnnotation != null) {
        logger.warning("Apollo: requiresOptInAnnotation is not used in Java")
      }
      if (codegenOptions.kotlin.jsExport != null) {
        logger.warning("Apollo: jsExport is not used in Java")
      }
      if (codegenOptions.kotlin.generateInputBuilders != null) {
        logger.warning("Apollo: generateInputBuilders is not used in Java")
      }
      JavaCodeGen.writeOperations(
          codegenSchema = codegenSchema,
          irOperations = irOperations,
          irSchema = irSchema,
          operationOutput = operationOutput,
          upstreamCodegenMetadata = upstreamCodegenMetadata,
          commonCodegenOptions = codegenOptions.common,
          javaCodegenOptions = codegenOptions.java,
          packageNameGenerator = packageNameGenerator,
          compilerJavaHooks = compilerJavaHooks ?: defaultCompilerJavaHooks,
          outputDir = outputDir,
          codegenMetadataFile = codegenMetadataFile
      )
    } else {
      if (compilerJavaHooks != null) {
        logger.warning("Apollo: compilerJavaHooks is not used in Kotlin")
      }
      if (codegenOptions.java.nullableFieldStyle != null) {
        logger.warning("Apollo: nullableFieldStyle is not used in Kotlin")
      }
      if (codegenOptions.java.generateModelBuilders != null) {
        logger.warning("Apollo: generateModelBuilders is not used in Kotlin")
      }
      if (codegenOptions.java.classesForEnumsMatching != null) {
        logger.warning("Apollo: classesForEnumsMatching is not used in Kotlin")
      }
      if (codegenOptions.java.generatePrimitiveTypes != null) {
        logger.warning("Apollo: generatePrimitiveTypes is not used in Kotlin")
      }

      KotlinCodegen.writeSchemaAndOperations(
          codegenSchema = codegenSchema,
          irOperations = irOperations,
          irSchema = irSchema,
          operationOutput = operationOutput,
          upstreamCodegenMetadata = upstreamCodegenMetadata,
          commonCodegenOptions = codegenOptions.common,
          kotlinCodegenOptions = codegenOptions.kotlin,
          packageNameGenerator = packageNameGenerator,
          compilerKotlinHooks = compilerKotlinHooks ?: defaultCompilerKotlinHooks,
          outputDir = outputDir,
          codegenMetadataFile = codegenMetadataFile
      )
    }
  }

  /**
   * Compiles a set of files without serializing the intermediate results
   */
  fun build(
      schemaFiles: Set<File>,
      executableFiles: Set<File>,
      codegenSchemaOptionsFile: File,
      irOptionsFile: File,
      codegenOptionsFile: File,
      packageNameGenerator: PackageNameGenerator? = null,
      packageNameRoots: Set<String>? = null,
      operationOutputGenerator: OperationOutputGenerator? = null,
      compilerJavaHooks: List<ApolloCompilerJavaHooks>? = null,
      compilerKotlinHooks: List<ApolloCompilerKotlinHooks>? = null,
      logger: Logger = defaultLogger,
      outputDir: File,
      operationManifestFile: File? = null,
      codegenMetadataFile: File? = null,
  ) {
    return build(
        schemaFiles = schemaFiles,
        executableFiles = executableFiles,
        codegenSchemaOptions = codegenSchemaOptionsFile.toCodegenSchemaOptions(),
        irOptions = irOptionsFile.toIrOptions(),
        codegenOptions = codegenOptionsFile.toCodegenOptions(),
        packageNameGenerator = packageNameGenerator,
        packageNameRoots = packageNameRoots,
        operationOutputGenerator = operationOutputGenerator,
        compilerJavaHooks = compilerJavaHooks,
        compilerKotlinHooks = compilerKotlinHooks,
        logger = logger,
        outputDir = outputDir,
        operationManifestFile = operationManifestFile,
        codegenMetadataFile = codegenMetadataFile
    )
  }

  /**
   * Compiles a set of files without serializing the intermediate results
   */
  fun build(
      schemaFiles: Set<File>,
      executableFiles: Set<File>,
      codegenSchemaOptions: CodegenSchemaOptions,
      irOptions: IrOptions = IrOptions(),
      codegenOptions: CodegenOptions = CodegenOptions(),
      packageNameGenerator: PackageNameGenerator? = null,
      packageNameRoots: Set<String>? = null,
      operationOutputGenerator: OperationOutputGenerator? = null,
      compilerJavaHooks: List<ApolloCompilerJavaHooks>? = null,
      compilerKotlinHooks: List<ApolloCompilerKotlinHooks>? = null,
      logger: Logger = defaultLogger,
      outputDir: File,
      operationManifestFile: File? = null,
      codegenMetadataFile: File? = null,
  ) {
    val codegenSchema = buildCodegenSchema(
        schemaFiles = schemaFiles,
        logger = logger,
        codegenSchemaOptions = codegenSchemaOptions
    )

    val irOperations = buildIrOperations(
        codegenSchema = codegenSchema,
        executableFiles = executableFiles,
        upstreamFragmentDefinitions = emptyList(),
        options = irOptions,
        logger = logger
    )

    val irSchema = buildIrSchema(
        codegenSchema = codegenSchema,
        irOperations = listOf(irOperations),
    )

    buildSchemaAndOperationSources(
        codegenSchema = codegenSchema,
        irOperations = irOperations,
        irSchema = irSchema,
        upstreamCodegenMetadata = emptyList(),
        codegenOptions = codegenOptions,
        packageNameGenerator = packageNameGenerator,
        packageNameRoots = packageNameRoots,
        operationOutputGenerator = operationOutputGenerator,
        compilerKotlinHooks = compilerKotlinHooks,
        compilerJavaHooks = compilerJavaHooks,
        outputDir = outputDir,
        operationManifestFile = operationManifestFile,
        codegenMetadataFile = codegenMetadataFile
    )
  }

  fun schemaFileSpecs(
      codegenSchema: CodegenSchema,
      packageName: String,
  ): Pair<CodegenMetadata, List<FileSpec>> {
    return KotlinCodegen.schemaFileSpecs(codegenSchema, packageName)
  }

  fun resolverFileSpecs(
      codegenSchema: CodegenSchema,
      codegenMetadata: CodegenMetadata,
      irTargetObjects: List<IrTargetObject>,
      packageName: String,
      serviceName: String,
  ): List<FileSpec> {
    return KotlinCodegen.resolverFileSpecs(codegenSchema, codegenMetadata, irTargetObjects, packageName = packageName, serviceName = serviceName)
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
    val errors: List<Issue>,
)

internal fun List<Issue>.group(
    warnOnDeprecatedUsages: Boolean,
    fieldsOnDisjointTypesMustMerge: Boolean,
): IssueGroup {
  val ignored = mutableListOf<Issue>()
  val warnings = mutableListOf<Issue>()
  val errors = mutableListOf<Issue>()
  val apolloDirectives = kotlinLabsDefinitions(KOTLIN_LABS_VERSION).mapNotNull { (it as? GQLDirectiveDefinition)?.name }.toSet()

  forEach {
    val severity = when (it) {
      is DeprecatedUsage -> if (warnOnDeprecatedUsages) Severity.Warning else Severity.None
      is DifferentShape -> if (fieldsOnDisjointTypesMustMerge) Severity.Error else Severity.Warning
      is UnusedVariable -> Severity.Warning
      is UnusedFragment -> Severity.None
      is UnknownDirective -> if (it.requireDefinition) Severity.Error else Severity.Warning
      /**
       * Because some users might have added the apollo directive to their schema, we just let that through for now
       */
      is DirectiveRedefinition -> if (it.name in apolloDirectives) Severity.None else Severity.Warning
      is IncompatibleDefinition -> Severity.Warning
      else -> Severity.Error
    }

    when (severity) {
      Severity.None -> ignored.add(it)
      Severity.Warning -> warnings.add(it)
      Severity.Error -> errors.add(it)
    }
  }

  return IssueGroup(ignored, warnings, errors)
}
