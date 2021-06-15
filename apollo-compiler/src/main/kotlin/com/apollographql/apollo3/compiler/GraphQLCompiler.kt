package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.api.QueryDocumentMinifier
import com.apollographql.apollo3.compiler.codegen.KotlinCodeGen
import com.apollographql.apollo3.compiler.introspection.toGQLDocument
import com.apollographql.apollo3.compiler.introspection.toIntrospectionSchema
import com.apollographql.apollo3.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo3.ast.*
import com.apollographql.apollo3.compiler.ir.IrBuilder
import com.apollographql.apollo3.compiler.ir.dumpTo
import java.io.File

class GraphQLCompiler {

  interface Logger {
    fun warning(message: String)
  }

  fun write(
      executableFiles: Set<File>,
      outputDir: File,
      debugDir: File? = null,
      incomingOptions: IncomingOptions,
      moduleOptions: ModuleOptions,
  ) {
    checkCustomScalars(incomingOptions)

    outputDir.deleteRecursively()
    outputDir.mkdirs()
    debugDir?.deleteRecursively()
    debugDir?.mkdirs()

    /**
     * Step 1: parse the documents
     */
    val definitions = mutableListOf<GQLDefinition>()
    val parseIssues = mutableListOf<Issue>()
    executableFiles.map { file ->
      when(val parseResult = file.parseAsGQLDocument()) {
        is ParseResult.Success -> definitions.addAll(parseResult.value.definitions)
        is ParseResult.Error -> parseIssues.addAll(parseResult.issues)
      }
    }

    // Parsing issues are fatal
    parseIssues.checkNoErrors()

    /**
     * Step 2, GraphQL validation
     */
    val validationIssues = GQLDocument(
        definitions = definitions + incomingOptions.metadataFragments.map { it.definition },
        filePath = null
    ).validateAsOperations(incomingOptions.schema)

    validationIssues.checkNoErrors()

    val warnings = validationIssues.filter {
      it.severity == Issue.Severity.WARNING && (it !is Issue.DeprecatedUsage || moduleOptions.warnOnDeprecatedUsages)
    }
    warnings.forEach {
      // Using this format, IntelliJ will parse the warning and display it in the 'run' panel
      moduleOptions.logger.warning("w: ${it.sourceLocation.pretty()}: ApolloGraphQL: ${it.message}")
    }
    if (moduleOptions.failOnWarnings && warnings.isNotEmpty()) {
      throw IllegalStateException("ApolloGraphQL: Warnings found and 'failOnWarnings' is true, aborting.")
    }

    /**
     * Step 3, Modify the AST to add typename and key fields
     */
    val incomingFragments = incomingOptions.metadataFragments.map { it.definition }
    var allFragmentDefinitions = (definitions.filterIsInstance<GQLFragmentDefinition>() + incomingFragments).associateBy { it.name }
    val fragments = definitions.filterIsInstance<GQLFragmentDefinition>().map {
      addRequiredFields(it, incomingOptions.schema)
    }

    val operations = definitions.filterIsInstance<GQLOperationDefinition>().map {
      addRequiredFields(it, incomingOptions.schema)
    }

    // Update the fragments with the possibly updated fragments
    allFragmentDefinitions = (fragments + incomingFragments).associateBy { it.name }

    operations.forEach {
      checkKeyFields(it, incomingOptions.schema, allFragmentDefinitions)
    }
    fragments.forEach {
      checkKeyFields(it, incomingOptions.schema, allFragmentDefinitions)
    }

    /**
     * Build the IR
     */
    val ir = IrBuilder(
        schema = incomingOptions.schema,
        operationDefinitions = operations,
        allFragmentDefinitions = allFragmentDefinitions,
        alwaysGenerateTypesMatching = moduleOptions.alwaysGenerateTypesMatching,
        customScalarToKotlinName = incomingOptions.customScalarsMapping,
        codegenModels = incomingOptions.codegenModels,
    ).build()

    if (debugDir != null) {
      ir.dumpTo(File(debugDir, "ir.json"))
    }

    val operationOutput = ir.operations.map {
      OperationDescriptor(
          name = it.name,
          source = QueryDocumentMinifier.minify(it.sourceWithFragments)
      )
    }.let {
      moduleOptions.operationOutputGenerator.generate(it)
    }

    check(operationOutput.size == operations.size) {
      """The number of operation IDs (${operationOutput.size}) should match the number of operations (${operations.size}).
        |Check that all your IDs are unique.
      """.trimMargin()
    }

    if (moduleOptions.operationOutputFile != null) {
      moduleOptions.operationOutputFile.writeText(operationOutput.toJson("  "))
    }

    /**
     * Write the generated models
     */
    KotlinCodeGen(
        ir = ir,
        generateAsInternal = moduleOptions.generateAsInternal,
        operationOutput = operationOutput,
        useSemanticNaming = moduleOptions.useSemanticNaming,
        packageNameProvider = moduleOptions.packageNameProvider,
        schemaPackageName = incomingOptions.schemaPackageName,
        generateFilterNotNull = moduleOptions.generateFilterNotNull,
        generateFragmentImplementations = moduleOptions.generateFragmentImplementations,
        generateQueryDocument = moduleOptions.generateQueryDocument,
        fragmentsToSkip = incomingOptions.metadataFragments.map { it.name }.toSet(),
        enumsToSkip = incomingOptions.metadataEnums,
        inputObjectsToSkip = incomingOptions.metadataInputObjects,
        generateSchema = !incomingOptions.isFromMetadata,
        flatten = incomingOptions.flattenModels,
        flattenNamesInOrder = incomingOptions.codegenModels != MODELS_COMPAT
    ).write(outputDir = outputDir)

    /**
     * Write the metadata
     */
    if (moduleOptions.metadataOutputFile != null) {
      moduleOptions.metadataOutputFile.parentFile.mkdirs()
      val schema = if (incomingOptions.isFromMetadata) {
        // There is already a schema defined in this tree
        null
      } else {
        incomingOptions.schema
      }

      val outgoingMetadataFragments = fragments.map {
        MetadataFragment(
            name = it.name,
            packageName = "${incomingOptions.schemaPackageName}.fragment",
            definition = it
        )
      }

      ApolloMetadata(
          schema = schema,
          customScalarsMapping = incomingOptions.customScalarsMapping,
          generatedFragments = outgoingMetadataFragments,
          generatedEnums = ir.enums.map { it.name }.toSet(),
          generatedInputObjects = ir.inputObjects.map { it.name }.toSet(),
          codegenModels = incomingOptions.codegenModels,
          flattenModels = incomingOptions.flattenModels,
          moduleName = moduleOptions.moduleName,
          pluginVersion = VERSION,
          schemaPackageName = incomingOptions.schemaPackageName
      ).writeTo(moduleOptions.metadataOutputFile)
    }
  }

