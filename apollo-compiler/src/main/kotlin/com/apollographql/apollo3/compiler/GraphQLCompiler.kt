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

class GraphQLCompiler(val logger: Logger = NoOpLogger) {

  interface Logger {
    fun warning(message: String)
  }

  fun write(args: Arguments) {
    args.outputDir.deleteRecursively()
    args.outputDir.mkdirs()

    val roots = Roots(args.rootFolders)
    val metadata = collectMetadata(args.metadata)

    val (schema, schemaPackageName) = getSchemaInfo(roots, args.schemaFile, metadata)

    val (documents, issues) = GraphQLParser.parseExecutableFiles(
        args.graphqlFiles,
        schema,
        metadata?.fragments ?: emptyList()
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
        logger.warning("w: ${it.sourceLocation.filePath}:${it.sourceLocation.line}:${column}: ApolloGraphQL: ${it.message}")
      }
      if (args.failOnWarnings && warnings.isNotEmpty()) {
        throw IllegalStateException("ApolloGraphQL: Warnings found and 'failOnWarnings' is true, aborting.")
      }
    }

    val fragments = documents.flatMap {
      it.definitions.filterIsInstance<GQLFragmentDefinition>()
    }.map {
      it.withTypenameWhenNeeded(schema)
    }

    val operations = documents.flatMap {
      it.definitions.filterIsInstance<GQLOperationDefinition>()
    }.map {
      it.withTypenameWhenNeeded(schema)
    }

    val generatedTypes = if (args.useUnifiedIr) {
      doWriteUnified(
          schema = schema,
          operations = operations,
          fragments = fragments,
          metadata = metadata,
          args = args,
          roots = roots,
          schemaPackageName = schemaPackageName,
          rootPackageName = args.rootPackageName
      )
    } else {
      doWrite(
          schema = schema,
          documents = documents,
          operations = operations,
          fragments = fragments,
          metadata = metadata,
          args = args,
          roots = roots,
          schemaPackageName = schemaPackageName,
          rootPackageName = args.rootPackageName
      )
    }

