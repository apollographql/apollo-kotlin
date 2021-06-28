package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.api.QueryDocumentMinifier
import com.apollographql.apollo3.compiler.codegen.KotlinCodeGen
import com.apollographql.apollo3.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo3.ast.*
import com.apollographql.apollo3.ast.transformation.addRequiredFields
import com.apollographql.apollo3.compiler.ir.IrBuilder
import com.apollographql.apollo3.compiler.ir.dumpTo
import java.io.File

object GraphQLCompiler {

  interface Logger {
    fun warning(message: String)
  }

  fun write(
      options: Options
  ) {
    val executableFiles = options.executableFiles
    val outputDir = options.outputDir
    val debugDir = options.debugDir
    val schema = options.schema

    checkCustomScalars(schema, options.customScalarsMapping)

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
        definitions = definitions + options.metadataFragments.map { it.definition },
        filePath = null
    ).validateAsExecutable(options.schema)

    validationIssues.checkNoErrors()

    if (options.codegenModels == MODELS_RESPONSE_BASED) {
      findConditionalFragments(definitions).checkNoErrors()
    }

    val warnings = validationIssues.filter {
      it.severity == Issue.Severity.WARNING && (it !is Issue.DeprecatedUsage || options.warnOnDeprecatedUsages)
    }
    warnings.forEach {
      // Using this format, IntelliJ will parse the warning and display it in the 'run' panel
      options.logger.warning("w: ${it.sourceLocation.pretty()}: ApolloGraphQL: ${it.message}")
    }
    if (options.failOnWarnings && warnings.isNotEmpty()) {
      throw IllegalStateException("ApolloGraphQL: Warnings found and 'failOnWarnings' is true, aborting.")
    }

    /**
     * Step 3, Modify the AST to add typename and key fields
     */
    val incomingFragments = options.metadataFragments.map { it.definition }
    var allFragmentDefinitions = (definitions.filterIsInstance<GQLFragmentDefinition>() + incomingFragments).associateBy { it.name }
    val fragments = definitions.filterIsInstance<GQLFragmentDefinition>().map {
      addRequiredFields(it, options.schema)
    }

    val operations = definitions.filterIsInstance<GQLOperationDefinition>().map {
      addRequiredFields(it, options.schema)
    }

    // Update the fragments with the possibly updated fragments
    allFragmentDefinitions = (fragments + incomingFragments).associateBy { it.name }

    operations.forEach {
      checkKeyFields(it, options.schema, allFragmentDefinitions)
    }
    fragments.forEach {
      checkKeyFields(it, options.schema, allFragmentDefinitions)
    }

    /**
     * Build the IR
     */
    val ir = IrBuilder(
        schema = options.schema,
        operationDefinitions = operations,
        allFragmentDefinitions = allFragmentDefinitions,
        alwaysGenerateTypesMatching = options.alwaysGenerateTypesMatching,
        customScalarToKotlinName = options.customScalarsMapping,
        codegenModels = options.codegenModels,
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
      options.operationOutputGenerator.generate(it)
    }

    check(operationOutput.size == operations.size) {
      """The number of operation IDs (${operationOutput.size}) should match the number of operations (${operations.size}).
        |Check that all your IDs are unique.
      """.trimMargin()
    }

    if (options.operationOutputFile != null) {
      options.operationOutputFile.writeText(operationOutput.toJson("  "))
    }

    /**
     * Write the generated models
     */
    KotlinCodeGen(
        ir = ir,
        generateAsInternal = options.generateAsInternal,
        operationOutput = operationOutput,
        useSemanticNaming = options.useSemanticNaming,
        packageNameGenerator = options.packageNameGenerator,
        schemaPackageName = options.schemaPackageName,
        generateFilterNotNull = options.generateFilterNotNull,
        generateFragmentImplementations = options.generateFragmentImplementations,
        generateQueryDocument = options.generateQueryDocument,
        fragmentsToSkip = options.metadataFragments.map { it.name }.toSet(),
        enumsToSkip = options.enumsToSkip,
        inputObjectsToSkip = options.inputObjectsToSkip,
        generateSchema = options.generateTypes,
        flatten = options.flattenModels,
        flattenNamesInOrder = options.codegenModels != MODELS_COMPAT
    ).write(outputDir = outputDir)

    /**
     * Write the metadata
     */
    if (options.metadataOutputFile != null) {
      options.metadataOutputFile.parentFile.mkdirs()
      val outgoingSchema = if (options.generateTypes) {
        options.schema
      } else {
        // There is already a schema defined in this tree
        null
      }

      val outgoingMetadataFragments = fragments.map {
        MetadataFragment(
            name = it.name,
            packageName = "${options.schemaPackageName}.fragment",
            definition = it
        )
      }

      ApolloMetadata(
          schema = outgoingSchema,
          customScalarsMapping = options.customScalarsMapping,
          generatedFragments = outgoingMetadataFragments,
          generatedEnums = ir.enums.map { it.name }.toSet(),
          generatedInputObjects = ir.inputObjects.map { it.name }.toSet(),
          codegenModels = options.codegenModels,
          flattenModels = options.flattenModels,
          moduleName = options.moduleName,
          pluginVersion = VERSION,
          schemaPackageName = options.schemaPackageName
      ).writeTo(options.metadataOutputFile)
    }
  }

  private fun checkCustomScalars(schema: Schema, customScalarsMapping: Map<String, String>) {
    /**
     * Generate the mapping for all custom scalars
     *
     * If the user specified a mapping, use it, else fallback to [Any]
     */
    val schemaScalars = schema
        .typeDefinitions
        .values
        .filterIsInstance<GQLScalarTypeDefinition>()
        .filter { !it.isBuiltIn() }
        .map { type -> type.name }
        .toSet()
    val unknownScalars = customScalarsMapping.keys.subtract(schemaScalars)
    check(unknownScalars.isEmpty()) {
      "ApolloGraphQL: unknown custom scalar(s): ${unknownScalars.joinToString(",")}"
    }
  }

  val NoOpLogger = object : Logger {
    override fun warning(message: String) {
    }
  }
}

