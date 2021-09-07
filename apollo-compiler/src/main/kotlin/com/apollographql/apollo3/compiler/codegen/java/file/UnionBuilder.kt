package com.apollographql.apollo3.compiler.codegen.java.file

import com.apollographql.apollo3.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo3.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.codegen.java.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.ir.IrUnion
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

class UnionBuilder(
    private val context: JavaContext,
    private val union: IrUnion
): JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = layout.compiledTypeName(name = union.name)

  override fun prepare() {
    context.resolver.registerSchemaType(union.name, ClassName.get(packageName, simpleName))
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = union.typeSpec()
    )
  }

  private fun IrUnion.typeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .addField(typeFieldSpec(context.resolver))
        .build()
  }
}
