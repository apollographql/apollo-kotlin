package com.apollographql.apollo3.compiler.codegen.java.file

import com.apollographql.apollo3.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo3.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.ir.IrUnion
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class UnionMapBuilder(
    val context: JavaContext,
    val union: IrUnion,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.builderPackageName()
  private val simpleName = layout.mapName(union.name)

  override fun prepare() {
    context.resolver.registerMapType(union.name, ClassName.get(packageName, simpleName))
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = mapTypeSpec()
    )
  }

  private fun mapTypeSpec(): TypeSpec {
    return TypeSpec
        .interfaceBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .build()
  }
}
