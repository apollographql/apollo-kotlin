package com.apollographql.apollo.compiler

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.ast.DeprecatedUsage
import com.apollographql.apollo.ast.DifferentShape
import com.apollographql.apollo.ast.DirectiveRedefinition
import com.apollographql.apollo.ast.ForeignSchema
import com.apollographql.apollo.ast.GQLDefinition
import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.GQLSchemaDefinition
import com.apollographql.apollo.ast.GQLTypeDefinition
import com.apollographql.apollo.ast.IncompatibleDefinition
import com.apollographql.apollo.ast.Issue
import com.apollographql.apollo.ast.ParserOptions
import com.apollographql.apollo.ast.QueryDocumentMinifier
import com.apollographql.apollo.ast.UnusedFragment
import com.apollographql.apollo.ast.UnusedVariable
import com.apollographql.apollo.ast.builtinForeignSchemas
import com.apollographql.apollo.ast.checkEmpty
import com.apollographql.apollo.ast.internal.SchemaValidationOptions
import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.ast.pretty
import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.ast.validateAsExecutable
import com.apollographql.apollo.ast.validateAsSchema
import com.apollographql.apollo.compiler.codegen.SchemaAndOperationsLayout
import com.apollographql.apollo.compiler.codegen.SchemaLayout
import com.apollographql.apollo.compiler.codegen.SourceOutput
import com.apollographql.apollo.compiler.codegen.java.JavaCodegen
import com.apollographql.apollo.compiler.codegen.java.JavaOutput
import com.apollographql.apollo.compiler.codegen.java.toSourceOutput
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodegen
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOutput
import com.apollographql.apollo.compiler.codegen.kotlin.toSourceOutput
import com.apollographql.apollo.compiler.codegen.plus
import com.apollographql.apollo.compiler.internal.ApolloExecutableDocumentTransform
import com.apollographql.apollo.compiler.internal.checkApolloInlineFragmentsHaveTypeCondition
import com.apollographql.apollo.compiler.internal.checkApolloReservedEnumValueNames
import com.apollographql.apollo.compiler.internal.checkApolloTargetNameClashes
import com.apollographql.apollo.compiler.internal.checkCapitalizedFields
import com.apollographql.apollo.compiler.internal.checkConditionalFragments
import com.apollographql.apollo.compiler.ir.IrOperations
import com.apollographql.apollo.compiler.ir.IrOperationsBuilder
import com.apollographql.apollo.compiler.ir.IrSchemaBuilder
import com.apollographql.apollo.compiler.ir.buildIrDataBuilders
import com.apollographql.apollo.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo.compiler.operationoutput.toOperationOutput
import com.apollographql.apollo.compiler.pqm.toPersistedQueryManifest
import java.io.File

object ApolloCompiler {
  interface Logger {
    fun debug(message: String)
    fun info(message: String)
    fun warning(message: String)
    @Deprecated("use warning instead", replaceWith = ReplaceWith("warning(message)"))
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
    fun warn(message: String) {
      warning(message)
    }
    fun error(message: String)
  }

