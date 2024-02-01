package com.apollographql.apollo3.compiler.codegen.java.schema

import com.apollographql.apollo3.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo3.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.JavaSchemaContext
import com.apollographql.apollo3.compiler.codegen.java.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.codegen.java.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.codegen.typePackageName
import com.apollographql.apollo3.compiler.ir.IrObject
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class ObjectBuilder(
    private val context: JavaSchemaContext,
    private val obj: IrObject,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = layout.schemaTypeName(obj.name)

  override fun prepare() {
    context.resolver.registerSchemaType(obj.name, ClassName.get(packageName, simpleName))
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = obj.typeSpec()
    )
  }

  private fun IrObject.typeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .addField(typeFieldSpec(context.resolver))
        .build()
  }
}
