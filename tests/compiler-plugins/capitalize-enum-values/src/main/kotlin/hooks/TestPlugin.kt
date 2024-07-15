package hooks

import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.ApolloCompilerPluginEnvironment
import com.apollographql.apollo.compiler.ApolloCompilerPluginProvider
import com.apollographql.apollo.compiler.Transform
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOutput
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec

class TestPluginProvider: ApolloCompilerPluginProvider {
  override fun create(environment: ApolloCompilerPluginEnvironment): ApolloCompilerPlugin {
    return TestPlugin()
  }
}

class TestPlugin : ApolloCompilerPlugin {
  override fun kotlinOutputTransform(): Transform<KotlinOutput> {
    return object : Transform<KotlinOutput> {
      override fun transform(input: KotlinOutput): KotlinOutput {
        return KotlinOutput(
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
                                  propertySpecs.replaceAll { propertySpec ->
                                    if (propertySpec.name == "knownEntries") {
                                      propertySpec.toBuilder()
                                          .getter(FunSpec.getterBuilder()
                                              .addStatement("return listOf(%L)", capitalizedEnumConstants.keys.filterNot { it == "UNKNOWN__" }.joinToString())
                                              .build()
                                          )
                                          .build()
                                    } else {
                                      propertySpec
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

}