  fun buildCodegenSchema(
      schemaFiles: List<InputFile>,
      logger: Logger?,
      codegenSchemaOptions: CodegenSchemaOptions,
      foreignSchemas: List<ForeignSchema>,
      schemaTransform: SchemaDocumentTransform?,
  ): CodegenSchema {
    val schemaDocuments = schemaFiles.map {
      it.normalizedPath to it.file.toGQLDocument(allowJson = true)
    }

    if (schemaDocuments.isEmpty()) {
      error("No schema found. Apollo needs a `.graphqls` or a `.json` schema.")
    }

    // Locate the mainSchemaDocument. It's the one that contains the operation roots
    val mainSchemaDocuments = mutableListOf<GQLDocument>()
    val otherSchemaDocuments = mutableListOf<GQLDocument>()
    var mainSchemaNormalizedPath: String? = null
    schemaDocuments.forEach {
      val document = it.second
      if (
          document.definitions.filterIsInstance<GQLSchemaDefinition>().isNotEmpty()
          || document.definitions.filterIsInstance<GQLTypeDefinition>().any { it.name == "Query" }
      ) {
        mainSchemaDocuments.add(document)
        mainSchemaNormalizedPath = it.first
      } else {
        otherSchemaDocuments.add(document)
      }
    }

    if (mainSchemaDocuments.size > 1) {
      error("Multiple schemas found:\n${mainSchemaDocuments.map { it.sourceLocation?.filePath }.joinToString("\n")}\n" +
          "Use different services for different schemas"
      )
    } else if (mainSchemaDocuments.isEmpty()) {
      error("Schema(s) found:\n${schemaFiles.map { it.normalizedPath }.joinToString("\n")}\n" +
          "But none of them contain type definitions."
      )
    }
    val mainSchemaDocument = mainSchemaDocuments.single()

    // Sort the other schema document as type extensions are order sensitive, and we want this to be under the user control
    val otherSchemaDocumentSorted = otherSchemaDocuments.sortedBy { it.sourceLocation?.filePath?.substringAfterLast(File.pathSeparator) }

    val schemaDefinitions = (listOf(mainSchemaDocument) + otherSchemaDocumentSorted).flatMap { it.definitions }

    val sdl = buildString {
      var hasLink = false
      if (codegenSchemaOptions.scalarTypeMapping.isNotEmpty()) {
        appendLine("extend schema @link(url: \"https://specs.apollo.dev/kotlin_compiler_options/v0.1/\")")
        hasLink = true

        codegenSchemaOptions.scalarTypeMapping.forEach {
          append("extend scalar ${it.key} @kotlin_compiler_options__map(to: \"${it.value}\"")
          val adapterInitializer = codegenSchemaOptions.scalarAdapterMapping.get(it.key)
          if (adapterInitializer != null) {
            append(", with: \"$adapterInitializer\"")
          }
          append(")\n")
        }
      }
      if (codegenSchemaOptions.generateDataBuilders) {
        if (!hasLink) {
          appendLine("extend schema @link(url: \"https://specs.apollo.dev/kotlin_compiler_options/v0.1/\")")
        }
        append("extend schema @kotlin_compiler_options__generateDataBuilders")
      }
    }
    val scalarExtensions = sdl.toGQLDocument().definitions

    var schemaDocument = GQLDocument(
        definitions = schemaDefinitions + scalarExtensions,
        sourceLocation = null
    )

    if (schemaTransform != null) {
      schemaDocument = schemaTransform.transform(schemaDocument)
    }

    val result = schemaDocument.validateAsSchema(
        validationOptions = SchemaValidationOptions(
            /**
             * TODO: switch to false
             */
            addKotlinLabsDefinitions = true,
            builtinForeignSchemas() + foreignSchemas
        )
    )

    val issueGroup = result.issues.group(warnOnDeprecatedUsages = true, fieldsOnDisjointTypesMustMerge = true)

    issueGroup.errors.checkEmpty()
    issueGroup.warnings.forEach {
      // Using this format, IntelliJ will parse the warning and display it in the 'run' panel
      (logger ?: defaultLogger).warning("w: ${it.sourceLocation.pretty()}: Apollo: ${it.message}")
    }

    val schema = result.value!!

    return CodegenSchema(
        schema = schema,
        normalizedPath = mainSchemaNormalizedPath ?: "",
    )
  }

  /**
   * Parses the given files. Throws if there are parsing errors
   */
  private fun File.definitions(): List<GQLDefinition> {
    val definitions = mutableListOf<GQLDefinition>()
    val parseIssues = mutableListOf<Issue>()

    val parseResult = parseAsGQLDocument(options = ParserOptions.Builder().build())
    if (parseResult.issues.isNotEmpty()) {
      parseIssues.addAll(parseResult.issues)
    } else {
      // We can force cast here because we're guaranteed the parsing step will produce either issues
      // or a value
      definitions.addAll(parseResult.value!!.definitions)
    }

    // Parsing issues are fatal
    parseIssues.checkEmpty()

    return definitions
  }