    args.metadataOutputFile.parentFile.mkdirs()
    val outgoingMetadata = ApolloMetadata(
        schema = if (metadata == null) schema else null,
        schemaPackageName = schemaPackageName,
        rootPackageName = args.rootPackageName,
        moduleName = args.moduleName,
        generatedEnums = generatedTypes.enums,
        generatedInputObjects = generatedTypes.inputObjects,
        fragments = documents.flatMap { it.definitions.filterIsInstance<GQLFragmentDefinition>() },
        generateKotlinModels = args.generateKotlinModels,
        customScalarsMapping = args.customScalarsMapping,
        pluginVersion = VERSION
    )
    outgoingMetadata.writeTo(args.metadataOutputFile)

  }

  private class GeneratedTypes(val enums: Set<String>, val inputObjects: Set<String>)

  private fun doWriteUnified(
      schema: Schema,
      operations: List<GQLOperationDefinition>,
      fragments: List<GQLFragmentDefinition>,
      metadata: ApolloMetadata?,
      args: Arguments,
      roots: Roots,
      schemaPackageName: String,
      rootPackageName: String
  ): GeneratedTypes {
    val ir = IrBuilder(
        schema = schema,
        operationDefinitions = operations,
        fragmentDefinitions = fragments,
        metadataFragmentDefinitions = metadata?.fragments ?: emptyList(),
        alwaysGenerateTypesMatching = args.alwaysGenerateTypesMatching,
        customScalarToKotlinName = args.customScalarsMapping,
        roots = roots
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
        generateScalarMapping = metadata == null,
        generateFilterNotNull = args.generateFilterNotNull,
        generateFragmentImplementations = args.generateFragmentImplementations,
        generateFragmentsAsInterfaces = args.generateFragmentsAsInterfaces,
        schemaPackageName = schemaPackageName,
        rootPackageName = rootPackageName
    ).write(outputDir = args.outputDir)

    return GeneratedTypes(
        enums = ir.enums.map { it.name }.toSet(),
        inputObjects = ir.inputObjects.map { it.name }.toSet(),
    )
  }

  private fun doWrite(
      schema: Schema,
      documents: List<GQLDocument>,
      operations: List<GQLOperationDefinition>,
      fragments: List<GQLFragmentDefinition>,
      metadata: ApolloMetadata?,
      args: Arguments,
      roots: Roots,
      schemaPackageName: String,
      rootPackageName: String
  ): GeneratedTypes {
    val packageNameProvider = DefaultPackageNameProvider(
        roots = roots,
        rootPackageName = rootPackageName,
    )

    val typesToGenerate = computeTypesToGenerate(
        documents = documents,
        schema = schema,
        incomingMetadata = metadata,
        alwaysGenerateTypesMatching = args.alwaysGenerateTypesMatching
    )

    val frontendIr = FrontendIrBuilder(
        schema = schema,
        operationDefinitions = operations,
        fragmentDefinitions = fragments,
        metadataFragmentDefinitions = metadata?.fragments ?: emptyList()
    ).build()

    val backendIr = BackendIrBuilder(
        schema = schema,
        useSemanticNaming = args.useSemanticNaming,
        packageNameProvider = packageNameProvider,
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

    val generateKotlinModels = metadata?.generateKotlinModels ?: args.generateKotlinModels
    val userScalarTypesMap = metadata?.customScalarsMapping ?: args.customScalarsMapping

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
    check (unknownScalars.isEmpty()) {
      "ApolloGraphQL: unknown custom scalar(s): ${unknownScalars.joinToString(",")}"
    }
    val customScalarsMapping = schemaScalars
        .map {
          it to (userScalarTypesMap[it] ?: anyClassName(generateKotlinModels))
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
        typesPackageName = "$rootPackageName.$schemaPackageName.type".removePrefix("."),
        fragmentsPackageName = "$rootPackageName.$schemaPackageName.fragment".removePrefix("."),
        generateFragmentImplementations = args.generateFragmentImplementations,
        generateFragmentsAsInterfaces = args.generateFragmentsAsInterfaces,
    ).write(args.outputDir)

    return GeneratedTypes(enums = typesToGenerate.enumsToGenerate, inputObjects = typesToGenerate.inputObjectsToGenerate)
  }

  private fun anyClassName(generateKotlinModels: Boolean) = if (generateKotlinModels) {
    // kotlin.Any
    Any::class.asClassName().toString()
  } else {
    TODO("ClassNames.OBJECT.toString()")
  }


  companion object {

    private fun collectMetadata(metadata: List<File>): ApolloMetadata? {
      return metadata.map {
        ApolloMetadata.readFrom(it)
      }.merge()
    }

    private data class SchemaInfo(val schema: Schema, val schemaPackageName: String)

    private fun getSchemaInfo(roots: Roots, schemaFile: File?, metadata: ApolloMetadata?): SchemaInfo {
      check(schemaFile != null || metadata != null) {
        "ApolloGraphQL: cannot find schema.[json | sdl]"
      }
      check(schemaFile == null || metadata == null) {
        "ApolloGraphQL: You can't define a schema in ${schemaFile?.absolutePath} as one is already defined in a dependency. " +
            "Either remove the schema or the dependency"
      }

      if (schemaFile != null) {
        val schema = if (schemaFile.extension == "json") {
          IntrospectionSchema(schemaFile).toSchema()
        } else {
          GraphQLParser.parseSchema(schemaFile)
        }

        val packageName = try {
          roots.filePackageName(schemaFile.absolutePath)
        } catch (e: IllegalArgumentException) {
          // Can happen if the schema is not a child of roots
          ""
        }
        return SchemaInfo(schema, packageName)
      } else if (metadata != null) {
        return SchemaInfo(metadata.schema!!, metadata.schemaPackageName!!)
      } else {
        throw IllegalStateException("There should at least be metadata or schemaFile")
      }
    }

    val NoOpLogger = object : Logger {
      override fun warning(message: String) {
      }
    }
  }

  /**
   * For more details about the fields defined here, check the gradle plugin
   */
  data class Arguments(
      /**
       * The rootFolders where the graphqlFiles are located. The package name of each individual graphql query
       * will be the relative path to the root folders
       */
      val rootFolders: List<File>,
      /**
       * The files where the graphql queries/fragments are located
       */
      val graphqlFiles: Set<File>,
      /**
       * The schema. Can be either a SDL schema or an introspection schema.
       * If null, the schema, metedata must not be empty
       */
      val schemaFile: File?,
      /**
       * The folder where to generate the sources
       */
      val outputDir: File,

      //========== multi-module ============

      /**
       * A list of files containing metadata from previous compilations
       */
      val metadata: List<File> = emptyList(),
      /**
       * The moduleName for this metadata. Used for debugging purposes
       */
      val moduleName: String = "?",
      /**
       * The file where to write the metadata
       */
      val metadataOutputFile: File,
      /**
       * Additional enum/input types to generate.
       * For input types, this will recursively add all input fields types/enums.
       */
      val alwaysGenerateTypesMatching: Set<String> = emptySet(),

      //========== operation-output ============

      /**
       * the file where to write the operationOutput
       * if null, no operationOutput is written
       */
      val operationOutputFile: File? = null,
      /**
       * the OperationOutputGenerator used to generate operation Ids
       */
      val operationOutputGenerator: OperationOutputGenerator = OperationOutputGenerator.Default(OperationIdGenerator.Sha256()),

      //========== global codegen options ============

      val rootPackageName: String = "",
      val generateKotlinModels: Boolean = false,
      val customScalarsMapping: Map<String, String> = emptyMap(),
      val useSemanticNaming: Boolean = true,
      val warnOnDeprecatedUsages: Boolean = true,
      val failOnWarnings: Boolean = false,
      val generateFragmentImplementations: Boolean = false,

      //========== Kotlin codegen options ============

      val generateAsInternal: Boolean = false,
      /**
       * Kotlin native will generate [Any?] for optional types
       * Setting generateFilterNotNull will generate extra `filterNotNull` functions that will help keep the type information
       */
      val generateFilterNotNull: Boolean = false,
      val enumAsSealedClassPatternFilters: Set<String> = emptySet(),
      val generateFragmentsAsInterfaces: Boolean = true,

      // Debug options
      val dumpIR: Boolean = false,
      val useUnifiedIr: Boolean = false
  )
}
