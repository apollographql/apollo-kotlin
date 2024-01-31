package com.apollographql.apollo3.compiler.codegen.java.schema

import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo3.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.JavaSchemaContext
import com.apollographql.apollo3.compiler.codegen.typeBuilderPackageName
import com.apollographql.apollo3.compiler.ir.IrUnion
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class UnionMapBuilder(
    val context: JavaSchemaContext,
    val union: IrUnion,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.typeBuilderPackageName()
  private val simpleName = "${union.name.capitalizeFirstLetter()}Map"

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
