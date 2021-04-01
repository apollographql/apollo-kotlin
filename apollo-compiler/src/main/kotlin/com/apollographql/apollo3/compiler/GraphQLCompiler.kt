package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.api.QueryDocumentMinifier
import com.apollographql.apollo3.compiler.ApolloMetadata.Companion.merge
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
import com.apollographql.apollo3.compiler.frontend.toSchema
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

  fun write(args: Arguments): Result {
    args.outputDir.deleteRecursively()
    args.outputDir.mkdirs()

    val (documents, issues) = GraphQLParser.parseExecutableFiles(
        args.operationFiles,
        args.schema,
        args.metadataFragments.map { it.definition }
    )

    val (errors, warnings) = issues.partition { it.severity == Issue.Severity.ERROR }

    val firstError = errors.firstOrNull()
    if (firstError != null) {
      throw SourceAwareException(
          error = firstError.message,
          sourceLocation = firstError.sourceLocation,
      )
    }

    if (args.warnOnDeprecatedUsages) {
      warnings.forEach {
        // antlr is 0-indexed but IntelliJ is 1-indexed. Add 1 so that clicking the link will land on the correct location
        val column = it.sourceLocation.position + 1
        // Using this format, IntelliJ will parse the warning and display it in the 'run' panel
        // XXX: uniformize with error handling above
        args.logger.warning("w: ${it.sourceLocation.filePath}:${it.sourceLocation.line}:${column}: ApolloGraphQL: ${it.message}")
      }
      if (args.failOnWarnings && warnings.isNotEmpty()) {
        throw IllegalStateException("ApolloGraphQL: Warnings found and 'failOnWarnings' is true, aborting.")
      }
    }

    val fragments = documents.flatMap {
      it.definitions.filterIsInstance<GQLFragmentDefinition>()
    }.map {
      it.withTypenameWhenNeeded(args.schema)
    }

    val operations = documents.flatMap {
      it.definitions.filterIsInstance<GQLOperationDefinition>()
    }.map {
      it.withTypenameWhenNeeded(args.schema)
    }

    val generatedTypes = if (args.useUnifiedIr) {
      doWriteUnified(
          operations = operations,
          fragments = fragments,
          args = args,
      )
    } else {
      doWrite(
          documents = documents,
          operations = operations,
          fragments = fragments,
          args = args,
      )
    }

    return Result(
        generatedEnums = generatedTypes.enums,
        generatedInputObjects = generatedTypes.inputObjects,
        generatedCustomScalars = args.metadataCustomScalars,
        generatedFragments = fragments.map {
          MetadataFragment(
              name = it.name,
              packageName = args.packageNameProvider.fragmentPackageName(it.sourceLocation.filePath!!),
              definition = it
          )
        }
    )
  }


  private class GeneratedTypes(val enums: Set<String>, val inputObjects: Set<String>)

  private fun doWriteUnified(
      operations: List<GQLOperationDefinition>,
      fragments: List<GQLFragmentDefinition>,
      args: Arguments,
  ): GeneratedTypes {
    val ir = IrBuilder(
        schema = args.schema,
        operationDefinitions = operations,
        fragmentDefinitions = fragments,
        metadataFragmentDefinitions = args.metadataFragments.map { it.definition },
        alwaysGenerateTypesMatching = args.alwaysGenerateTypesMatching,
        customScalarToKotlinName = args.customScalarsMapping,
    ).build()

    val operationOutput = ir.operations.map {
      OperationDescriptor(
          name = it.name,
          source = QueryDocumentMinifier.minify(it.sourceWithFragments)
      )
    }.let {
      args.operationOutputGenerator.generate(it)
    }

    KotlinCodeGenerator(
        ir = ir,
        generateAsInternal = args.generateAsInternal,
        enumAsSealedClassPatternFilters = args.enumAsSealedClassPatternFilters,
        operationOutput = operationOutput,
        useSemanticNaming = args.useSemanticNaming,
        packageNameProvider = args.packageNameProvider,
        generateCustomScalars = args.metadataCustomScalars,
        generateFilterNotNull = args.generateFilterNotNull,
        generateFragmentsAsInterfaces = args.generateFragmentsAsInterfaces,
        generateFragmentImplementations = args.generateFragmentImplementations,
        generateResponseFields = args.generateResponseFields,
        generateQueryDocument = args.generateQueryDocument,
    ).write(outputDir = args.outputDir)

    return GeneratedTypes(
        enums = ir.enums.map { it.name }.toSet(),
        inputObjects = ir.inputObjects.map { it.name }.toSet(),
    )
  }

  private fun doWrite(
      documents: List<GQLDocument>,
      operations: List<GQLOperationDefinition>,
      fragments: List<GQLFragmentDefinition>,
      args: Arguments,
  ): GeneratedTypes {
    val schema = args.schema

    val typesToGenerate = computeTypesToGenerate(
        documents = documents,
        schema = schema,
        metadataEnums = args.metadataEnums,
        metadataInputObjects = args.metadataInputObjects,
        metadataCustomScalars = args.metadataCustomScalars,
        alwaysGenerateTypesMatching = args.alwaysGenerateTypesMatching
    )

    val frontendIr = FrontendIrBuilder(
        schema = schema,
        operationDefinitions = operations,
        fragmentDefinitions = fragments,
        metadataFragmentDefinitions = args.metadataFragments.map { it.definition }
    ).build()

    val backendIr = BackendIrBuilder(
        schema = schema,
        useSemanticNaming = args.useSemanticNaming,
        packageNameProvider = args.packageNameProvider,
        generateFragmentsAsInterfaces = args.generateFragmentsAsInterfaces,
    ).buildBackendIR(frontendIr)

    val operationOutput = backendIr.operations.map {
      OperationDescriptor(
          name = it.operationName,
          source = QueryDocumentMinifier.minify(it.definition)
      )
    }.let {
      args.operationOutputGenerator.generate(it)
    }

    check(operationOutput.size == operations.size) {
      """The number of operation IDs (${operationOutput.size}) should match the number of operations (${operations.size}).
        |Check that all your IDs are unique.
      """.trimMargin()
    }

    if (args.operationOutputFile != null) {
      args.operationOutputFile.writeText(operationOutput.toJson("  "))
    }

    // TODO: use another schema for codegen than introspection schema
    val introspectionSchema = schema.toIntrospectionSchema()

    val userScalarTypesMap = args.customScalarsMapping

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
        generateAsInternal = args.generateAsInternal,
        generateFilterNotNull = args.generateFilterNotNull,
        enumAsSealedClassPatternFilters = args.enumAsSealedClassPatternFilters.map { it.toRegex() },
        typesPackageName = args.packageNameProvider.typePackageName(),
        fragmentsPackageName = args.packageNameProvider.fragmentPackageName("unusued"),
        generateFragmentImplementations = args.generateFragmentImplementations,
        generateFragmentsAsInterfaces = args.generateFragmentsAsInterfaces,
    ).write(args.outputDir)

    return GeneratedTypes(enums = typesToGenerate.enumsToGenerate, inputObjects = typesToGenerate.inputObjectsToGenerate)
  }

  data class Arguments(
      //========== Inputs/Outputs ============
      /**
       * The files where the graphql queries/mutations/subscriptions/fragments are located
       */
      val operationFiles: Set<File>,

      /**
       * The schema. Use [GraphQLParser] to obtain an instance of a schema.
       */
      val schema: Schema,

      /**
       * The folder where to generate the sources
       */
      val outputDir: File,

      //========== multi-module ============
      val metadataFragments: List<MetadataFragment>,
      val metadataInputObjects: Set<String>,
      val metadataEnums: Set<String>,
      val metadataCustomScalars: Boolean,
      val packageNameProvider: PackageNameProvider,

      /**
       * Additional enum/input types to generate.
       * For input types, this will recursively add all input fields types/enums.
       */
      val alwaysGenerateTypesMatching: Set<String>,

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

      val customScalarsMapping: Map<String, String>,
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
      val generateFragmentsAsInterfaces: Boolean,

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

      //========== debug options ============
      val useUnifiedIr: Boolean,
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
    
    fun DefaultArguments(
        operationFiles: Set<File>,
        schema: Schema,
        outputDir: File,
    ) = Arguments(
        operationFiles = operationFiles,
        schema = schema,
        outputDir = outputDir,
        metadataFragments = defaultMetadataFragments,
        metadataInputObjects = defaultMetadataInputObjects,
        metadataEnums = defaultMetadataEnums,
        metadataCustomScalars = defaultMetadataCustomScalars,
        packageNameProvider = defaultPackageNameProvider,
        alwaysGenerateTypesMatching = defaultAlwaysGenerateTypesMatching,
        operationOutputFile = defaultOperationOutputFile,
        operationOutputGenerator = defaultOperationOutputGenerator,
        customScalarsMapping = defaultCustomScalarsMapping,
        useSemanticNaming = defaultUseSemanticNaming,
        warnOnDeprecatedUsages = defaultWarnOnDeprecatedUsages,
        failOnWarnings = defaultFailOnWarnings,
        logger = defaultLogger,
        generateAsInternal = defaultGenerateAsInternal,
        generateFilterNotNull = defaultGenerateFilterNotNull,
        enumAsSealedClassPatternFilters = defaultEnumAsSealedClassPatternFilters,
        generateFragmentsAsInterfaces = defaultGenerateFragmentsAsInterfaces,
        generateFragmentImplementations = defaultGenerateFragmentImplementations,
        generateResponseFields = defaultGenerateResponseFields,
        generateQueryDocument = defaultGenerateQueryDocument,
        useUnifiedIr = defaultUseUnifiedIr,
    )

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
  }
}
