package com.apollographql.apollo.compiler.codegen.java.builders

import com.apollographql.apollo.compiler.codegen.dataMapName
import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaDataBuilderContext
import com.apollographql.apollo.compiler.codegen.builderPackageName
import com.apollographql.apollo.compiler.ir.IrDataBuilder
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class DataInterfaceBuilder(
    private val context: JavaDataBuilderContext,
    private val dataBuilder: IrDataBuilder,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.builderPackageName()
  private val simpleName = dataMapName(dataBuilder.name, false, layout)

  override fun prepare() {
    context.resolver.registerMapType(dataBuilder.name, ClassName.get(packageName, simpleName))
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = dataBuilder.mapTypeSpec()
    )
  }

  private fun IrDataBuilder.mapTypeSpec(): TypeSpec {
    return TypeSpec
        .interfaceBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .addSuperinterfaces(
            superTypes.map {
              ClassName.get(packageName, dataMapName(it, false, layout))
            }
        )
        .build()
  }
}