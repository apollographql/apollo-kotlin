package com.apollographql.apollo3.compiler.codegen.java.file

import com.apollographql.apollo3.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo3.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo3.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.L
import com.apollographql.apollo3.compiler.codegen.java.T
import com.apollographql.apollo3.compiler.ir.IrObject
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class BuilderFactoryBuilder(
    private val context: JavaContext,
    private val objs: List<IrObject>,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.builderPackageName()
  private val simpleName = "BuilderFactory"

  override fun prepare() {
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = typeSpec()
    )
  }

  private fun typeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .addField(JavaClassNames.CustomScalarAdapters, customScalarAdapters)
        .addMethod(
            MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(JavaClassNames.CustomScalarAdapters, customScalarAdapters)
                .addStatement("this.$customScalarAdapters = $customScalarAdapters")
                .build()
        )
        .addMethods(
            objs.map {
              it.toMethodSpec()
            }
        )
        .addField(
            FieldSpec.builder(ClassName.get(packageName, simpleName), "DEFAULT")
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .initializer(
                    CodeBlock.of("new $L($T.PassThrough)", simpleName, JavaClassNames.CustomScalarAdapters)
                )
                .build()
        )
        .build()
  }

  private fun IrObject.toMethodSpec(): MethodSpec {
    val builderClassName = ClassName.get(packageName, layout.objectBuilderName(name))
    return MethodSpec.methodBuilder(layout.builderFunName(name))
        .addModifiers(Modifier.PUBLIC)
        .returns(builderClassName)
        .addStatement("return new $T($customScalarAdapters)", builderClassName)
        .build()
  }
}
