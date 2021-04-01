package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.api.QueryDocumentMinifier
import com.apollographql.apollo3.compiler.backend.GraphQLCodeGenerator
import com.apollographql.apollo3.compiler.backend.ir.BackendIrBuilder
import com.apollographql.apollo3.compiler.frontend.GQLDocument
import com.apollographql.apollo3.compiler.frontend.GQLFragmentDefinition
import com.apollographql.apollo3.compiler.frontend.GQLOperationDefinition
import com.apollographql.apollo3.compiler.frontend.GQLTypeDefinition
import com.apollographql.apollo3.compiler.frontend.GraphQLParser
import com.apollographql.apollo3.compiler.frontend.Issue
import com.apollographql.apollo3.compiler.frontend.Schema
import com.apollographql.apollo3.compiler.frontend.SourceAwareException
import com.apollographql.apollo3.compiler.frontend.ir.FrontendIrBuilder
import com.apollographql.apollo3.compiler.frontend.toIntrospectionSchema
import com.apollographql.apollo3.compiler.frontend.withTypenameWhenNeeded
import com.apollographql.apollo3.compiler.introspection.IntrospectionSchema
import com.apollographql.apollo3.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo3.compiler.operationoutput.toJson
import com.apollographql.apollo3.compiler.unified.IrBuilder
import com.apollographql.apollo3.compiler.unified.codegen.KotlinCodeGenerator
import com.squareup.kotlinpoet.asClassName
import java.io.File

class GraphQLCompiler {

  interface Logger {
    fun warning(message: String)
  }

