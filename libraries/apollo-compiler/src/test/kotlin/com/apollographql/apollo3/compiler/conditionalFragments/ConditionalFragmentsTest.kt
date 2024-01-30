package com.apollographql.apollo3.compiler.conditionalFragments

import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.MODELS_OPERATION_BASED
import com.apollographql.apollo3.compiler.MODELS_RESPONSE_BASED
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.buildCodegenOptions
import com.apollographql.apollo3.compiler.buildCodegenSchemaOptions
import com.apollographql.apollo3.compiler.buildIrOptions
import com.apollographql.apollo3.compiler.toInputFiles
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFails

@RunWith(TestParameterInjector::class)
class ConditionalFragmentsTest {

  class ParametersProvider : TestParameter.TestParameterValuesProvider {
    override fun provideValues() = listOf("include.graphql", "skip.graphql", "defer.graphql")
  }

  @Test
  fun `responseBased codegen fails with conditional fragments`(@TestParameter(valuesProvider = ParametersProvider::class) fileName: String) {
    val throwable = assertFails {
      ApolloCompiler.buildSchemaAndOperationsSources(
          executableFiles = setOf(File("src/test/kotlin/com/apollographql/apollo3/compiler/conditionalFragments/$fileName")).toInputFiles(),
          schemaFiles = setOf(File("src/test/kotlin/com/apollographql/apollo3/compiler/conditionalFragments/schema.graphqls")).toInputFiles(),
          codegenSchemaOptions = buildCodegenSchemaOptions(),
          irOptions = buildIrOptions(flattenModels = false, codegenModels = MODELS_RESPONSE_BASED),
          packageNameGenerator = PackageNameGenerator.Flat(""),
          codegenOptions = buildCodegenOptions(),
          compilerKotlinHooks = null,
          compilerJavaHooks = null,
          logger = null,
          operationManifestFile = null,
          operationOutputGenerator = null,
      )
    }

    assertEquals(true, throwable.message?.contains("'responseBased' and 'experimental_operationBasedWithInterfaces' models do not support @include/@skip and @defer directives"))
  }

  @Test
  fun `operationBased codegen succeeds with conditional fragments`(@TestParameter(valuesProvider = ParametersProvider::class) fileName: String) {
    ApolloCompiler.buildSchemaAndOperationsSources(
        executableFiles = setOf(File("src/test/kotlin/com/apollographql/apollo3/compiler/conditionalFragments/$fileName")).toInputFiles(),
        schemaFiles = setOf(File("src/test/kotlin/com/apollographql/apollo3/compiler/conditionalFragments/schema.graphqls")).toInputFiles(),
        codegenSchemaOptions = buildCodegenSchemaOptions(),
        irOptions = buildIrOptions(flattenModels = false, codegenModels = MODELS_OPERATION_BASED),
        packageNameGenerator = PackageNameGenerator.Flat(""),
        codegenOptions = buildCodegenOptions(),
        compilerKotlinHooks = null,
        compilerJavaHooks = null,
        logger = null,
        operationManifestFile = null,
        operationOutputGenerator = null,
    )
  }
}
