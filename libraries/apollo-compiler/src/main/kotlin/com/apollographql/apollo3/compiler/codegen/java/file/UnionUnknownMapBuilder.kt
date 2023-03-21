package com.apollographql.apollo3.compiler.codegen.java.file

import com.apollographql.apollo3.compiler.codegen.Identifier.__fields
import com.apollographql.apollo3.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo3.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.ir.IrUnion
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class UnionUnknownMapBuilder(
    private val context: JavaContext,
    private val union: IrUnion,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.builderPackageName()
  private val simpleName = layout.otherMapName(union.name)

  override fun prepare() {
    context.resolver.registerMapType(union.name, ClassName.get(packageName, simpleName))
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = union.mapTypeSpec()
    )
  }

  private fun IrUnion.mapTypeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .superclass(JavaClassNames.ObjectMap)
        .addSuperinterface(ClassName.get(packageName, context.layout.mapName(union.name)))
        .addMethod(
            MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(JavaClassNames.MapOfStringToObject, __fields)
                .addStatement("super($__fields)")
                .build()
        )
        .build()
  }
}