  fun buildIrOperations(
      codegenSchema: CodegenSchema,
      executableFiles: List<InputFile>,
      upstreamCodegenModels: List<String>,
      upstreamFragmentDefinitions: List<GQLFragmentDefinition>,
      options: IrOptions,
      documentTransform: ExecutableDocumentTransform?,
      logger: Logger?,
  ): IrOperations {
    val schema = codegenSchema.schema

    /**
     * Remember the normalized path of each operation for the package name
     */
    val operationNameToNormalizedPath = mutableMapOf<String, String>()
    val fragmentNameToNormalizedPath = mutableMapOf<String, String>()

    /**
     * Step 1: parse the documents
     */
    val userDefinitions = mutableListOf<GQLDefinition>()
    /**
     * Sort the input files.
     * The generated Kotlin code does not depend on the order of the inputs, but in case we're serializing the
     * intermediate usedCoordinates, their order depends on the order of the input files.
     *
     * See https://github.com/gradle/gradle/issues/29321
     * See https://github.com/apollographql/apollo-kotlin/pull/5916
     */
    executableFiles.sortedBy { it.normalizedPath }.forEach { normalizedFile ->
      val fileDefinitions = normalizedFile.file.definitions()

      userDefinitions.addAll(fileDefinitions)
      fileDefinitions.forEach {
        when (it) {
          is GQLOperationDefinition -> {
            // Anonymous operations trigger an error during validation, so it's fine to fall back to the empty string here.
            val name = it.name ?: ""
            operationNameToNormalizedPath[name] = normalizedFile.normalizedPath
          }
          is GQLFragmentDefinition -> fragmentNameToNormalizedPath[it.name] = normalizedFile.normalizedPath
          else -> Unit
        }
      }
    }

    /**
     * Step 2, Modify the AST to add typename, key fields and call any user-provided transform.
     * If we detect that the cache compiler plugin is present, we skip adding the keyfields because it will do it.
     * TODO: deprecate `addTypename`
     */
    val hasCacheCompilerPlugin = try {
      Class.forName("com.apollographql.cache.apollocompilerplugin.internal.ApolloCacheCompilerPlugin")
      true
    } catch (_: ClassNotFoundException) {
      false
    }

    var document = ApolloExecutableDocumentTransform(options.addTypename ?: defaultAddTypename, !hasCacheCompilerPlugin).transform(
      schema = schema,
      document = GQLDocument(userDefinitions, sourceLocation = null),
      upstreamFragmentDefinitions
    )

    if (documentTransform != null) {
      document = documentTransform.transform(schema, document, upstreamFragmentDefinitions)
    }

    /**
     * Step 3, GraphQL validation
     */
    val validationResult = GQLDocument(
        definitions = document.definitions + upstreamFragmentDefinitions,
        sourceLocation = null
    ).validateAsExecutable(schema)

    val allIssues = mutableListOf<Issue>()
    allIssues.addAll(validationResult.issues)

    val codegenModels = defaultCodegenModels(options.codegenModels, upstreamCodegenModels)
    if (codegenModels == MODELS_RESPONSE_BASED || codegenModels == MODELS_OPERATION_BASED_WITH_INTERFACES) {
      allIssues.addAll(checkConditionalFragments(userDefinitions))
    }

    allIssues.addAll(checkApolloReservedEnumValueNames(schema))
    allIssues.addAll(checkApolloTargetNameClashes(schema))
    allIssues.addAll(checkApolloInlineFragmentsHaveTypeCondition(userDefinitions))

    val flattenModels = options.flattenModels ?: flattenModels(codegenModels)
    val decapitalizeFields = options.decapitalizeFields ?: defaultDecapitalizeFields
    val warnOnDeprecatedUsages = options.warnOnDeprecatedUsages ?: defaultWarnOnDeprecatedUsages
    val failOnWarnings = options.failOnWarnings ?: defaultFailOnWarnings
    val fieldsOnDisjointTypesMustMerge = options.fieldsOnDisjointTypesMustMerge ?: defaultFieldsOnDisjointTypesMustMerge
    val generateOptionalOperationVariables = options.generateOptionalOperationVariables ?: defaultGenerateOptionalOperationVariables
    val alwaysGenerateTypesMatching = options.alwaysGenerateTypesMatching ?: defaultAlwaysGenerateTypesMatching

    if (!decapitalizeFields) {
      // When flattenModels is true, we still must check capitalized fields inside fragment spreads
      allIssues.addAll(checkCapitalizedFields(userDefinitions, checkFragmentsOnly = flattenModels))
    }

    val issueGroup = allIssues.group(
        warnOnDeprecatedUsages,
        fieldsOnDisjointTypesMustMerge,
    )

    issueGroup.errors.checkEmpty()

    issueGroup.warnings.forEach {
      // Using this format, IntelliJ will parse the warning and display it in the 'run' panel
      (logger ?: defaultLogger).warning("w: ${it.sourceLocation.pretty()}: Apollo: ${it.message}")
    }

    if (failOnWarnings && issueGroup.warnings.isNotEmpty()) {
      throw IllegalStateException("Apollo: Warnings found and 'failOnWarnings' is true, aborting.")
    }

    /**
     * Step 4 Build the IR
     */
    val operations = mutableListOf<GQLOperationDefinition>()
    val fragments = mutableListOf<GQLFragmentDefinition>()
    document.definitions.forEach {
      when(it) {
        is GQLOperationDefinition -> operations.add(it)
        is GQLFragmentDefinition -> fragments.add(it)
        else -> Unit
      }
    }

    // Remember the fragments with the possibly updated fragments
    val allFragmentDefinitions = (fragments + upstreamFragmentDefinitions).associateBy { it.name }

    return IrOperationsBuilder(
        schema = schema,
        operationDefinitions = operations,
        operationNameToNormalizedPath = operationNameToNormalizedPath,
        fragmentDefinitions = fragments,
        fragmentNameToNormalizedPath = fragmentNameToNormalizedPath,
        allFragmentDefinitions = allFragmentDefinitions,
        codegenModels = codegenModels,
        generateOptionalOperationVariables = generateOptionalOperationVariables,
        flattenModels = flattenModels,
        decapitalizeFields = decapitalizeFields,
        alwaysGenerateTypesMatching = alwaysGenerateTypesMatching,
        fragmentVariableUsages = validationResult.fragmentVariableUsages
    ).build()
  }