  private fun checkCustomScalars(incomingOptions: IncomingOptions) {
    /**
     * Generate the mapping for all custom scalars
     *
     * If the user specified a mapping, use it, else fallback to [Any]
     */
    val schemaScalars = incomingOptions.schema
        .typeDefinitions
        .values
        .filterIsInstance<GQLScalarTypeDefinition>()
        .filter { !it.isBuiltIn() }
        .map { type -> type.name }
        .toSet()
    val unknownScalars = incomingOptions.customScalarsMapping.keys.subtract(schemaScalars)
    check(unknownScalars.isEmpty()) {
      "ApolloGraphQL: unknown custom scalar(s): ${unknownScalars.joinToString(",")}"
    }
  }

  /**
   * These are the options that should be the same in all modules.
   */
  class IncomingOptions(
      val schema: Schema,
      val schemaPackageName: String,
      val customScalarsMapping: Map<String, String>,
      val codegenModels: String,
      val flattenModels: Boolean,
      val metadataInputObjects: Set<String>,
      val metadataEnums: Set<String>,
      val isFromMetadata: Boolean,
      val metadataFragments: List<MetadataFragment>,
  ) {
    companion object {
      fun fromMetadata(metadata: ApolloMetadata): IncomingOptions {
        return IncomingOptions(
            schema = metadata.schema!!,
            schemaPackageName = metadata.schemaPackageName,
            customScalarsMapping = metadata.customScalarsMapping,
            codegenModels = metadata.codegenModels,
            flattenModels = metadata.flattenModels,
            metadataInputObjects = metadata.generatedInputObjects,
            metadataEnums = metadata.generatedEnums,
            isFromMetadata = true,
            metadataFragments = metadata.generatedFragments,
        )
      }

      fun fromOptions(
          schemaFiles: Set<File>,
          customScalarsMapping: Map<String, String>,
          codegenModels: String,
          schemaPackageName: String,
          flattenModels: Boolean,
      ): IncomingOptions {
        val schemaDefinitions = schemaFiles.flatMap {
          it.toGQLDocument().definitions
        }

        val schemaDocument = GQLDocument(
            definitions = schemaDefinitions + apolloDefinitions(),
            filePath = null
        )

        val schema = schemaDocument.toSchema()

        return IncomingOptions(
            schema = schema,
            schemaPackageName = schemaPackageName,
            customScalarsMapping = customScalarsMapping,
            codegenModels = codegenModels,
            flattenModels = flattenModels,
            metadataInputObjects = emptySet(),
            metadataEnums = emptySet(),
            isFromMetadata = false,
            metadataFragments = emptyList(),
        )
      }

      private fun File.toGQLDocument(): GQLDocument {
        return if (extension == "json") {
           toIntrospectionSchema().toGQLDocument(filePath = path)
        } else {
           parseAsGQLDocument().getOrThrow()
        }
      }
    }
  }

