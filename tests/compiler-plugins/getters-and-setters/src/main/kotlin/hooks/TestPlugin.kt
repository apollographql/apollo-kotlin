package hooks

import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.ApolloCompilerPluginEnvironment
import com.apollographql.apollo.compiler.ApolloCompilerPluginProvider
import com.apollographql.apollo.compiler.Transform
import com.apollographql.apollo.compiler.capitalizeFirstLetter
import com.apollographql.apollo.compiler.codegen.java.JavaOutput
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import javax.lang.model.element.Modifier

class TestPluginProvider: ApolloCompilerPluginProvider {
  override fun create(environment: ApolloCompilerPluginEnvironment): ApolloCompilerPlugin {
    return TestPlugin()
  }
}
class TestPlugin : ApolloCompilerPlugin {
  override fun javaOutputTransform(): Transform<JavaOutput> {
    return object : Transform<JavaOutput> {
      override fun transform(input: JavaOutput): JavaOutput {
        return JavaOutput(
            javaFiles = input.javaFiles.map { javaFile ->
              JavaFile.builder(
                  javaFile.packageName,
                  // Only process operations (generated at the top level)
                  if (javaFile.packageName.endsWith(".adapter") ||
                      javaFile.packageName.endsWith(".selections") ||
                      javaFile.packageName.endsWith(".type") ||
                      javaFile.packageName.endsWith(".fragment")
                  ) {
                    javaFile.typeSpec
                  } else {
                    javaFile.typeSpec!!.toBuilder()
                        .apply {
                          typeSpecs.replaceAll { typeSpec ->
                            if (typeSpec.name == "Builder") {
                              typeSpec
                            } else {
                              typeSpec
                                  .toBuilder()
                                  .addMethods(typeSpec.fieldSpecs
                                      // Ignore $hashCode, $toString etc.
                                      .filterNot { fieldSpec -> fieldSpec.hasModifier(Modifier.TRANSIENT) }
                                      .flatMap { fieldSpec ->
                                        listOf(
                                            MethodSpec.methodBuilder("get${fieldSpec.name.removePrefix("__").capitalizeFirstLetter()}")
                                                .addModifiers(Modifier.PUBLIC)
                                                .returns(fieldSpec.type)
                                                .addStatement("return this.\$L", fieldSpec.name)
                                                .build(),
                                            MethodSpec.methodBuilder("set${fieldSpec.name.removePrefix("__").capitalizeFirstLetter()}")
                                                .addModifiers(Modifier.PUBLIC)
                                                .addParameter(fieldSpec.type, fieldSpec.name)
                                                .addStatement("this.\$L = \$L", fieldSpec.name, fieldSpec.name)
                                                .build()
                                        )
                                      })
                                  .build()
                            }
                          }
                        }
                        .build()
                  }).build()
            },
            codegenMetadata = input.codegenMetadata
        )
      }
    }
  }
}