  fun buildSchemaSources(
      codegenSchema: CodegenSchema,
      usedCoordinates: UsedCoordinates,
      codegenOptions: CodegenOptions,
      schemaLayout: SchemaLayout?,
      javaOutputTransform: Transform<JavaOutput>?,
      kotlinOutputTransform: Transform<KotlinOutput>?,
  ): SourceOutput {
    val irSchema = IrSchemaBuilder.build(
        schema = codegenSchema.schema,
        usedCoordinates = usedCoordinates,
    )

    val targetLanguage = defaultTargetLanguage(codegenOptions.targetLanguage, emptyList())
    codegenOptions.validate()

    val layout = schemaLayout ?: SchemaAndOperationsLayout(
        codegenSchema = codegenSchema,
        packageName = codegenOptions.packageName,
        rootPackageName = codegenOptions.rootPackageName,
        useSemanticNaming = codegenOptions.useSemanticNaming,
        decapitalizeFields = codegenOptions.decapitalizeFields,
        generatedSchemaName = codegenOptions.generatedSchemaName,
    )

    return if (targetLanguage == TargetLanguage.JAVA) {
      JavaCodegen.buildSchemaSources(
          irSchema = irSchema,
          codegenOptions = codegenOptions,
          layout = layout,
          javaOutputTransform = javaOutputTransform
      ).toSourceOutput()
    } else {
      KotlinCodegen.buildSchemaSources(
          targetLanguage = targetLanguage,
          irSchema = irSchema,
          codegenOptions = codegenOptions,
          layout = layout,
          kotlinOutputTransform = kotlinOutputTransform
      ).toSourceOutput()
    }
  }