  fun write(
      operationFiles: Set<File>,
      outputDir: File,
      incomingOptions: IncomingOptions,
      useUnifiedIr: Boolean,
      moduleOptions: ModuleOptions
  ) {
    outputDir.deleteRecursively()
    outputDir.mkdirs()
    

    val (documents, issues) = GraphQLParser.parseExecutableFiles(
        operationFiles,
        incomingOptions.schema,
        incomingOptions.metadataFragments.map { it.definition }
    )

    val (errors, warnings) = issues.partition { it.severity == Issue.Severity.ERROR }

    val firstError = errors.firstOrNull()
    if (firstError != null) {
      throw SourceAwareException(
          error = firstError.message,
          sourceLocation = firstError.sourceLocation,
      )
    }

    if (moduleOptions.warnOnDeprecatedUsages) {
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

    val fragments = documents.flatMap {
      it.definitions.filterIsInstance<GQLFragmentDefinition>()
    }.map {
      it.withTypenameWhenNeeded(incomingOptions.schema)
    }

    val operations = documents.flatMap {
      it.definitions.filterIsInstance<GQLOperationDefinition>()
    }.map {
      it.withTypenameWhenNeeded(incomingOptions.schema)
    }

    val generatedTypes = if (useUnifiedIr) {
      doWriteUnified(
          operations = operations,
          fragments = fragments,
          moduleOptions = moduleOptions,
          incomingOptions = incomingOptions,
          outputDir = outputDir,
      )
    } else {
      doWrite(
          documents = documents,
          operations = operations,
          fragments = fragments,
          moduleOptions = moduleOptions,
          incomingOptions = incomingOptions,
          outputDir = outputDir,
      )
    }

    val result = Result(
        generatedEnums = generatedTypes.enums,
        generatedInputObjects = generatedTypes.inputObjects,
        generatedCustomScalars = incomingOptions.metadataCustomScalars,
        generatedFragments = fragments.map {
          MetadataFragment(
              name = it.name,
              packageName = moduleOptions.packageNameProvider.fragmentPackageName(it.sourceLocation.filePath!!),
              definition = it
          )
        }
    )

    if (moduleOptions.metadataOutputFile != null) {
      check(!moduleOptions.generateAsInternal) {
        "Specifying 'generateAsInternal=true' does not make sense in a multi-module setup"
      }
      ApolloMetadata(
          schema = incomingOptions.schema,
          customScalarsMapping = incomingOptions.customScalarsMapping,
          generatedFragments = result.generatedFragments,
          generatedEnums = result.generatedEnums,
          generatedInputObjects = result.generatedInputObjects,
          generateFragmentsAsInterfaces = incomingOptions.generateFragmentsAsInterfaces,
          moduleName = moduleOptions.moduleName,
          pluginVersion = VERSION,
          schemaPackageName = incomingOptions.schemaPackageName
      ).writeTo(moduleOptions.metadataOutputFile)
    }
  }


  private class GeneratedTypes(val enums: Set<String>, val inputObjects: Set<String>)

  private fun doWriteUnified(
      outputDir: File,
      operations: List<GQLOperationDefinition>,
      fragments: List<GQLFragmentDefinition>,
      incomingOptions: IncomingOptions,
      moduleOptions: ModuleOptions,
  ): GeneratedTypes {
    val ir = IrBuilder(
        schema = incomingOptions.schema,
        operationDefinitions = operations,
        fragmentDefinitions = fragments,
        metadataFragments = incomingOptions.metadataFragments,
        alwaysGenerateTypesMatching = moduleOptions.alwaysGenerateTypesMatching,
        customScalarToKotlinName = incomingOptions.customScalarsMapping,
    ).build()

    val operationOutput = ir.operations.map {
      OperationDescriptor(
          name = it.name,
          source = QueryDocumentMinifier.minify(it.sourceWithFragments)
      )
    }.let {
      moduleOptions.operationOutputGenerator.generate(it)
    }

    KotlinCodeGenerator(
        ir = ir,
        generateAsInternal = moduleOptions.generateAsInternal,
        enumAsSealedClassPatternFilters = moduleOptions.enumAsSealedClassPatternFilters,
        operationOutput = operationOutput,
        useSemanticNaming = moduleOptions.useSemanticNaming,
        packageNameProvider = moduleOptions.packageNameProvider,
        generateCustomScalars = incomingOptions.metadataCustomScalars,
        generateFilterNotNull = moduleOptions.generateFilterNotNull,
        generateFragmentsAsInterfaces = incomingOptions.generateFragmentsAsInterfaces,
        generateFragmentImplementations = moduleOptions.generateFragmentImplementations,
        generateResponseFields = moduleOptions.generateResponseFields,
        generateQueryDocument = moduleOptions.generateQueryDocument,
    ).write(outputDir = outputDir)

    return GeneratedTypes(
        enums = ir.enums.map { it.name }.toSet(),
        inputObjects = ir.inputObjects.map { it.name }.toSet(),
    )
  }

  private fun doWrite(
      documents: List<GQLDocument>,
      operations: List<GQLOperationDefinition>,
      fragments: List<GQLFragmentDefinition>,
      outputDir: File,
      moduleOptions: ModuleOptions,
      incomingOptions: IncomingOptions,
  ): GeneratedTypes {
    val schema = incomingOptions.schema

    val typesToGenerate = computeTypesToGenerate(
        documents = documents,
        schema = schema,
        metadataEnums = incomingOptions.metadataEnums,
        metadataInputObjects = incomingOptions.metadataInputObjects,
        metadataCustomScalars = incomingOptions.metadataCustomScalars,
        alwaysGenerateTypesMatching = moduleOptions.alwaysGenerateTypesMatching
    )

    val frontendIr = FrontendIrBuilder(
        schema = schema,
        operationDefinitions = operations,
        fragmentDefinitions = fragments,
        metadataFragmentDefinitions = incomingOptions.metadataFragments.map { it.definition }
    ).build()

    val backendIr = BackendIrBuilder(
        schema = schema,
        useSemanticNaming = moduleOptions.useSemanticNaming,
        packageNameProvider = moduleOptions.packageNameProvider,
        generateFragmentsAsInterfaces = incomingOptions.generateFragmentsAsInterfaces,
    ).buildBackendIR(frontendIr)

    val operationOutput = backendIr.operations.map {
      OperationDescriptor(
          name = it.operationName,
          source = QueryDocumentMinifier.minify(it.definition)
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

    // TODO: use another schema for codegen than introspection schema
    val introspectionSchema = schema.toIntrospectionSchema()

    val userScalarTypesMap = incomingOptions.customScalarsMapping

    /**
     * Generate the mapping for all custom scalars
     *
     * If the user specified a mapping, use it, else fallback to [Any]
     */
    val schemaScalars = introspectionSchema.types
        .values
        .filter { type -> type is IntrospectionSchema.Type.Scalar && !GQLTypeDefinition.builtInTypes.contains(type.name) }
        .map { type -> type.name }
    val unknownScalars = userScalarTypesMap.keys.subtract(schemaScalars.toSet())
    check(unknownScalars.isEmpty()) {
      "ApolloGraphQL: unknown custom scalar(s): ${unknownScalars.joinToString(",")}"
    }
    val customScalarsMapping = schemaScalars
        .map {
          it to (userScalarTypesMap[it] ?: Any::class.asClassName().toString())
        }.toMap()

    GraphQLCodeGenerator(
        backendIr = backendIr,
        schema = schema,
        enumsToGenerate = typesToGenerate.enumsToGenerate,
        inputObjectsToGenerate = typesToGenerate.inputObjectsToGenerate,
        generateScalarMapping = typesToGenerate.generateScalarMapping,
        customScalarsMapping = customScalarsMapping,
        operationOutput = operationOutput,
        generateAsInternal = moduleOptions.generateAsInternal,
        generateFilterNotNull = moduleOptions.generateFilterNotNull,
        enumAsSealedClassPatternFilters = moduleOptions.enumAsSealedClassPatternFilters.map { it.toRegex() },
        typesPackageName = "${incomingOptions.schemaPackageName}.type",
        fragmentsPackageName = moduleOptions.packageNameProvider.fragmentPackageName("unused"),
        generateFragmentImplementations = moduleOptions.generateFragmentImplementations,
        generateFragmentsAsInterfaces = incomingOptions.generateFragmentsAsInterfaces,
    ).write(outputDir)

    return GeneratedTypes(enums = typesToGenerate.enumsToGenerate, inputObjects = typesToGenerate.inputObjectsToGenerate)
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
      val metadataCustomScalars: Boolean,
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
            metadataCustomScalars = true,
            metadataFragments = metadata.generatedFragments,
        )
      }

      fun from(
          roots: Roots,
          schemaFile: File,
          customScalarsMapping: Map<String, String>,
          generateFragmentsAsInterfaces: Boolean,
          rootPackageName: String,
      ): IncomingOptions {
        val relativeSchemaPackageName = try {
          roots.filePackageName(schemaFile.absolutePath)
        } catch (e: Exception) {
          ""
        }
        return IncomingOptions(
            schema = Schema.fromFile(schemaFile),
            schemaPackageName = "$rootPackageName.$relativeSchemaPackageName".removePrefix(".").removeSuffix("."),
            customScalarsMapping = customScalarsMapping,
            generateFragmentsAsInterfaces = generateFragmentsAsInterfaces,
            metadataInputObjects = emptySet(),
            metadataEnums = emptySet(),
            metadataCustomScalars = false,
            metadataFragments = emptyList(),
        )
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
      val enumAsSealedClassPatternFilters: Set<String>,

      //========== on/off flags to switch some codegen off ============

      /**
       * Whether to generate the [com.apollographql.apollo3.api.Fragment] as well as response and variables adapters.
       * If generateFragmentsAsInterfaces is true, this will also generate data classes for the fragments.
       *
       * Set to true if you need to read/write fragments from the cache or if you need to instantiate fragments
       */
      val generateFragmentImplementations: Boolean,
      /**
       * Whether to generate the [com.apollographql.apollo3.api.ResponseField]s. [com.apollographql.apollo3.api.ResponseField]s are
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

  data class Result(
      val generatedInputObjects: Set<String>,
      val generatedEnums: Set<String>,
      val generatedCustomScalars: Boolean,
      val generatedFragments: List<MetadataFragment>,
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
    val defaultEnumAsSealedClassPatternFilters = emptySet<String>()
    val defaultGenerateFragmentsAsInterfaces = false
    val defaultGenerateFragmentImplementations = false
    val defaultGenerateResponseFields = true
    val defaultGenerateQueryDocument = true
    val defaultUseUnifiedIr = false
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
        enumAsSealedClassPatternFilters = defaultEnumAsSealedClassPatternFilters,
        generateFragmentImplementations = defaultGenerateFragmentImplementations,
        generateResponseFields = defaultGenerateResponseFields,
        generateQueryDocument = defaultGenerateQueryDocument,
        packageNameProvider = defaultPackageNameProvider,
        metadataOutputFile = defaultMetadataOutputFile,
        moduleName = defaultModuleName

    )
  }
}
