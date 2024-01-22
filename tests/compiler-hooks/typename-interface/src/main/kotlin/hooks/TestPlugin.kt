package hooks

import com.apollographql.apollo3.compiler.KotlinOutput
import com.apollographql.apollo3.compiler.Plugin
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec

class TestPlugin : Plugin {
  private val interfaceName = "hooks.typenameinterface.HasTypeName"

  override fun kotlinOutputTransform(): ((KotlinOutput) -> KotlinOutput) {
    return ::transform
  }

  private fun transform(source: KotlinOutput): KotlinOutput {
    return KotlinOutput(
        fileSpecs = source.fileSpecs.map {
          it.toBuilder()
              .apply {
                members.replaceAll { member ->
                  if (member is TypeSpec) {
                    member.addSuperInterfaceOnType()
                  } else {
                    member
                  }
                }
              }
              .build()
        },
        symbols = source.symbols
    )
  }

  private fun TypeSpec.addSuperInterfaceOnType(): TypeSpec {
    return toBuilder()
        .apply {
          var hasTypeName = false
          propertySpecs.replaceAll { propertySpec ->
            if (propertySpec.name == "__typename") {
              hasTypeName = true
              propertySpec.toBuilder()
                  .addModifiers(KModifier.OVERRIDE)
                  .build()
            } else {
              propertySpec
            }
          }
          if (hasTypeName) {
            addSuperinterface(ClassName.bestGuess(interfaceName))
          }

          // Recurse on nested types
          typeSpecs.replaceAll { typeSpec ->
            typeSpec.addSuperInterfaceOnType()
          }
        }
        .build()
  }
}