  fun buildSchemaAndOperationsSourcesFromIr(
      codegenSchema: CodegenSchema,
      irOperations: IrOperations,
      downstreamUsedCoordinates: UsedCoordinates,
      upstreamCodegenMetadata: List<CodegenMetadata>,
      codegenOptions: CodegenOptions,
      layout: SchemaAndOperationsLayout?,
      operationIdsGenerator: OperationIdsGenerator?,
      irOperationsTransform: Transform<IrOperations>?,
      javaOutputTransform: Transform<JavaOutput>?,
      kotlinOutputTransform: Transform<KotlinOutput>?,
      operationManifestFile: File?,
  ): SourceOutput {
    @Suppress("NAME_SHADOWING")
    val irOperations = irOperations.maybeTransform(irOperationsTransform)

    val targetLanguage = defaultTargetLanguage(codegenOptions.targetLanguage, upstreamCodegenMetadata)
    codegenOptions.validate()

    val descriptors = irOperations.operations.map {
      OperationDescriptor(
          name = it.name,
          source = QueryDocumentMinifier.minify(it.sourceWithFragments),
          type = it.operationType.name.lowercase()
      )
    }

    val operationOutput = descriptors.toOperationOutput(
        (operationIdsGenerator ?: defaultOperationIdsGenerator).generate(descriptors)
    )

    check(operationOutput.size == irOperations.operations.size) {
      """The number of operation IDs (${operationOutput.size}) should match the number of operations (${irOperations.operations.size}).
        |Check that all your IDs are unique.
      """.trimMargin()
    }

    if (operationManifestFile != null) {
      val operationManifestFormat = codegenOptions.operationManifestFormat
      @Suppress("DEPRECATION_ERROR")
      when (operationManifestFormat) {
        MANIFEST_NONE -> operationManifestFile.writeText("Use operationManifestFormat to generate the operation manifest.")
        MANIFEST_OPERATION_OUTPUT -> operationOutput.writeTo(operationManifestFile)
        MANIFEST_PERSISTED_QUERY -> operationOutput.toPersistedQueryManifest().writeTo(operationManifestFile)
      }
    }

    @Suppress("NAME_SHADOWING")
    val layout = layout ?: SchemaAndOperationsLayout(
        codegenSchema = codegenSchema,
        packageName = codegenOptions.packageName,
        rootPackageName = codegenOptions.rootPackageName,
        useSemanticNaming = codegenOptions.useSemanticNaming,
        decapitalizeFields = codegenOptions.decapitalizeFields,
        generatedSchemaName = codegenOptions.generatedSchemaName,
    )

    var sourceOutput: SourceOutput? = null
    if (upstreamCodegenMetadata.isEmpty()) {
      sourceOutput = buildSchemaSources(
          codegenSchema = codegenSchema,
          usedCoordinates = downstreamUsedCoordinates.mergeWith(irOperations.usedCoordinates),
          codegenOptions = codegenOptions,
          schemaLayout = layout,
          javaOutputTransform = javaOutputTransform,
          kotlinOutputTransform = kotlinOutputTransform,
      )
    }
    if (targetLanguage == TargetLanguage.JAVA) {
      sourceOutput = sourceOutput plus JavaCodegen.buildOperationsSources(
          irOperations = irOperations,
          operationOutput = operationOutput,
          upstreamCodegenMetadatas = upstreamCodegenMetadata + listOfNotNull(sourceOutput?.codegenMetadata),
          codegenOptions = codegenOptions,
          layout = layout,
          javaOutputTransform = javaOutputTransform,
      ).toSourceOutput()
    } else {
      sourceOutput = sourceOutput plus KotlinCodegen.buildOperationSources(
          targetLanguage = targetLanguage,
          irOperations = irOperations,
          operationOutput = operationOutput,
          upstreamCodegenMetadata = upstreamCodegenMetadata + listOfNotNull(sourceOutput?.codegenMetadata),
          codegenOptions = codegenOptions,
          layout = layout,
          kotlinOutputTransform = kotlinOutputTransform
      ).toSourceOutput()
    }

    return sourceOutput
  }

  /**
   * Compiles a set of files without serializing the intermediate results
   */
  fun buildSchemaAndOperationsSources(
      schemaFiles: List<InputFile>,
      executableFiles: List<InputFile>,
      codegenSchemaOptions: CodegenSchemaOptions,
      irOptions: IrOptions,
      codegenOptions: CodegenOptions,
      layoutFactory: LayoutFactory?,
      operationIdsGenerator: OperationIdsGenerator?,
      irOperationsTransform: Transform<IrOperations>?,
      javaOutputTransform: Transform<JavaOutput>?,
      kotlinOutputTransform: Transform<KotlinOutput>?,
      documentTransform: ExecutableDocumentTransform?,
      schemaDocumentTransform: SchemaDocumentTransform?,
      logger: Logger?,
      operationManifestFile: File?,
  ): SourceOutput {
    val codegenSchema = buildCodegenSchema(
        schemaFiles = schemaFiles,
        logger = logger,
        codegenSchemaOptions = codegenSchemaOptions,
        foreignSchemas = emptyList(),
        schemaTransform = schemaDocumentTransform
    )

    return buildSchemaAndOperationsSources(
        codegenSchema,
        executableFiles,
        irOptions,
        codegenOptions,
        layoutFactory,
        operationIdsGenerator,
        irOperationsTransform,
        javaOutputTransform,
        kotlinOutputTransform,
        documentTransform,
        logger,
        operationManifestFile
    )
  }

