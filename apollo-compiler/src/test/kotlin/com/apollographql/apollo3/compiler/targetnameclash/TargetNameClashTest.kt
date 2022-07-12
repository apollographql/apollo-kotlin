package com.apollographql.apollo3.compiler.targetnameclash

import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.MODELS_RESPONSE_BASED
import com.apollographql.apollo3.compiler.Options
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFails

class TargetNameClashTest {

  @Test
  fun `codegen fails when targetName clashes with existing class name`() {
    val throwable = assertFails {
      ApolloCompiler.write(
          Options(
              executableFiles = setOf(File("src/test/kotlin/com/apollographql/apollo3/compiler/targetnameclash/operations.graphql")),
              schemaFile = File("src/test/kotlin/com/apollographql/apollo3/compiler/targetnameclash/schema.graphqls"),
              outputDir = File("build/test/targetnameclash"),
              packageName = ""
          ).copy(
              scalarMapping = emptyMap(),
              codegenModels = MODELS_RESPONSE_BASED,
              flattenModels = false
          )
      )
    }

    assertEquals("Apollo: 'renamedEnum1' cannot be used as a target name for 'ReservedEnum' because it clashes with another class name", throwable.message)
  }
}