  data class ModuleOptions(
      /**
       * Additional enum/input types to generate.
       * For input types, this will recursively add all input fields types/enums.
       */
      val alwaysGenerateTypesMatching: Set<String>,

      /**
       *
       */
      val metadataOutputFile: File?,

      val packageNameProvider: PackageNameProvider,

      //========== operation-output ============

      /**
       * the file where to write the operationOutput or null if no operationOutput is required
       */
      val operationOutputFile: File?,
      /**
       * the OperationOutputGenerator used to generate operation Ids
       */
      val operationOutputGenerator: OperationOutputGenerator,

      //========== codegen options ============

      val useSemanticNaming: Boolean,
      val warnOnDeprecatedUsages: Boolean,
      val failOnWarnings: Boolean,
      val logger: Logger,
      val generateAsInternal: Boolean,
      /**
       * Kotlin native will generate [Any?] for optional types
       * Setting generateFilterNotNull will generate extra `filterNotNull` functions that will help keep the type information
       */
      val generateFilterNotNull: Boolean,

      //========== on/off flags to switch some codegen off ============

      /**
       * Whether to generate the [com.apollographql.apollo3.api.Fragment] as well as response and variables adapters.
       * If generateFragmentsAsInterfaces is true, this will also generate data classes for the fragments.
       *
       * Set to true if you need to read/write fragments from the cache or if you need to instantiate fragments
       */
      val generateFragmentImplementations: Boolean,
      /**
       * Whether to generate the compiled selections used to read/write from the normalized cache.
       * Disable this option if you don't use the normalized cache to save some bytecode
       */
      val generateResponseFields: Boolean,
      /**
       * Whether to embed the query document in the [com.apollographql.apollo3.api.Operation]s. By default this is true as it is needed
       * to send the operations to the server.
       * If performance is critical and you have a way to whitelist/read the document from another place, disable this.
       */
      val generateQueryDocument: Boolean,
      val moduleName: String,
  )

  companion object {
    private val NoOpLogger = object : Logger {
      override fun warning(message: String) {
      }
    }

    val defaultMetadataFragments = emptyList<MetadataFragment>()
    private val defaultPackageNameProvider = PackageNameProvider.Flat("com.apollographql.generated")
    val defaultAlwaysGenerateTypesMatching = emptySet<String>()
    private val defaultOperationOutputFile = null
    private val defaultOperationOutputGenerator = OperationOutputGenerator.Default(OperationIdGenerator.Sha256)
    val defaultCustomScalarsMapping = emptyMap<String, String>()
    const val defaultUseSemanticNaming = true
    const val defaultWarnOnDeprecatedUsages = true
    const val defaultFailOnWarnings = false
    private val defaultLogger = NoOpLogger
    const val defaultGenerateAsInternal = false
    const val defaultGenerateFilterNotNull = false
    const val defaultGenerateFragmentsAsInterfaces = false
    const val defaultGenerateFragmentImplementations = false
    const val defaultGenerateResponseFields = true
    const val defaultGenerateQueryDocument = true
    private const val defaultModuleName = "apollographql"
    const val defaultCodegenModels = MODELS_COMPAT
    private val defaultMetadataOutputFile = null

    val DefaultModuleOptions = ModuleOptions(
        alwaysGenerateTypesMatching = defaultAlwaysGenerateTypesMatching,
        operationOutputFile = defaultOperationOutputFile,
        operationOutputGenerator = defaultOperationOutputGenerator,
        useSemanticNaming = defaultUseSemanticNaming,
        warnOnDeprecatedUsages = defaultWarnOnDeprecatedUsages,
        failOnWarnings = defaultFailOnWarnings,
        logger = defaultLogger,
        generateAsInternal = defaultGenerateAsInternal,
        generateFilterNotNull = defaultGenerateFilterNotNull,
        generateFragmentImplementations = defaultGenerateFragmentImplementations,
        generateResponseFields = defaultGenerateResponseFields,
        generateQueryDocument = defaultGenerateQueryDocument,
        packageNameProvider = defaultPackageNameProvider,
        metadataOutputFile = defaultMetadataOutputFile,
        moduleName = defaultModuleName

    )
  }
}

const val MODELS_RESPONSE_BASED = "responseBased"
const val MODELS_OPERATION_BASED = "operationBased"
const val MODELS_COMPAT = "compat"