  /**
   * Compiles a set of files without serializing the intermediate results
   */
  fun buildSchemaAndOperationsSources(
      codegenSchema: CodegenSchema,
      executableFiles: List<InputFile>,
      irOptions: IrOptions,
      codegenOptions: CodegenOptions,
      layoutFactory: LayoutFactory?,
      operationIdsGenerator: OperationIdsGenerator?,
      irOperationsTransform: Transform<IrOperations>?,
      javaOutputTransform: Transform<JavaOutput>?,
      kotlinOutputTransform: Transform<KotlinOutput>?,
      documentTransform: ExecutableDocumentTransform?,
      logger: Logger?,
      operationManifestFile: File?,
  ): SourceOutput {
    val irOperations = buildIrOperations(
        codegenSchema = codegenSchema,
        executableFiles = executableFiles,
        upstreamCodegenModels = emptyList(),
        upstreamFragmentDefinitions = emptyList(),
        documentTransform = documentTransform,
        options = irOptions,
        logger = logger
    )

    val sourceOutput = buildSchemaAndOperationsSourcesFromIr(
        codegenSchema = codegenSchema,
        irOperations = irOperations,
        downstreamUsedCoordinates = UsedCoordinates(),
        upstreamCodegenMetadata = emptyList(),
        codegenOptions = codegenOptions,
        layout = layoutFactory?.create(codegenSchema),
        irOperationsTransform = irOperationsTransform,
        javaOutputTransform = javaOutputTransform,
        kotlinOutputTransform = kotlinOutputTransform,
        operationManifestFile = operationManifestFile,
        operationIdsGenerator = operationIdsGenerator,
    )

    return sourceOutput
  }

  fun buildDataBuilders(
      codegenSchema: CodegenSchema,
      usedCoordinates: UsedCoordinates,
      codegenOptions: CodegenOptions,
      layout: SchemaLayout?,
      upstreamCodegenMetadata: List<CodegenMetadata>,
  ): SourceOutput {
    val irDataBuilders = buildIrDataBuilders(codegenSchema, usedCoordinates)

    @Suppress("NAME_SHADOWING")
    val layout = layout ?: SchemaAndOperationsLayout(
        codegenSchema = codegenSchema,
        packageName = codegenOptions.packageName,
        rootPackageName = codegenOptions.rootPackageName,
        useSemanticNaming = codegenOptions.useSemanticNaming,
        decapitalizeFields = codegenOptions.decapitalizeFields,
        generatedSchemaName = codegenOptions.generatedSchemaName,
    )

    val targetLanguage = codegenOptions.targetLanguage ?: defaultTargetLanguage(codegenOptions.targetLanguage, upstreamCodegenMetadata)
    return if (targetLanguage == TargetLanguage.JAVA) {
      JavaCodegen.buildDataBuilders(
          dataBuilders = irDataBuilders,
          layout = layout,
          codegenOptions = codegenOptions,
          upstreamCodegenMetadata = upstreamCodegenMetadata,
      ).toSourceOutput()
    } else {
      KotlinCodegen.buildDataBuilders(
          dataBuilders = irDataBuilders,
          layout = layout,
          codegenOptions = codegenOptions,
          upstreamCodegenMetadata = upstreamCodegenMetadata,
          targetLanguage = targetLanguage,
      ).toSourceOutput()
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
    val errors: List<Issue>,
)

internal fun List<Issue>.group(
    warnOnDeprecatedUsages: Boolean,
    fieldsOnDisjointTypesMustMerge: Boolean,
): IssueGroup {
  val ignored = mutableListOf<Issue>()
  val warnings = mutableListOf<Issue>()
  val errors = mutableListOf<Issue>()

  forEach {
    val severity = when (it) {
      is DeprecatedUsage -> if (warnOnDeprecatedUsages) Severity.Warning else Severity.None
      is DifferentShape -> if (fieldsOnDisjointTypesMustMerge) Severity.Error else Severity.Warning
      is UnusedVariable -> Severity.Warning
      is UnusedFragment -> Severity.Warning
      is IncompatibleDefinition -> Severity.Warning // This should probably be an error
      is DirectiveRedefinition -> Severity.Warning
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

/**
 * An input file together with its normalizedPath.
 * normalizedPath is used to compute the package name in some cases
 */
class InputFile(val file: File, val normalizedPath: String)

fun Collection<File>.toInputFiles(): List<InputFile> = map { InputFile(it, "") }

internal fun <T> T.maybeTransform(transform: Transform<T>?) = transform?.transform(this) ?: this

fun interface LayoutFactory {
  fun create(codegenSchema: CodegenSchema): SchemaAndOperationsLayout?
}

