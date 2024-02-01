package hooks

import com.apollographql.apollo3.compiler.Plugin
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinOutput
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec

class TestPlugin: Plugin {
  override fun kotlinOutputTransform(): ((KotlinOutput) -> KotlinOutput) {
    return ::transform
  }

  private fun transform(source: KotlinOutput): KotlinOutput {
    return  KotlinOutput(
        fileSpecs = source.fileSpecs.map {
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
        codegenMetadata = source.codegenMetadata
    )
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
}
