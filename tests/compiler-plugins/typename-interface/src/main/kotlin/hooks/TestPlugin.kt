package hooks

import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.ApolloCompilerPluginEnvironment
import com.apollographql.apollo.compiler.ApolloCompilerPluginProvider
import com.apollographql.apollo.compiler.ApolloCompilerRegistry
import com.apollographql.apollo.compiler.Transform
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOutput
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec

class TestPluginProvider: ApolloCompilerPluginProvider {
  override fun create(environment: ApolloCompilerPluginEnvironment): ApolloCompilerPlugin {
    return TestPlugin()
  }
}

private val interfaceName = "hooks.typenameinterface.HasTypeName"

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
