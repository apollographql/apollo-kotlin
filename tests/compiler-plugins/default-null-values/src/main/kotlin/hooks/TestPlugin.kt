package hooks

import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.ApolloCompilerPluginEnvironment
import com.apollographql.apollo.compiler.ApolloCompilerPluginProvider
import com.apollographql.apollo.compiler.ApolloCompilerRegistry
import com.apollographql.apollo.compiler.Transform
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOutput
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec

class TestPlugin: ApolloCompilerPlugin {
  override fun beforeCompilationStep(
      environment: ApolloCompilerPluginEnvironment,
      registry: ApolloCompilerRegistry,
  ) {
    registry.registerKotlinOutputTransform("test") { input ->
      KotlinOutput(
          fileSpecs = input.fileSpecs.map {
            it.toBuilder()
                .apply {
                  members.replaceAll { member ->
                    if (member is TypeSpec) {
                      member.addDefaultValueToNullableProperties()
                    } else {
                      member
                    }
                  }
                }
                .build()
          },
          codegenMetadata = input.codegenMetadata
      )
    }
  }
}

private fun TypeSpec.addDefaultValueToNullableProperties(): TypeSpec {
  return toBuilder()
      .apply {
        // Only care about data classes
        if (modifiers.contains(KModifier.DATA)) {
          primaryConstructor(
              primaryConstructor!!.toBuilder()
                  .apply {
                    parameters.replaceAll { param ->
                      if (param.type.isNullable) {
                        param.toBuilder()
                            .defaultValue(CodeBlock.of("null"))
                            .build()
                      } else {
                        param
                      }
                    }
                  }
                  .build()
          )
        }

        // Recurse on nested types
        typeSpecs.replaceAll { typeSpec ->
          typeSpec.addDefaultValueToNullableProperties()
        }
      }
      .build()
}