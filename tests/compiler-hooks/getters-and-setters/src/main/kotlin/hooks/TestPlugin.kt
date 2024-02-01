package hooks

import com.apollographql.apollo3.compiler.Plugin
import com.apollographql.apollo3.compiler.codegen.java.JavaOutput
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import javax.lang.model.element.Modifier

class TestPlugin : Plugin {
  override fun javaOutputTransform(): ((JavaOutput) -> JavaOutput) {
    return ::transform
  }

  private fun transform(source: JavaOutput): JavaOutput {
    return JavaOutput(
        javaFiles = source.javaFiles.map { javaFile ->
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
                                        MethodSpec.methodBuilder("get${fieldSpec.name.removePrefix("__").capitalize()}")
                                            .addModifiers(Modifier.PUBLIC)
                                            .returns(fieldSpec.type)
                                            .addStatement("return this.\$L", fieldSpec.name)
                                            .build(),
                                        MethodSpec.methodBuilder("set${fieldSpec.name.removePrefix("__").capitalize()}")
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
        codegenMetadata = source.codegenMetadata
    )
  }
}
