package com.apollographql.apollo3.compiler.conditionalFragments

import com.apollographql.apollo3.compiler.GraphQLCompiler
import com.apollographql.apollo3.compiler.MODELS_OPERATION_BASED
import com.apollographql.apollo3.compiler.MODELS_RESPONSE_BASED
import com.apollographql.apollo3.compiler.Options
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ConditionalFragmentsTest {
  @Test
  fun `responseBased codegen fails with conditional fragments`() {
    val throwable = assertFails {
      GraphQLCompiler.write(
          Options(
              executableFiles = setOf(File("src/test/kotlin/com/apollographql/apollo3/compiler/conditionalFragments/operations.graphql")),
              schemaFile = File("src/test/kotlin/com/apollographql/apollo3/compiler/conditionalFragments/schema.graphqls"),
              outputDir = File("build/test/conditionalFragmentsTest"),
              packageName = "",
              customScalarsMapping = emptyMap(),
              codegenModels = MODELS_RESPONSE_BASED,
              flattenModels = false
          )
      )
    }

    assertEquals(true, throwable.message?.contains("'responseBased' models do not support @include/@skip directives"))
  }

  @Test
  fun `operationBased codegen succeeds with conditional fragments`() {
    GraphQLCompiler.write(
        Options(
            executableFiles = setOf(File("src/test/kotlin/com/apollographql/apollo3/compiler/conditionalFragments/operations.graphql")),
            schemaFile = File("src/test/kotlin/com/apollographql/apollo3/compiler/conditionalFragments/schema.graphqls"),
            outputDir = File("build/test/conditionalFragmentsTest"),
            packageName = "",
            customScalarsMapping = emptyMap(),
            codegenModels = MODELS_OPERATION_BASED,
            flattenModels = false
        )
    )
  }
}