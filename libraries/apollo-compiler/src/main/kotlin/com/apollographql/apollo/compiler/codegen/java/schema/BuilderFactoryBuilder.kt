package com.apollographql.apollo.compiler.codegen.java.schema

import com.apollographql.apollo.compiler.capitalizeFirstLetter
import com.apollographql.apollo.compiler.codegen.Identifier
import com.apollographql.apollo.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaSchemaContext
import com.apollographql.apollo.compiler.codegen.java.L
import com.apollographql.apollo.compiler.codegen.java.T
import com.apollographql.apollo.compiler.codegen.typeBuilderPackageName
import com.apollographql.apollo.compiler.ir.IrInterface
import com.apollographql.apollo.compiler.ir.IrObject
import com.apollographql.apollo.compiler.ir.IrSchemaType
import com.apollographql.apollo.compiler.ir.IrUnion
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class BuilderFactoryBuilder(
    context: JavaSchemaContext,
    private val objs: List<IrObject>,
    private val ifaces: List<IrInterface>,
    private val unions: List<IrUnion>,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.typeBuilderPackageName()
  private val simpleName = "BuilderFactory"

  override fun prepare() {
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = typeSpec()
    )
  }

  private fun typeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .addField(JavaClassNames.CustomScalarAdapters, customScalarAdapters)
        .addMethod(
            MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(JavaClassNames.CustomScalarAdapters, customScalarAdapters)
                .addStatement("this.$customScalarAdapters = $customScalarAdapters")
                .build()
        )
        .addMethods(
            objs.map {
              it.toObjectBuilderMethodSpec()
            }
        )
        .addMethods(
            ifaces.map {
              it.toUnknownBuilderMethodSpec()
            }
        )
        .addMethods(
            unions.map {
              it.toUnknownBuilderMethodSpec()
            }
        )
        .addField(
            FieldSpec.builder(ClassName.get(packageName, simpleName), "DEFAULT")
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .initializer(
                    CodeBlock.of("new $L($T.PassThrough)", simpleName, JavaClassNames.CustomScalarAdapters)
                )
                .build()
        )
        .build()
  }

  private fun IrObject.toObjectBuilderMethodSpec(): MethodSpec {
    val builderClassName = ClassName.get(packageName, "${layout.schemaTypeName(name)}Builder")
    return MethodSpec.methodBuilder("build${layout.schemaTypeName(name)}")
        .addModifiers(Modifier.PUBLIC)
        .returns(builderClassName)
        .addStatement("return new $T($customScalarAdapters)", builderClassName)
        .build()
  }

  private fun IrSchemaType.toUnknownBuilderMethodSpec(): MethodSpec {
    val builderClassName = ClassName.get(packageName, "Other${layout.schemaTypeName(name)}Builder")
    return MethodSpec.methodBuilder("buildOther${layout.schemaTypeName(name)}")
        .addModifiers(Modifier.PUBLIC)
        .addParameter(JavaClassNames.String, Identifier.__typename)
        .returns(builderClassName)
        .addStatement("return new $T(__typename, $customScalarAdapters)", builderClassName)
        .build()
  }
}
