package hooks

import com.apollographql.apollo3.compiler.ApolloCompilerPlugin
import com.apollographql.apollo3.compiler.Transform
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinOutput
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec

class TestPlugin : ApolloCompilerPlugin {
  private val interfaceName = "hooks.typenameinterface.HasTypeName"

  override fun kotlinOutputTransform(): Transform<KotlinOutput> {
    return object : Transform<KotlinOutput> {
      override fun transform(input: KotlinOutput): KotlinOutput {
        return KotlinOutput(
            fileSpecs = input.fileSpecs.map {
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
            codegenMetadata = input.codegenMetadata
        )
      }
    }
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
