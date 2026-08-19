package hooks

import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.ApolloCompilerPluginEnvironment
import com.apollographql.apollo.compiler.ApolloCompilerRegistry
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOutput
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec

class TestPlugin : ApolloCompilerPlugin {
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
                    if (member is TypeSpec && member.isEnum) {
                      member.toBuilder()
                          .apply {
                            val capitalizedEnumConstants = enumConstants.mapKeys { (key, _) ->
                              key.uppercase()
                            }
                            enumConstants.clear()
                            enumConstants.putAll(capitalizedEnumConstants)

                            // knownValues is in the companion object
                            typeSpecs.replaceAll { typeSpec ->
                              typeSpec.toBuilder()
                                  .apply {
                                    funSpecs.replaceAll { funSpec ->
                                      if (funSpec.name == "safeValueOf") {
                                        funSpec.toBuilder()
                                            .clearBody()
                                            // We can't access the rawValues anymore so we fall back to iterating everything
                                            .addCode(CodeBlock.of("return values().find { it.rawValue == rawValue } ?: UNKNOWN__"))
                                            .build()
                                      } else {
                                        funSpec
                                      }
                                    }
                                  }
                                  .build()
                            }
                          }
                          .build()
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
