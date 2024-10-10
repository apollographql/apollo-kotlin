package com.apollographql.apollo.compiler.codegen.java.schema

import com.apollographql.apollo.compiler.capitalizeFirstLetter
import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaSchemaContext
import com.apollographql.apollo.compiler.codegen.typeBuilderPackageName
import com.apollographql.apollo.compiler.ir.IrInterface
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class InterfaceMapBuilder(
    private val context: JavaSchemaContext,
    private val iface: IrInterface,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.typeBuilderPackageName()
  private val simpleName = "${layout.schemaTypeName(iface.name)}Map"

  override fun prepare() {
    context.resolver.registerMapType(iface.name, ClassName.get(packageName, simpleName))
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = iface.mapTypeSpec()
    )
  }

  private fun IrInterface.mapTypeSpec(): TypeSpec {
    return TypeSpec
        .interfaceBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .addSuperinterfaces(
            implements.map {
              ClassName.get(packageName, "${layout.schemaTypeName(it)}Map")
            }
        )
        .build()
  }
}
