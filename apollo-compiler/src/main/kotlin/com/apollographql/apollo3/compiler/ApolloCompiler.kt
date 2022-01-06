package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.QueryDocumentMinifier
import com.apollographql.apollo3.ast.GQLDefinition
import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.GQLScalarTypeDefinition
import com.apollographql.apollo3.ast.Issue
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.checkKeyFields
import com.apollographql.apollo3.ast.checkNoErrors
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.transformation.addRequiredFields
import com.apollographql.apollo3.ast.validateAsExecutable
import com.apollographql.apollo3.compiler.codegen.java.JavaCodeGen
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinCodeGen
import com.apollographql.apollo3.compiler.ir.IrBuilder
import com.apollographql.apollo3.compiler.ir.dumpTo
import com.apollographql.apollo3.compiler.operationoutput.OperationDescriptor
import okio.buffer
import okio.source
import java.io.File

@ApolloExperimental
object ApolloCompiler {

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

    if (options.targetLanguage == TargetLanguage.JAVA && options.codegenModels != MODELS_OPERATION_BASED) {
      error("Java codegen does not support ${options.codegenModels}. Only $MODELS_OPERATION_BASED is supported.")
    }
    if (options.targetLanguage == TargetLanguage.JAVA && !options.flattenModels) {
      error("Java codegen does not support nested models as it could trigger name clashes when a nested class has the same name as an " +
          "enclosing one.")
    }

    checkCustomScalars(schema, options.scalarMapping)

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
      val parseResult = file.source().buffer().parseAsGQLDocument(file.path)
      if (parseResult.issues.isNotEmpty()) {
        parseIssues.addAll(parseResult.issues)
      } else {
        // We can force cast here because we're guaranteed the parsing step will produce either issues
        // or a value
        definitions.addAll(parseResult.value!!.definitions)
      }
    }

    // Parsing issues are fatal
    parseIssues.checkNoErrors()

    val incomingFragments = options.incomingCompilerMetadata.flatMap { it.fragments }

    /**
     * Step 2, GraphQL validation
     */
    val validationResult = GQLDocument(
        definitions = definitions + incomingFragments,
        filePath = null
    ).validateAsExecutable(options.schema)

    validationResult.issues.checkNoErrors()

    if (options.codegenModels == MODELS_RESPONSE_BASED) {
      findConditionalFragments(definitions).checkNoErrors()
    }

    val warnings = validationResult.issues.filter {
      it.severity == Issue.Severity.WARNING && (it !is Issue.DeprecatedUsage || options.warnOnDeprecatedUsages)
    }

    warnings.forEach {
      // Using this format, IntelliJ will parse the warning and display it in the 'run' panel
      options.logger.warning("w: ${it.sourceLocation.pretty()}: Apollo: ${it.message}")
    }
    if (options.failOnWarnings && warnings.isNotEmpty()) {
      throw IllegalStateException("Apollo: Warnings found and 'failOnWarnings' is true, aborting.")
    }

    /**
     * Step 3, Modify the AST to add typename and key fields
     */
    val fragmentDefinitions =  (definitions.filterIsInstance<GQLFragmentDefinition>() + incomingFragments).associateBy { it.name }
    val fragments = definitions.filterIsInstance<GQLFragmentDefinition>().map {
      addRequiredFields(it, options.schema, fragmentDefinitions)
    }

    val operations = definitions.filterIsInstance<GQLOperationDefinition>().map {
      addRequiredFields(it, options.schema, fragmentDefinitions)
    }

    // Remember the fragments with the possibly updated fragments
    val allFragmentDefinitions = (fragments + incomingFragments).associateBy { it.name }

    // Check if all the key fields are present in operations and fragments
    // (do this only if there are key fields as it may be costly)
    if (options.schema.hasTypeWithTypePolicy()) {
      operations.forEach {
        checkKeyFields(it, options.schema, allFragmentDefinitions)
      }
      fragments.forEach {
        checkKeyFields(it, options.schema, allFragmentDefinitions)
      }
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
        fragmentDefinitions = fragments,
        allFragmentDefinitions = allFragmentDefinitions,
        alwaysGenerateTypesMatching = alwaysGenerateTypesMatching,
        scalarMapping = options.scalarMapping,
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
      TargetLanguage.JAVA -> {
        JavaCodeGen(
            ir = ir,
            resolverInfos = options.incomingCompilerMetadata.map { it.resolverInfo },
            operationOutput = operationOutput,
            useSemanticNaming = options.useSemanticNaming,
            packageNameGenerator = options.packageNameGenerator,
            schemaPackageName = options.schemaPackageName,
            useSchemaPackageNameForFragments = options.useSchemaPackageNameForFragments,
            generateFragmentImplementations = options.generateFragmentImplementations,
            generateQueryDocument = options.generateQueryDocument,
            generateSchema = options.generateSchema,
            generatedSchemaName = options.generatedSchemaName,
            flatten = options.flattenModels,
            scalarMapping = options.scalarMapping,
        ).write(outputDir = outputDir)
      }
      else -> {
        KotlinCodeGen(
            ir = ir,
            resolverInfos = options.incomingCompilerMetadata.map { it.resolverInfo },
            generateAsInternal = options.generateAsInternal,
            operationOutput = operationOutput,
            useSemanticNaming = options.useSemanticNaming,
            packageNameGenerator = options.packageNameGenerator,
            schemaPackageName = options.schemaPackageName,
            useSchemaPackageNameForFragments = options.useSchemaPackageNameForFragments,
            generateFilterNotNull = options.generateFilterNotNull,
            generateFragmentImplementations = options.generateFragmentImplementations,
            generateQueryDocument = options.generateQueryDocument,
            generateSchema = options.generateSchema,
            generatedSchemaName = options.generatedSchemaName,
            generateTestBuilders = options.generateTestBuilders,
            flatten = options.flattenModels,
            sealedClassesForEnumsMatching = options.sealedClassesForEnumsMatching,
            targetLanguageVersion = options.targetLanguage,
            scalarMapping = options.scalarMapping,
        ).write(outputDir = outputDir, testDir = testDir)
      }
    }

    return CompilerMetadata(
        fragments = fragments,
        resolverInfo = outputResolverInfo,
    )
  }

  private fun checkCustomScalars(schema: Schema, scalarMapping: Map<String, ScalarInfo>) {
    /**
     * Generate the mapping for all custom scalars
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
      "Apollo: unknown custom scalar(s) in customScalarsMapping: ${unknownScalars.joinToString(",")}"
    }
  }

  val NoOpLogger = object : Logger {
    override fun warning(message: String) {
    }
  }
}

