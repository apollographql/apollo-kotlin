package com.apollographql.apollo.compiler.codegen.java.schema

import com.apollographql.apollo.compiler.codegen.inputAdapter
import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaSchemaContext
import com.apollographql.apollo.compiler.codegen.java.helpers.inputAdapterTypeSpec
import com.apollographql.apollo.compiler.codegen.java.helpers.toNamedType
import com.apollographql.apollo.compiler.codegen.typeAdapterPackageName
import com.apollographql.apollo.compiler.ir.IrInputObject
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeSpec

internal class InputObjectAdapterBuilder(
    val context: JavaSchemaContext,
    val inputObject: IrInputObject,
) : JavaClassBuilder {
  val packageName = context.layout.typeAdapterPackageName()
  val simpleName = context.layout.schemaTypeName(inputObject.name).inputAdapter()

  override fun prepare() {
    context.resolver.registerInputObjectAdapter(
        inputObject.name,
        ClassName.get(packageName, simpleName)
    )
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = inputObject.adapterTypeSpec()
    )
  }

  private fun IrInputObject.adapterTypeSpec(): TypeSpec {
    return fields.map {
      it.toNamedType()
    }.inputAdapterTypeSpec(
        context,
        simpleName,
        context.resolver.resolveSchemaType(inputObject.name),
    )
  }
}
