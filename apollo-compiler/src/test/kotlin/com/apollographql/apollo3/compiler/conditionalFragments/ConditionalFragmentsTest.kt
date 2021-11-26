package com.apollographql.apollo3.compiler.conditionalFragments

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.MODELS_OPERATION_BASED
import com.apollographql.apollo3.compiler.MODELS_RESPONSE_BASED
import com.apollographql.apollo3.compiler.Options
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFails

@OptIn(ApolloExperimental::class)
class ConditionalFragmentsTest {
  @Test
  fun `responseBased codegen fails with conditional fragments`() {
    val throwable = assertFails {
      ApolloCompiler.write(
          Options(
              executableFiles = setOf(File("src/test/kotlin/com/apollographql/apollo3/compiler/conditionalFragments/operations.graphql")),
              schemaFile = File("src/test/kotlin/com/apollographql/apollo3/compiler/conditionalFragments/schema.graphqls"),
              outputDir = File("build/test/conditionalFragmentsTest"),
              packageName = ""
          ).copy(
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
    ApolloCompiler.write(
        Options(
            executableFiles = setOf(File("src/test/kotlin/com/apollographql/apollo3/compiler/conditionalFragments/operations.graphql")),
            schemaFile = File("src/test/kotlin/com/apollographql/apollo3/compiler/conditionalFragments/schema.graphqls"),
            outputDir = File("build/test/conditionalFragmentsTest"),
            packageName = ""
        ).copy(
            customScalarsMapping = emptyMap(),
            codegenModels = MODELS_OPERATION_BASED,
            flattenModels = false
        )
    )
  }
}