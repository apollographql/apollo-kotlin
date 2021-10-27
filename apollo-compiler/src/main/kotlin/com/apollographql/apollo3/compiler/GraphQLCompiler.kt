package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.api.QueryDocumentMinifier
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinCodeGen
import com.apollographql.apollo3.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo3.ast.*
import com.apollographql.apollo3.ast.transformation.addRequiredFields
import com.apollographql.apollo3.compiler.codegen.java.JavaCodeGen
import com.apollographql.apollo3.compiler.ir.IrBuilder
import com.apollographql.apollo3.compiler.ir.dumpTo
import java.io.File

object GraphQLCompiler {

  interface Logger {
    fun warning(message: String)
  }

  fun write(
      options: Options,
  ): CompilerMetadata {
    val executableFiles = options.executableFiles
    val outputDir = options.outputDir
    val testDir = options.testDir
    val debugDir = options.debugDir
    val schema = options.schema

    if (options.targetLanguage == TARGET_JAVA && options.codegenModels != MODELS_OPERATION_BASED) {
      error("Java codegen does not support ${options.codegenModels}. Only $MODELS_OPERATION_BASED is supported.")
    }
    if (options.targetLanguage == TARGET_JAVA && !options.flattenModels) {
      error("Java codegen does not support nested models as it could trigger name clashes when a nested class has the same name as an " +
          "enclosing one.")
    }

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
      when (val parseResult = file.parseAsGQLDocument()) {
        is ParseResult.Success -> definitions.addAll(parseResult.value.definitions)
        is ParseResult.Error -> parseIssues.addAll(parseResult.issues)
      }
    }

    // Parsing issues are fatal
    parseIssues.checkNoErrors()

    val incomingFragments = options.incomingCompilerMetadata.flatMap { it.fragments }

    /**
     * Step 2, GraphQL validation
     */
    val validationIssues = GQLDocument(
        definitions = definitions + incomingFragments,
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

    val fragments = definitions.filterIsInstance<GQLFragmentDefinition>().map {
      addRequiredFields(it, options.schema)
    }

    val operations = definitions.filterIsInstance<GQLOperationDefinition>().map {
      addRequiredFields(it, options.schema)
    }

    // Remember the fragments with the possibly updated fragments
    val allFragmentDefinitions = (fragments + incomingFragments).associateBy { it.name }

    operations.forEach {
      checkKeyFields(it, options.schema, allFragmentDefinitions)
    }
    fragments.forEach {
      checkKeyFields(it, options.schema, allFragmentDefinitions)
    }

    var alwaysGenerateTypesMatching = options.alwaysGenerateTypesMatching
    if (options.generateSchema) {
      // If we generate the __Schema class, we need all types for possibleTypes to work
      alwaysGenerateTypesMatching = alwaysGenerateTypesMatching + ".*"
    }
    /**
     * Build the IR
     */
    val ir = IrBuilder(
        schema = options.schema,
        operationDefinitions = operations,
        alwaysGenerateResponseBasedDataModelGroup = options.generateTestBuilders,
        fragments = fragments,
        allFragmentDefinitions = allFragmentDefinitions,
        alwaysGenerateTypesMatching = alwaysGenerateTypesMatching,
        customScalarsMapping = options.customScalarsMapping,
        codegenModels = options.codegenModels,
        generateOptionalOperationVariables = options.generateOptionalOperationVariables
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
    val outputResolverInfo = when (options.targetLanguage) {
      TARGET_KOTLIN -> {
        KotlinCodeGen(
            ir = ir,
            resolverInfos = options.incomingCompilerMetadata.map { it.resolverInfo },
            generateAsInternal = options.generateAsInternal,
            operationOutput = operationOutput,
            useSemanticNaming = options.useSemanticNaming,
            packageNameGenerator = options.packageNameGenerator,
            schemaPackageName = options.schemaPackageName,
            generateFilterNotNull = options.generateFilterNotNull,
            generateFragmentImplementations = options.generateFragmentImplementations,
            generateQueryDocument = options.generateQueryDocument,
            generateSchema = options.generateSchema,
            generateTestBuilders = options.generateTestBuilders,
            flatten = options.flattenModels,
            flattenNamesInOrder = options.codegenModels != MODELS_COMPAT,
            sealedClassesForEnumsMatching = options.sealedClassesForEnumsMatching,
            targetLanguageVersion = VersionNumber.parse(options.targetLanguageVersion),
        ).write(outputDir = outputDir, testDir = testDir)
      }
      TARGET_JAVA -> {
        JavaCodeGen(
            ir = ir,
            resolverInfos = options.incomingCompilerMetadata.map { it.resolverInfo },
            operationOutput = operationOutput,
            useSemanticNaming = options.useSemanticNaming,
            packageNameGenerator = options.packageNameGenerator,
            schemaPackageName = options.schemaPackageName,
            generateFragmentImplementations = options.generateFragmentImplementations,
            generateQueryDocument = options.generateQueryDocument,
            generateSchema = options.generateSchema,
            flatten = options.flattenModels,
            flattenNamesInOrder = true
        ).write(outputDir = outputDir)
      }
      else -> error("Target language not supported: ${options.targetLanguage}")
    }


    return CompilerMetadata(
        fragments = fragments,
        resolverInfo = outputResolverInfo,
    )
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
      "ApolloGraphQL: unknown custom scalar(s) in customScalarsMapping: ${unknownScalars.joinToString(",")}"
    }
  }

  val NoOpLogger = object : Logger {
    override fun warning(message: String) {
    }
  }
}

