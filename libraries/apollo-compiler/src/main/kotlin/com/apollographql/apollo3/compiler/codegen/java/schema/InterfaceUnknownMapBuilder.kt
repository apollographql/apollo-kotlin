package com.apollographql.apollo3.compiler.codegen.java.schema

import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.compiler.codegen.Identifier.__fields
import com.apollographql.apollo3.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo3.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaSchemaContext
import com.apollographql.apollo3.compiler.codegen.typeBuilderPackageName
import com.apollographql.apollo3.compiler.ir.IrInterface
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class InterfaceUnknownMapBuilder(
    private val context: JavaSchemaContext,
    private val iface: IrInterface,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.typeBuilderPackageName()
  private val simpleName = "Other${iface.name.capitalizeFirstLetter()}Map"

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
        .classBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .superclass(JavaClassNames.ObjectMap)
        .addSuperinterfaces(
            implements.map {
              ClassName.get(packageName, "${it.capitalizeFirstLetter()}Map")
            }
        )
        .addSuperinterface(ClassName.get(packageName, "${iface.name.capitalizeFirstLetter()}Map"))
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
