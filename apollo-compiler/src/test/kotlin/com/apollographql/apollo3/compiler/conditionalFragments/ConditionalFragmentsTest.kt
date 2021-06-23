package com.apollographql.apollo3.compiler.conditionalFragments

import com.apollographql.apollo3.compiler.GraphQLCompiler
import com.apollographql.apollo3.compiler.MODELS_OPERATION_BASED
import com.apollographql.apollo3.compiler.MODELS_RESPONSE_BASED
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ConditionalFragmentsTest {
  @Test
  fun `responseBased codegen fails with conditional fragments`() {
    val throwable = assertFails {
      GraphQLCompiler().write(
          executableFiles = setOf(File("src/test/kotlin/com/apollographql/apollo3/compiler/conditionalFragments/operations.graphql")),
          outputDir = File("build/test/conditionalFragmentsTest"),
          incomingOptions = GraphQLCompiler.IncomingOptions.fromOptions(
              schemaFiles = setOf(File("src/test/kotlin/com/apollographql/apollo3/compiler/conditionalFragments/schema.graphqls")),
              codegenModels = MODELS_RESPONSE_BASED,
              customScalarsMapping = emptyMap(),
              flattenModels = false,
              schemaPackageName = ""
          ),
          moduleOptions = GraphQLCompiler.DefaultModuleOptions
      )
    }

    assertEquals(true, throwable.message?.contains("'responseBased' models do not support @include/@skip directives"))
  }

  @Test
  fun `operationBased codegen succeeds with conditional fragments`() {
    GraphQLCompiler().write(
        executableFiles = setOf(File("src/test/kotlin/com/apollographql/apollo3/compiler/conditionalFragments/operations.graphql")),
        outputDir = File("build/test/conditionalFragmentsTest"),
        incomingOptions = GraphQLCompiler.IncomingOptions.fromOptions(
            schemaFiles = setOf(File("src/test/kotlin/com/apollographql/apollo3/compiler/conditionalFragments/schema.graphqls")),
            codegenModels = MODELS_OPERATION_BASED,
            customScalarsMapping = emptyMap(),
            flattenModels = false,
            schemaPackageName = ""
        ),
        moduleOptions = GraphQLCompiler.DefaultModuleOptions
    )
  }
}