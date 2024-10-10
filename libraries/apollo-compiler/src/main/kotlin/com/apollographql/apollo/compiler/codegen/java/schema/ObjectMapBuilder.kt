package com.apollographql.apollo.compiler.codegen.java.schema

import com.apollographql.apollo.compiler.capitalizeFirstLetter
import com.apollographql.apollo.compiler.codegen.Identifier.__fields
import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaSchemaContext
import com.apollographql.apollo.compiler.codegen.typeBuilderPackageName
import com.apollographql.apollo.compiler.ir.IrObject
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class ObjectMapBuilder(
    private val context: JavaSchemaContext,
    private val obj: IrObject,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.typeBuilderPackageName()
  private val simpleName = "${layout.schemaTypeName(obj.name)}Map"

  override fun prepare() {
    context.resolver.registerMapType(obj.name, ClassName.get(packageName, simpleName))
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = obj.mapTypeSpec()
    )
  }

  private fun IrObject.mapTypeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .superclass(JavaClassNames.ObjectMap)
        .addSuperinterfaces(
            superTypes.map {
              ClassName.get(packageName, "${layout.schemaTypeName(it)}Map")
            }
        )
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
