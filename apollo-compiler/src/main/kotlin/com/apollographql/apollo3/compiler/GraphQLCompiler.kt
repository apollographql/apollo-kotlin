package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.api.QueryDocumentMinifier
import com.apollographql.apollo3.compiler.codegen.KotlinCodeGenerator
import com.apollographql.apollo3.compiler.introspection.IntrospectionSchema
import com.apollographql.apollo3.compiler.introspection.toGQLDocument
import com.apollographql.apollo3.compiler.introspection.toIntrospectionSchema
import com.apollographql.apollo3.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo3.compiler.unified.ir.IrBuilder
import com.apollographql.apollo3.compiler.unified.ir.dumpTo
import com.apollographql.apollo3.ast.*
import java.io.File

class GraphQLCompiler {

  interface Logger {
    fun warning(message: String)
  }

  fun write(
      operationFiles: Set<File>,
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

    val definitions = mutableListOf<GQLDefinition>()
    val parseIssues = mutableListOf<Issue>()
    operationFiles.map { file ->
      when(val parseResult = file.parseAsGQLDocument()) {
        is ParseResult.Success -> definitions.addAll(parseResult.value.definitions)
        is ParseResult.Error -> parseIssues.addAll(parseResult.issues)
      }
    }

    // Parsing issues are fatal
    parseIssues.checkNoErrors()

    val validationIssues = GQLDocument(
        definitions = definitions + incomingOptions.metadataFragments.map { it.definition },
        filePath = null
    ).validateAsOperations(incomingOptions.schema)

    validationIssues.checkNoErrors()

    if (moduleOptions.warnOnDeprecatedUsages) {
      val warnings = validationIssues.filter { it.severity == Issue.Severity.WARNING }
      warnings.forEach {
        // antlr is 0-indexed but IntelliJ is 1-indexed. Add 1 so that clicking the link will land on the correct location
        val column = it.sourceLocation.position + 1
        // Using this format, IntelliJ will parse the warning and display it in the 'run' panel
        // XXX: uniformize with error handling above
        moduleOptions.logger.warning("w: ${it.sourceLocation.filePath}:${it.sourceLocation.line}:${column}: ApolloGraphQL: ${it.message}")
      }
      if (moduleOptions.failOnWarnings && warnings.isNotEmpty()) {
        throw IllegalStateException("ApolloGraphQL: Warnings found and 'failOnWarnings' is true, aborting.")
      }
    }

    val fragments = definitions.filterIsInstance<GQLFragmentDefinition>().map {
      it.withTypenameWhenNeeded(incomingOptions.schema)
    }

    val operations = definitions.filterIsInstance<GQLOperationDefinition>().map {
      it.withTypenameWhenNeeded(incomingOptions.schema)
    }

    val ir = IrBuilder(
        schema = incomingOptions.schema,
        operationDefinitions = operations,
        fragmentDefinitions = fragments,
        alwaysGenerateTypesMatching = moduleOptions.alwaysGenerateTypesMatching,
        customScalarToKotlinName = incomingOptions.customScalarsMapping,
        generateFragmentAsInterfaces = incomingOptions.generateFragmentsAsInterfaces,
        metadataFragments = incomingOptions.metadataFragments,
        metadataEnums = incomingOptions.metadataEnums,
        metadataInputObjects = incomingOptions.metadataInputObjects,
        metadataSchema = incomingOptions.isFromMetadata
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

    KotlinCodeGenerator(
        ir = ir,
        generateAsInternal = moduleOptions.generateAsInternal,
        operationOutput = operationOutput,
        useSemanticNaming = moduleOptions.useSemanticNaming,
        packageNameProvider = moduleOptions.packageNameProvider,
        typePackageName = "${incomingOptions.schemaPackageName}.type",
        generateSchema = !incomingOptions.isFromMetadata,
        generateFilterNotNull = moduleOptions.generateFilterNotNull,
        generateFragmentsAsInterfaces = incomingOptions.generateFragmentsAsInterfaces,
        generateFragmentImplementations = moduleOptions.generateFragmentImplementations,
        generateResponseFields = moduleOptions.generateResponseFields,
        generateQueryDocument = moduleOptions.generateQueryDocument,
    ).write(outputDir = outputDir)

    if (moduleOptions.metadataOutputFile != null) {
      moduleOptions.metadataOutputFile.parentFile.mkdirs()
      // Disable this check for now as we generate the metadata always as it is part of the "assemble" target
//      check(!moduleOptions.generateAsInternal) {
//        "Specifying 'generateAsInternal=true' does not make sense in a multi-module setup"
//      }
      val schema = if (incomingOptions.isFromMetadata) {
        // There is already a schema defined in this tree
        null
      } else {
        incomingOptions.schema
      }

      val outgoingMetadataFragments = fragments.map {
        MetadataFragment(
            name = it.name,
            packageName = moduleOptions.packageNameProvider.fragmentPackageName(it.sourceLocation.filePath!!),
            definition = it
        )
      }

      ApolloMetadata(
          schema = schema,
          customScalarsMapping = incomingOptions.customScalarsMapping,
          generatedFragments = outgoingMetadataFragments,
          generatedEnums = ir.enums.map { it.name }.toSet(),
          generatedInputObjects = ir.inputObjects.map { it.name }.toSet(),
          generateFragmentsAsInterfaces = incomingOptions.generateFragmentsAsInterfaces,
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
      val generateFragmentsAsInterfaces: Boolean,
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
            generateFragmentsAsInterfaces = metadata.generateFragmentsAsInterfaces,
            metadataInputObjects = metadata.generatedInputObjects,
            metadataEnums = metadata.generatedEnums,
            isFromMetadata = true,
            metadataFragments = metadata.generatedFragments,
        )
      }

      fun from(
          roots: Roots,
          schemaFile: File,
          extraSchemaFiles: Set<File>,
          customScalarsMapping: Map<String, String>,
          generateFragmentsAsInterfaces: Boolean,
          rootPackageName: String,
      ): IncomingOptions {
        val relativeSchemaPackageName = try {
          roots.filePackageName(schemaFile.absolutePath)
        } catch (e: Exception) {
          ""
        }

        val document = schemaFile.toGQLDocument()
        val extraDefinitions = extraSchemaFiles.flatMap {
          it.parseAsGQLDocument().getOrThrow().definitions
        }

        val schema = GQLDocument(
            definitions = document.definitions + extraDefinitions +  apolloDefinitions(),
            filePath = null
        ).toSchema()

        return IncomingOptions(
            schema = schema,
            schemaPackageName = "$rootPackageName.$relativeSchemaPackageName".removePrefix(".").removeSuffix("."),
            customScalarsMapping = customScalarsMapping,
            generateFragmentsAsInterfaces = generateFragmentsAsInterfaces,
            metadataInputObjects = emptySet(),
            metadataEnums = emptySet(),
            isFromMetadata = false,
            metadataFragments = emptyList(),
        )
      }

      private fun File.toGQLDocument(): GQLDocument {
        return if (extension == "json") {
           toIntrospectionSchema().toGQLDocument()
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
       * Whether to generate the [com.apollographql.apollo3.api.MergedField]s. [com.apollographql.apollo3.api.MergedField]s are
       * used to read/write from the normalized cache. Disable this option if you don't use the normalized cache to save some bytecode
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
    val NoOpLogger = object : Logger {
      override fun warning(message: String) {
      }
    }

    val defaultMetadataFragments = emptyList<MetadataFragment>()
    val defaultMetadataInputObjects = emptySet<String>()
    val defaultMetadataEnums = emptySet<String>()
    val defaultMetadataCustomScalars = false
    val defaultPackageNameProvider = PackageNameProvider.Flat("com.apollographql.generated")
    val defaultAlwaysGenerateTypesMatching = emptySet<String>()
    val defaultOperationOutputFile = null
    val defaultOperationOutputGenerator = OperationOutputGenerator.Default(OperationIdGenerator.Sha256)
    val defaultCustomScalarsMapping = emptyMap<String, String>()
    val defaultUseSemanticNaming = true
    val defaultWarnOnDeprecatedUsages = true
    val defaultFailOnWarnings = false
    val defaultLogger = NoOpLogger
    val defaultGenerateAsInternal = false
    val defaultGenerateFilterNotNull = false
    val defaultGenerateFragmentsAsInterfaces = false
    val defaultGenerateFragmentImplementations = false
    val defaultGenerateResponseFields = true
    val defaultGenerateQueryDocument = true
    val defaultModuleName = "apollographql"
    val defaultMetadataOutputFile = null

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
